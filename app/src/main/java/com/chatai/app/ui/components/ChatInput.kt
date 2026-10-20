package com.chatai.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.TextSnippet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chatai.app.domain.model.AttachmentType
import com.chatai.app.domain.model.ChatAttachment
import com.chatai.app.domain.model.readableSize
import com.chatai.app.ui.theme.ChatColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatInput(
    message: String,
    onMessageChange: (String) -> Unit,
    onSend: () -> Unit,
    attachment: ChatAttachment?,
    isProcessingAttachment: Boolean,
    onOpenAttachmentMenu: () -> Unit,
    onRemoveAttachment: (String) -> Unit,
    isStreaming: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    remainingRequests: Int = 10,
    maxCharacters: Int = 8_000,
    placeholder: String = "Ask a coding question..."
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val canSend = enabled && !isProcessingAttachment && (message.isNotBlank() || attachment != null || isStreaming)

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color.Transparent
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            if (attachment != null || isProcessingAttachment) {
                AttachmentPreviewCard(
                    attachment = attachment,
                    isProcessing = isProcessingAttachment,
                    onRemove = onRemoveAttachment
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = ChatColors.InputBackground,
                        shape = RoundedCornerShape(24.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = ChatColors.Border,
                        shape = RoundedCornerShape(24.dp)
                    )
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onOpenAttachmentMenu,
                    enabled = enabled && !isStreaming && !isProcessingAttachment,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add attachment",
                        tint = ChatColors.TextSecondary
                    )
                }

                OutlinedTextField(
                    value = message,
                    onValueChange = onMessageChange,
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester),
                    placeholder = {
                        Text(
                            text = placeholder,
                            color = ChatColors.TextTertiary,
                            fontSize = 15.sp
                        )
                    },
                    textStyle = LocalTextStyle.current.copy(
                        color = ChatColors.TextPrimary,
                        fontSize = 15.sp
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        disabledBorderColor = Color.Transparent,
                        cursorColor = ChatColors.TextPrimary
                    ),
                    maxLines = 8,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Send
                    ),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (enabled && !isStreaming && !isProcessingAttachment && (message.isNotBlank() || attachment != null)) {
                                onSend()
                                keyboardController?.hide()
                            }
                        }
                    ),
                    enabled = enabled && !isStreaming
                )

                IconButton(
                    onClick = {
                        if (isStreaming) {
                            // TODO: Stop streaming
                        } else if (!isProcessingAttachment && (message.isNotBlank() || attachment != null)) {
                            onSend()
                            keyboardController?.hide()
                        }
                    },
                    modifier = Modifier.size(40.dp),
                    enabled = canSend
                ) {
                    Icon(
                        imageVector = if (isStreaming) Icons.Default.StopCircle else Icons.AutoMirrored.Filled.Send,
                        contentDescription = if (isStreaming) "Stop" else "Send",
                        tint = if (message.isNotBlank() || attachment != null || isStreaming)
                            ChatColors.TextOnAccent
                        else
                            ChatColors.IconTint,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Bottom text
            Text(
                text = if (remainingRequests > 0) {
                    "$remainingRequests/10 AI requests remaining today  |  ${message.length}/$maxCharacters"
                } else {
                    "Daily AI limit reached. Requests reset tomorrow."
                },
                color = ChatColors.TextTertiary,
                fontSize = 11.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun AttachmentPreviewCard(
    attachment: ChatAttachment?,
    isProcessing: Boolean,
    onRemove: (String) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = ChatColors.Surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (isProcessing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = ChatColors.Accent,
                    strokeWidth = 2.dp
                )
                Text(
                    text = "Processing attachment...",
                    color = ChatColors.TextSecondary,
                    fontSize = 13.sp,
                    modifier = Modifier.weight(1f)
                )
            } else if (attachment != null) {
                Icon(
                    imageVector = attachmentIcon(attachment.type),
                    contentDescription = null,
                    tint = ChatColors.Accent,
                    modifier = Modifier.size(20.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = attachment.displayName,
                        color = ChatColors.TextPrimary,
                        fontSize = 13.sp,
                        maxLines = 1,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = attachment.readableSize(),
                        color = ChatColors.TextTertiary,
                        fontSize = 11.sp
                    )
                }
                IconButton(onClick = { onRemove(attachment.id) }, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove attachment",
                        tint = ChatColors.TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun attachmentIcon(type: AttachmentType) = when (type) {
    AttachmentType.PDF -> Icons.Default.PictureAsPdf
    AttachmentType.TXT -> Icons.AutoMirrored.Filled.TextSnippet
    AttachmentType.JPG,
    AttachmentType.JPEG,
    AttachmentType.PNG -> Icons.Default.Image
    AttachmentType.DOC,
    AttachmentType.DOCX -> Icons.Default.Description
    AttachmentType.OTHER -> Icons.AutoMirrored.Filled.InsertDriveFile
}
