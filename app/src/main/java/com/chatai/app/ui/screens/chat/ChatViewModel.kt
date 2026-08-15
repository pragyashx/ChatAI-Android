package com.chatai.app.ui.screens.chat

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.chatai.app.BuildConfig
import com.chatai.app.ChatApplication
import com.chatai.app.CrashLogger
import com.chatai.app.data.remote.AiModels
import com.chatai.app.data.repository.ChatRepository
import com.chatai.app.data.repository.StreamEvent
import com.chatai.app.domain.model.ChatMessage
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "ChatViewModel"
    }

    private val appContext: Application get() = getApplication()
    private val repository: ChatRepository =
        (application as ChatApplication).container.repository
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
        _error.value = "Lỗi hệ thống: ${throwable.message}"
        _isLoading.value = false
    }

    fun startNewChat() {
        viewModelScope.launch(exceptionHandler) {
            try {
                val conversation = repository.createConversation()
                _currentConversationId.value = conversation.id
                chatHistory.clear()
                _messages.value = emptyList()
                loadMessages(conversation.id)
            } catch (e: Exception) {
                CrashLogger.log(appContext, TAG, "startNewChat failed", e)
                _error.value = "Không thể tạo cuộc trò chuyện mới"
            }
        }
    }

    fun selectConversation(conversationId: String) {
        if (conversationId == _currentConversationId.value) return
        _currentConversationId.value = conversationId
        chatHistory.clear()
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
        if (content.isBlank()) return
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
                    sendMessageToApi(conversation.id, content)
                } else {
                    sendMessageToApi(conversationId, content)
                }
            } catch (e: Exception) {
                CrashLogger.log(appContext, TAG, "sendMessage failed", e)
                _error.value = "Không thể gửi tin nhắn: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    private suspend fun sendMessageToApi(conversationId: String, content: String) {
        _isLoading.value = true
        _error.value = null

        try {
            repository.sendMessageStream(
                apiKey = apiKey,
                conversationId = conversationId,
                messages = chatHistory.filter {
                    it.content.isNotBlank() && (it.role == "user" || it.role == "assistant")
                },
                userMessage = content
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
                                modelName = AiModels.LUNA_MODEL_NAME
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
                        _error.value = event.error
                        CrashLogger.log(appContext, TAG, "Stream error", Exception(event.error))
                    }
                }
            }
        } catch (e: Exception) {
            _streamingMessageId.value = null
            _isLoading.value = false
            CrashLogger.log(appContext, TAG, "sendMessageToApi failed", e)
            _error.value = "Lỗi kết nối: ${e.message}"
        }
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

    override fun onCleared() {
        super.onCleared()
        CrashLogger.log(appContext, TAG, "ViewModel cleared")
    }
}
