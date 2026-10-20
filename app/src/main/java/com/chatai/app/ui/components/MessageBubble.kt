package com.chatai.app.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.TextSnippet
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chatai.app.data.remote.AiModels
import com.chatai.app.domain.model.AttachmentType
import com.chatai.app.domain.model.ChatMessage
import com.chatai.app.domain.model.readableSize
import com.chatai.app.ui.theme.ChatColors

@Composable
fun MessageBubble(
    message: ChatMessage,
    modifier: Modifier = Modifier
) {
    when (message.role) {
        "user" -> UserMessage(message, modifier)
        else -> AssistantMessage(message, modifier)
    }
}

@Composable
private fun UserMessage(message: ChatMessage, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .animateContentSize(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Surface(
            modifier = Modifier.size(28.dp),
            shape = CircleShape,
            color = ChatColors.UserAvatarBg
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "You",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = "You",
                color = ChatColors.TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            if (message.attachments.isNotEmpty()) {
                AttachmentMessageCard(message = message)
            }
            if (message.content.isNotBlank()) {
                Text(
                    text = message.content,
                    color = ChatColors.TextPrimary,
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun AttachmentMessageCard(message: ChatMessage) {
    message.attachments.forEach { attachment ->
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = ChatColors.InputBackground,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp, bottom = 4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = iconForAttachment(attachment.type),
                    contentDescription = null,
                    tint = ChatColors.Accent,
                    modifier = Modifier.size(19.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = attachment.displayName,
                        color = ChatColors.TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                    Text(
                        text = attachment.readableSize(),
                        color = ChatColors.TextTertiary,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

private fun iconForAttachment(type: AttachmentType): ImageVector = when (type) {
    AttachmentType.PDF -> Icons.Default.PictureAsPdf
    AttachmentType.TXT -> Icons.AutoMirrored.Filled.TextSnippet
    AttachmentType.JPG,
    AttachmentType.JPEG,
    AttachmentType.PNG -> Icons.Default.Image
    AttachmentType.DOC,
    AttachmentType.DOCX -> Icons.Default.Description
    AttachmentType.OTHER -> Icons.AutoMirrored.Filled.InsertDriveFile
}

@Composable
private fun AssistantMessage(message: ChatMessage, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .animateContentSize(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Surface(
            modifier = Modifier.size(28.dp),
            shape = CircleShape,
            color = ChatColors.AssistantAvatarBg
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "AI",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = AiModels.LUNA_MODEL_NAME,
                color = ChatColors.TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )

            if (message.isStreaming && message.content.isEmpty()) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = ChatColors.Accent,
                    strokeWidth = 2.dp
                )
            } else {
                MarkdownContent(
                    markdown = message.content,
                    modifier = Modifier.fillMaxWidth()
                )
                if (message.isStreaming) {
                    Surface(
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .size(6.dp, 18.dp),
                        color = ChatColors.Accent,
                        shape = MaterialTheme.shapes.small
                    ) {}
                }
            }
        }
    }
}
