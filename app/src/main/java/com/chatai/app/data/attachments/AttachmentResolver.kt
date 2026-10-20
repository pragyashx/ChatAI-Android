package com.chatai.app.data.attachments

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.chatai.app.domain.model.AttachmentStatus
import com.chatai.app.domain.model.ChatAttachment

class AttachmentResolver(private val context: Context) {
    private val contentResolver: ContentResolver = context.contentResolver

    fun resolve(uri: Uri): ChatAttachment {
        val mimeType = contentResolver.getType(uri).orEmpty()
        val metadata = queryMetadata(uri)
        val fileName = metadata.first ?: "attachment"
        val size = metadata.second ?: 0L

        if (!AttachmentPolicy.supports(mimeType, fileName)) {
            throw AttachmentError.UnsupportedType(fileName)
        }
        if (size <= 0L) {
            ensureAccessible(uri, fileName)
        }
        if (size > AttachmentPolicy.MAX_FILE_SIZE_BYTES) {
            throw AttachmentError.FileTooLarge(size, AttachmentPolicy.MAX_FILE_SIZE_BYTES)
        }

        val safeMime = if (mimeType.isBlank()) "application/octet-stream" else mimeType
        val type = AttachmentPolicy.detectType(safeMime, fileName)
        return ChatAttachment(
            uriString = uri.toString(),
            displayName = fileName,
            mimeType = safeMime,
            sizeBytes = size,
            type = type,
            status = AttachmentStatus.READY
        )
    }

    private fun queryMetadata(uri: Uri): Pair<String?, Long?> {
        val cursor = contentResolver.query(uri, null, null, null, null) ?: return null to null
        cursor.use {
            if (!it.moveToFirst()) return null to null
            val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)
            val name = if (nameIndex >= 0) it.getString(nameIndex) else null
            val size = if (sizeIndex >= 0 && !it.isNull(sizeIndex)) it.getLong(sizeIndex) else null
            return name to size
        }
    }

    private fun ensureAccessible(uri: Uri, fileName: String) {
        runCatching {
            contentResolver.openInputStream(uri)?.use { stream ->
                val buffer = ByteArray(1)
                stream.read(buffer)
            }
        }.onFailure {
            throw AttachmentError.AccessDenied(fileName)
        }
    }
}
