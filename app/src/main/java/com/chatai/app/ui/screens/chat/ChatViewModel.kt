package com.chatai.app.ui.screens.chat

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.chatai.app.BuildConfig
import com.chatai.app.ChatApplication
import com.chatai.app.CrashLogger
import com.chatai.app.data.attachments.AttachmentError
import com.chatai.app.data.local.preferences.DailyUsage
import com.chatai.app.data.local.preferences.DailyUsagePolicy
import com.chatai.app.data.remote.AiModels
import com.chatai.app.domain.model.AttachmentType
import com.chatai.app.domain.model.AttachmentStatus
import com.chatai.app.domain.model.ChatAttachment
import com.chatai.app.data.repository.ChatRepository
import com.chatai.app.data.repository.StreamEvent
import com.chatai.app.domain.model.ChatMessage
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "ChatViewModel"
        private const val MESSAGE_GENERIC_ERROR = "Something went wrong. Please try again."
        private const val MESSAGE_NETWORK_ERROR = "Connection error. Please check your internet and try again."
        private const val MESSAGE_UNSUPPORTED_ATTACHMENT =
            "This chat model currently supports image attachments only (JPG, JPEG, PNG)."
    }

    private val appContext: Application get() = getApplication()
    private val container = (application as ChatApplication).container
    private val repository: ChatRepository = container.repository
    private val attachmentRepository = container.attachmentRepository
    private val usageLimitRepository = container.usageLimitRepository
    private val apiKey = BuildConfig.OPENROUTER_API_KEY

    private val _currentConversationId = MutableStateFlow<String?>(null)
    val currentConversationId: StateFlow<String?> = _currentConversationId

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _streamingMessageId = MutableStateFlow<String?>(null)
    val streamingMessageId: StateFlow<String?> = _streamingMessageId

    private val _pendingAttachments = MutableStateFlow<List<ChatAttachment>>(emptyList())
    val pendingAttachments: StateFlow<List<ChatAttachment>> = _pendingAttachments

    private val _isProcessingAttachment = MutableStateFlow(false)
    val isProcessingAttachment: StateFlow<Boolean> = _isProcessingAttachment

    val dailyUsage: StateFlow<DailyUsage> = usageLimitRepository.usage.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        DailyUsage(used = 0)
    )

    private var chatHistory = mutableListOf<ChatMessage>()

    // Catch uncaught coroutine failures so a network error is visible to the user.
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e(TAG, "Uncaught coroutine exception: ${throwable.javaClass.simpleName}: ${throwable.message}")
        CrashLogger.logCrash(
            context = appContext,
            tag = TAG,
            message = "Uncaught coroutine exception: ${throwable.message}",
            throwable = throwable
        )
        _error.value = normalizeErrorMessage(throwable.message)
        _isLoading.value = false
    }

    fun startNewChat() {
        viewModelScope.launch(exceptionHandler) {
            try {
                val conversation = repository.createConversation()
                _currentConversationId.value = conversation.id
                chatHistory.clear()
                _messages.value = emptyList()
                _pendingAttachments.value = emptyList()
                loadMessages(conversation.id)
            } catch (e: Exception) {
                CrashLogger.log(appContext, TAG, "startNewChat failed", e)
                _error.value = "Could not start a new chat. Please try again."
            }
        }
    }

    fun selectConversation(conversationId: String) {
        if (conversationId == _currentConversationId.value) return
        _currentConversationId.value = conversationId
        chatHistory.clear()
        _pendingAttachments.value = emptyList()
        loadMessages(conversationId)
    }

    private fun loadMessages(conversationId: String) {
        viewModelScope.launch(exceptionHandler) {
            try {
                repository.getMessages(conversationId).collect { messageList ->
                    // Legacy generated-image rows stay in Room but are no longer shown.
                    val textMessages = messageList.filter { it.role != "image" }
                    _messages.value = textMessages
                    chatHistory.clear()
                    chatHistory.addAll(textMessages.filter { !it.isStreaming })
                }
            } catch (e: Exception) {
                CrashLogger.log(appContext, TAG, "loadMessages failed for $conversationId", e)
            }
        }
    }

    fun sendMessage(content: String) {
        val attachments = _pendingAttachments.value
        if (content.isBlank() && attachments.isEmpty()) return
        if (_isLoading.value) return
        if (_isProcessingAttachment.value) return
        if (content.length > DailyUsagePolicy.MAX_INPUT_CHARACTERS) {
            _error.value = "Messages are limited to ${DailyUsagePolicy.MAX_INPUT_CHARACTERS} characters"
            return
        }
        if (attachments.any { it.status != AttachmentStatus.READY }) {
            _error.value = "Please wait for attachment processing to finish"
            return
        }
        if (attachments.any { it.type !in setOf(AttachmentType.JPG, AttachmentType.JPEG, AttachmentType.PNG) }) {
            _error.value = MESSAGE_UNSUPPORTED_ATTACHMENT
            return
        }
        if (apiKey.isBlank()) {
            _error.value = "Please set API key"
            return
        }

        viewModelScope.launch(exceptionHandler) {
            try {
                val conversationId = _currentConversationId.value
                if (conversationId == null) {
                    val conversation = repository.createConversation()
                    _currentConversationId.value = conversation.id
                    loadMessages(conversation.id)
                    sendMessageToApi(conversation.id, content, attachments)
                } else {
                    sendMessageToApi(conversationId, content, attachments)
                }
            } catch (e: Exception) {
                CrashLogger.log(appContext, TAG, "sendMessage failed", e)
                _error.value = "Unable to send message. Please try again."
                _isLoading.value = false
            }
        }
    }

    private suspend fun sendMessageToApi(
        conversationId: String,
        content: String,
        attachments: List<ChatAttachment>
    ) {
        if (!usageLimitRepository.tryConsume()) {
            _error.value = "Daily AI limit reached. Your 10 requests reset tomorrow."
            return
        }
        _isLoading.value = true
        _error.value = null

        try {
            repository.sendMessageStream(
                apiKey = apiKey,
                conversationId = conversationId,
                messages = chatHistory.filter {
                    (it.content.isNotBlank() || it.attachments.isNotEmpty()) &&
                        (it.role == "user" || it.role == "assistant")
                },
                userMessage = content,
                attachments = attachments
            ).collect { event ->
                when (event) {
                    is StreamEvent.UserMessageSaved -> Unit
                    is StreamEvent.AssistantMessageStarted -> {
                        _streamingMessageId.value = event.message.id
                    }
                    is StreamEvent.ContentDelta -> {
                        _messages.value = _messages.value.map { message ->
                            if (message.id == event.messageId) {
                                message.copy(content = event.fullContent, isStreaming = true)
                            } else {
                                message
                            }
                        }
                    }
                    is StreamEvent.StreamCompleted -> {
                        _streamingMessageId.value = null
                        _isLoading.value = false
                        _messages.value = _messages.value.map { message ->
                            if (message.id == event.messageId) {
                                message.copy(content = event.fullContent, isStreaming = false)
                            } else {
                                message
                            }
                        }

                        chatHistory.add(
                            ChatMessage(
                                conversationId = conversationId,
                                role = "user",
                                content = content,
                                modelName = AiModels.LUNA_MODEL_NAME,
                                attachments = attachments
                            )
                        )
                        chatHistory.add(
                            ChatMessage(
                                conversationId = conversationId,
                                role = "assistant",
                                content = event.fullContent,
                                modelName = AiModels.LUNA_MODEL_NAME
                            )
                        )
                    }
                    is StreamEvent.StreamError -> {
                        _streamingMessageId.value = null
                        _isLoading.value = false
                        _error.value = normalizeErrorMessage(event.error)
                        CrashLogger.log(appContext, TAG, "Stream error", Exception(event.error))
                    }
                }
            }
            _pendingAttachments.value = emptyList()
        } catch (e: Exception) {
            _streamingMessageId.value = null
            _isLoading.value = false
            CrashLogger.log(appContext, TAG, "sendMessageToApi failed", e)
            _error.value = when (e) {
                is AttachmentError -> e.message ?: MESSAGE_GENERIC_ERROR
                else -> normalizeErrorMessage(e.message)
            }
        }
    }

    fun addAttachment(uri: Uri) {
        if (_isLoading.value) return
        viewModelScope.launch(exceptionHandler) {
            _isProcessingAttachment.value = true
            _error.value = null
            try {
                val attachment = attachmentRepository.prepareAttachment(uri)
                _pendingAttachments.value = listOf(attachment)
            } catch (error: Exception) {
                _error.value = when (error) {
                    is AttachmentError -> error.message
                    else -> "Attachment processing failed"
                }
            } finally {
                _isProcessingAttachment.value = false
            }
        }
    }

    fun removeAttachment(attachmentId: String) {
        _pendingAttachments.value = _pendingAttachments.value.filterNot { it.id == attachmentId }
    }

    fun clearAttachments() {
        _pendingAttachments.value = emptyList()
    }

    fun deleteConversation(id: String) {
        viewModelScope.launch(exceptionHandler) {
            try {
                repository.deleteConversation(id)
                if (id == _currentConversationId.value) {
                    _currentConversationId.value = null
                    _messages.value = emptyList()
                    chatHistory.clear()
                }
            } catch (e: Exception) {
                CrashLogger.log(appContext, TAG, "deleteConversation failed", e)
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    private fun normalizeErrorMessage(raw: String?): String {
        val message = raw?.trim().orEmpty()
        if (message.isBlank()) return MESSAGE_GENERIC_ERROR

        val lower = message.lowercase()
        return when {
            "401" in lower || "unauthorized" in lower || "invalid api key" in lower ->
                "Invalid API key. Please update your key and try again."

            "429" in lower || "rate limit" in lower || "too many requests" in lower ->
                "Rate limit reached. Please wait a moment and try again."

            "timeout" in lower || "failed to connect" in lower || "network" in lower ||
                "unable to resolve host" in lower || "connection" in lower ->
                MESSAGE_NETWORK_ERROR

            "attachment" in lower || "file" in lower || "unsupported" in lower ||
                "image_url" in lower || "content part" in lower ->
                MESSAGE_UNSUPPORTED_ATTACHMENT

            else -> MESSAGE_GENERIC_ERROR
        }
    }

    override fun onCleared() {
        super.onCleared()
        CrashLogger.log(appContext, TAG, "ViewModel cleared")
    }
}
