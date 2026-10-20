package com.chatai.app.ui.screens.chat

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.res.painterResource
import com.chatai.app.R
import com.chatai.app.data.local.preferences.DailyUsagePolicy
import com.chatai.app.data.remote.AiModels
import com.chatai.app.domain.model.Conversation
import com.chatai.app.ui.components.ChatInput
import com.chatai.app.ui.components.MessageBubble
import com.chatai.app.ui.components.Sidebar
import com.chatai.app.ui.theme.ChatColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    chatViewModel: ChatViewModel = viewModel(),
    conversations: List<Conversation>,
    onNewChat: () -> Unit,
    onDeleteConversation: (String) -> Unit,
    onSelectConversation: (String) -> Unit,
    onOpenPlayground: () -> Unit
) {
    var sidebarOpen by remember { mutableStateOf(false) }
    var attachmentSheetOpen by remember { mutableStateOf(false) }
    var requestedMimeTypes by remember { mutableStateOf(arrayOf("*/*")) }
    val messages by chatViewModel.messages.collectAsState()
    val isLoading by chatViewModel.isLoading.collectAsState()
    val isProcessingAttachment by chatViewModel.isProcessingAttachment.collectAsState()
    val pendingAttachments by chatViewModel.pendingAttachments.collectAsState()
    val dailyUsage by chatViewModel.dailyUsage.collectAsState()
    var currentInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val context = LocalContext.current

    val attachmentPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            chatViewModel.addAttachment(uri)
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val error by chatViewModel.error.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            chatViewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = ChatColors.Background
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .background(ChatColors.Background)
                        .padding(horizontal = 4.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { sidebarOpen = true }) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu", tint = ChatColors.TextSecondary)
                    }

                    Text(
                        text = AiModels.LUNA_MODEL_NAME,
                        color = ChatColors.TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    IconButton(onClick = onOpenPlayground) {
                        Icon(
                            Icons.Default.Code,
                            contentDescription = "Open verification lab",
                            tint = ChatColors.Accent
                        )
                    }
                }

                if (messages.isEmpty()) {
                    WelcomeScreen(
                        modifier = Modifier.weight(1f),
                        onSendExample = { example -> currentInput = example }
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(messages, key = { message -> "msg_${message.id}" }) { message ->
                            MessageBubble(message = message)
                        }
                        item { Spacer(modifier = Modifier.height(8.dp)) }
                    }
                }

                ChatInput(
                    modifier = Modifier.imePadding(),
                    message = currentInput,
                    onMessageChange = {
                        currentInput = it.take(DailyUsagePolicy.MAX_INPUT_CHARACTERS)
                    },
                    onSend = {
                        if (currentInput.isNotBlank() || pendingAttachments.isNotEmpty()) {
                            chatViewModel.sendMessage(currentInput)
                            currentInput = ""
                        }
                    },
                    attachment = pendingAttachments.firstOrNull(),
                    isProcessingAttachment = isProcessingAttachment,
                    onOpenAttachmentMenu = {
                        attachmentSheetOpen = true
                    },
                    onRemoveAttachment = chatViewModel::removeAttachment,
                    isStreaming = isLoading,
                    enabled = dailyUsage.canConsume,
                    remainingRequests = dailyUsage.remaining,
                    maxCharacters = DailyUsagePolicy.MAX_INPUT_CHARACTERS
                )
            }

            if (sidebarOpen) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .clickable { sidebarOpen = false }
                )
                Sidebar(
                    conversations = conversations,
                    currentConversationId = chatViewModel.currentConversationId.collectAsState().value,
                    onConversationClick = {
                        chatViewModel.selectConversation(it)
                        onSelectConversation(it)
                        sidebarOpen = false
                    },
                    onNewChat = {
                        chatViewModel.startNewChat()
                        onNewChat()
                        sidebarOpen = false
                    },
                    onDeleteConversation = {
                        chatViewModel.deleteConversation(it)
                        onDeleteConversation(it)
                    },
                    onClose = { sidebarOpen = false }
                )
            }

            if (attachmentSheetOpen) {
                AttachmentMenuSheet(
                    onDismiss = { attachmentSheetOpen = false },
                    onPickOption = { mimeTypes ->
                        requestedMimeTypes = mimeTypes
                        attachmentSheetOpen = false
                        attachmentPicker.launch(requestedMimeTypes)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AttachmentMenuSheet(
    onDismiss: () -> Unit,
    onPickOption: (Array<String>) -> Unit
) {
    val options = remember {
        listOf(
            AttachmentMenuOption(
                title = "Upload Image",
                icon = Icons.Default.Image,
                mimeTypes = arrayOf("image/jpeg", "image/png")
            )
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = ChatColors.Surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Add attachment",
                color = ChatColors.TextPrimary,
                style = MaterialTheme.typography.titleMedium
            )
            options.forEach { option ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPickOption(option.mimeTypes) },
                    shape = RoundedCornerShape(12.dp),
                    color = ChatColors.InputBackground
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(option.icon, contentDescription = null, tint = ChatColors.Accent)
                        Text(option.title, color = ChatColors.TextPrimary)
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

private data class AttachmentMenuOption(
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val mimeTypes: Array<String>
)

@Composable
private fun WelcomeScreen(
    modifier: Modifier = Modifier,
    onSendExample: (String) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(56.dp),
            shape = CircleShape,
            color = Color.Transparent,
            shadowElevation = 6.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF6D5DFB),
                                Color(0xFF0F766E)
                            )
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_launcher_foreground),
                    contentDescription = "Robot",
                    modifier = Modifier.size(48.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "What can I help you code?",
            color = ChatColors.TextPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(32.dp))

        val examples = listOf(
            "Give me prime number code in Python",
            "Write binary search in Java",
            "Create a prime number program in C",
            "Debug this Kotlin null pointer error"
        )

        examples.forEach { example ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(14.dp),
                color = ChatColors.SurfaceVariant,
                onClick = { onSendExample(example) }
            ) {
                Text(
                    text = example,
                    color = ChatColors.TextSecondary,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}
