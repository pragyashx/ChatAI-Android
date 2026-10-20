package com.chatai.app.data.repository

import android.content.Context
import android.net.Uri
import com.chatai.app.data.attachments.AttachmentError
import com.chatai.app.data.attachments.AttachmentResolver
import com.chatai.app.domain.model.ChatAttachment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Base64

class AttachmentRepository(context: Context) {
    private val appContext = context.applicationContext
    private val resolver = AttachmentResolver(context)

    suspend fun prepareAttachment(uri: Uri): ChatAttachment = withContext(Dispatchers.IO) {
        runCatching {
            resolver.resolve(uri)
        }.getOrElse { error ->
            when (error) {
                is AttachmentError -> throw error
                else -> throw AttachmentError.ReadFailure(uri.toString())
            }
        }
    }

    suspend fun readBase64Data(attachment: ChatAttachment): String = withContext(Dispatchers.IO) {
        val uri = Uri.parse(attachment.uriString)
        val bytes = runCatching {
            appContext.contentResolver.openInputStream(uri)?.use { input ->
                input.readBytes()
            }
        }.getOrNull() ?: throw AttachmentError.AccessDenied(attachment.displayName)

        if (bytes.isEmpty()) {
            throw AttachmentError.ReadFailure(attachment.displayName)
        }
        Base64.getEncoder().encodeToString(bytes)
    }
}
