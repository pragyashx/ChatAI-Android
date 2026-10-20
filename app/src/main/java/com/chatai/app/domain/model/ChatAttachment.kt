package com.chatai.app.domain.model

import java.util.UUID

enum class AttachmentType {
    PDF,
    DOC,
    DOCX,
    TXT,
    JPG,
    JPEG,
    PNG,
    OTHER
}

enum class AttachmentStatus {
    READY,
    PROCESSING,
    FAILED
}

data class ChatAttachment(
    val id: String = UUID.randomUUID().toString(),
    val uriString: String,
    val displayName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val type: AttachmentType,
    val status: AttachmentStatus = AttachmentStatus.READY
)

fun ChatAttachment.readableSize(): String {
    if (sizeBytes < 1024) return "$sizeBytes B"
    val kb = sizeBytes / 1024.0
    if (kb < 1024) return String.format("%.1f KB", kb)
    val mb = kb / 1024.0
    if (mb < 1024) return String.format("%.1f MB", mb)
    val gb = mb / 1024.0
    return String.format("%.1f GB", gb)
}
