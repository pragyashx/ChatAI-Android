package com.chatai.app.data.attachments

sealed class AttachmentError(message: String) : Exception(message) {
    class UnsupportedType(fileName: String) :
        AttachmentError("Unsupported file type for \"$fileName\". Use PDF, DOC, DOCX, TXT, JPG, JPEG, or PNG.")

    class FileTooLarge(sizeBytes: Long, maxSizeBytes: Long) :
        AttachmentError("File is too large (${sizeBytes.toReadableSize()}). Maximum allowed is ${maxSizeBytes.toReadableSize()}.")

    class AccessDenied(fileName: String) :
        AttachmentError("Cannot access file \"$fileName\". Please choose it again.")

    class ReadFailure(fileName: String) :
        AttachmentError("Could not read file \"$fileName\".")

    class UploadFailure(fileName: String) :
        AttachmentError("Failed to process \"$fileName\".")

    class NetworkFailure :
        AttachmentError("Network failure while sending attachment.")
}

fun Long.toReadableSize(): String {
    if (this < 1024) return "$this B"
    val kb = this / 1024.0
    if (kb < 1024) return String.format("%.1f KB", kb)
    val mb = kb / 1024.0
    if (mb < 1024) return String.format("%.1f MB", mb)
    val gb = mb / 1024.0
    return String.format("%.1f GB", gb)
}
