package com.chatai.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.chatai.app.domain.model.ChatAttachment
import com.chatai.app.domain.model.ChatMessage
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val role: String,
    val content: String,
    val timestamp: Long,
    val imageUrl: String? = null,
    val imageGenerationId: String? = null,
    val imageStatus: String? = null,
    val imageType: String? = null,
    val galleryId: String? = null,
    val characterName: String? = null,
    val characterHeadshotUrl: String? = null,
    val modelName: String? = null,
    val attachmentsJson: String? = null
) {
    fun toDomainModel(): ChatMessage = ChatMessage(
        id = id,
        conversationId = conversationId,
        role = role,
        content = content,
        timestamp = timestamp,
        imageUrl = imageUrl,
        imageGenerationId = imageGenerationId,
        imageStatus = imageStatus,
        imageType = imageType,
        galleryId = galleryId,
        characterName = characterName,
        characterHeadshotUrl = characterHeadshotUrl,
        modelName = modelName,
        attachments = parseAttachmentsJson(attachmentsJson)
    )

    companion object {
        private val gson = Gson()
        private val attachmentListType = object : TypeToken<List<ChatAttachment>>() {}.type

        fun fromDomainModel(message: ChatMessage): MessageEntity = MessageEntity(
            id = message.id,
            conversationId = message.conversationId,
            role = message.role,
            content = message.content,
            timestamp = message.timestamp,
            imageUrl = message.imageUrl,
            imageGenerationId = message.imageGenerationId,
            imageStatus = message.imageStatus,
            imageType = message.imageType,
            galleryId = message.galleryId,
            characterName = message.characterName,
            characterHeadshotUrl = message.characterHeadshotUrl,
            modelName = message.modelName,
            attachmentsJson = attachmentsToJson(message.attachments)
        )

        private fun attachmentsToJson(attachments: List<ChatAttachment>): String? {
            if (attachments.isEmpty()) return null
            return gson.toJson(attachments)
        }

        private fun parseAttachmentsJson(value: String?): List<ChatAttachment> {
            if (value.isNullOrBlank()) return emptyList()
            return runCatching {
                gson.fromJson<List<ChatAttachment>>(value, attachmentListType).orEmpty()
            }.getOrDefault(emptyList())
        }
    }
}
