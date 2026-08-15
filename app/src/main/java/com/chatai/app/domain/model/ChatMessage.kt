package com.chatai.app.domain.model

import java.util.UUID

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val conversationId: String,
    val role: String, // "user" or "assistant"; "image" is retained for legacy rows
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    // Legacy image fields remain for Room compatibility but are no longer used by the app.
    val imageUrl: String? = null,
    val imageGenerationId: String? = null,
    val imageStatus: String? = null,
    val imageType: String? = null,
    val galleryId: String? = null,
    val characterName: String? = null,
    val characterHeadshotUrl: String? = null,
    val isStreaming: Boolean = false,
    val modelName: String? = null
)
