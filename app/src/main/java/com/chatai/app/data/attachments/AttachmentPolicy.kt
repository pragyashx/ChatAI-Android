package com.chatai.app.data.attachments

import com.chatai.app.domain.model.AttachmentType
import java.util.Locale

object AttachmentPolicy {
    const val MAX_FILE_SIZE_BYTES: Long = 10L * 1024L * 1024L

    val allowedMimeTypes = setOf(
        "application/pdf",
        "application/msword",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "text/plain",
        "image/jpeg",
        "image/png"
    )

    val allowedExtensions = setOf("pdf", "doc", "docx", "txt", "jpg", "jpeg", "png")

    fun detectType(mimeType: String?, fileName: String): AttachmentType {
        val normalizedMime = mimeType?.lowercase(Locale.ROOT).orEmpty()
        if (normalizedMime == "application/pdf") return AttachmentType.PDF
        if (normalizedMime == "application/msword") return AttachmentType.DOC
        if (normalizedMime == "application/vnd.openxmlformats-officedocument.wordprocessingml.document") return AttachmentType.DOCX
        if (normalizedMime == "text/plain") return AttachmentType.TXT
        if (normalizedMime == "image/jpeg") return AttachmentType.JPEG
        if (normalizedMime == "image/png") return AttachmentType.PNG

        val extension = fileName.substringAfterLast('.', "").lowercase(Locale.ROOT)
        return when (extension) {
            "pdf" -> AttachmentType.PDF
            "doc" -> AttachmentType.DOC
            "docx" -> AttachmentType.DOCX
            "txt" -> AttachmentType.TXT
            "jpg" -> AttachmentType.JPG
            "jpeg" -> AttachmentType.JPEG
            "png" -> AttachmentType.PNG
            else -> AttachmentType.OTHER
        }
    }

    fun supports(mimeType: String?, fileName: String): Boolean {
        val extension = fileName.substringAfterLast('.', "").lowercase(Locale.ROOT)
        val normalizedMime = mimeType?.lowercase(Locale.ROOT)
        return (normalizedMime != null && normalizedMime in allowedMimeTypes) || extension in allowedExtensions
    }
}
