package com.chatai.app.data.repository

import com.chatai.app.data.local.ChatDatabase
import com.chatai.app.data.local.dao.ConversationDao
import com.chatai.app.data.local.dao.MessageDao
import com.chatai.app.data.local.entity.ConversationEntity
import com.chatai.app.data.local.entity.MessageEntity
import com.chatai.app.data.remote.AiModels
import com.chatai.app.data.remote.OpenRouterApi
import com.chatai.app.data.remote.dto.MessageDto
import com.chatai.app.domain.model.ChatMessage
import com.chatai.app.domain.model.Conversation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*

class ChatRepository(
    private val database: ChatDatabase,
    private val api: OpenRouterApi
) {
    private val conversationDao: ConversationDao = database.conversationDao()
    private val messageDao: MessageDao = database.messageDao()

    val systemPrompt = """You are ChatAI, a helpful and friendly text-based AI assistant.

Answer clearly and thoroughly, and always respond in the same language the user uses. Image generation is not supported. Do not emit image-generation commands or pretend that an image was created."""

    // Conversations
    fun getConversations(): Flow<List<Conversation>> =
        conversationDao.getAllConversations()
            .map { entities -> entities.map { it.toDomainModel() } }
            .flowOn(Dispatchers.IO)

    suspend fun createConversation(title: String = "New Chat"): Conversation {
        val conversation = Conversation(title = title)
        conversationDao.insertConversation(ConversationEntity.fromDomainModel(conversation))
        return conversation
    }

    suspend fun updateConversationTitle(id: String, title: String) {
        val entity = conversationDao.getConversationById(id) ?: return
        conversationDao.updateConversation(entity.copy(title = title, updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteConversation(id: String) {
        conversationDao.deleteConversationById(id)
        messageDao.deleteMessagesByConversation(id)
    }

    // Messages
    fun getMessages(conversationId: String): Flow<List<ChatMessage>> =
        messageDao.getMessagesByConversation(conversationId)
            .map { entities -> entities.map { it.toDomainModel() } }
            .flowOn(Dispatchers.IO)

    private suspend fun saveMessage(message: ChatMessage) {
        messageDao.insertMessage(MessageEntity.fromDomainModel(message))
    }

    suspend fun updateMessageContent(id: String, content: String) {
        messageDao.updateMessageContent(id, content)
    }

    fun sendMessageStream(
        apiKey: String,
        conversationId: String,
        messages: List<ChatMessage>,
        userMessage: String
    ): Flow<StreamEvent> = flow {
        // Save user message
        val userMsg = ChatMessage(
            conversationId = conversationId,
            role = "user",
            content = userMessage,
            modelName = AiModels.LUNA_MODEL_NAME
        )
        saveMessage(userMsg)
        emit(StreamEvent.UserMessageSaved(userMsg))

        // Update conversation timestamp
        val convEntity = conversationDao.getConversationById(conversationId)
        if (convEntity != null) {
            conversationDao.updateConversation(
                convEntity.copy(updatedAt = System.currentTimeMillis())
            )
        }

        // Create assistant message placeholder
        val assistantMsg = ChatMessage(
            conversationId = conversationId,
            role = "assistant",
            content = "",
            isStreaming = true,
            modelName = AiModels.LUNA_MODEL_NAME
        )
        saveMessage(assistantMsg)
        emit(StreamEvent.AssistantMessageStarted(assistantMsg))

        // Build API messages: system prompt + history + user message
        val apiMessages = mutableListOf<MessageDto>()
        apiMessages.add(MessageDto(role = "system", content = systemPrompt))

        // Add conversation history (filter out image-only messages)
        val historyMessages = messages.filter {
            it.role == "user" || it.role == "assistant"
        }.takeLast(20) // Keep last 20 messages for context

        for (msg in historyMessages) {
            val role = if (msg.role == "assistant") "assistant" else "user"
            apiMessages.add(MessageDto(role = role, content = msg.content))
        }
        apiMessages.add(MessageDto(role = "user", content = userMessage))

        var fullContent = ""
        try {
            api.sendMessageStream(apiKey, apiMessages).collect { chunk ->
                fullContent += chunk
                emit(StreamEvent.ContentDelta(assistantMsg.id, chunk, fullContent))
            }

            // Save final content
            updateMessageContent(assistantMsg.id, fullContent)
            emit(StreamEvent.StreamCompleted(assistantMsg.id, fullContent))

            // Auto-generate title from first message if conversation is new
            if (convEntity?.title == "New Chat" && messages.isEmpty()) {
                val title = if (userMessage.length > 40) userMessage.substring(0, 40) + "..." else userMessage
                updateConversationTitle(conversationId, title)
            }
        } catch (e: Exception) {
            emit(StreamEvent.StreamError(assistantMsg.id, e.message ?: "Unknown error"))
            if (fullContent.isNotEmpty()) {
                updateMessageContent(assistantMsg.id, fullContent)
            }
        }
    }.flowOn(Dispatchers.IO)
}

sealed class StreamEvent {
    data class UserMessageSaved(val message: ChatMessage) : StreamEvent()
    data class AssistantMessageStarted(val message: ChatMessage) : StreamEvent()
    data class ContentDelta(val messageId: String, val delta: String, val fullContent: String) : StreamEvent()
    data class StreamCompleted(val messageId: String, val fullContent: String) : StreamEvent()
    data class StreamError(val messageId: String, val error: String) : StreamEvent()
}
