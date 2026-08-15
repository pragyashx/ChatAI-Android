package com.chatai.app.ui.screens.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
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
    onSelectConversation: (String) -> Unit
) {
    var sidebarOpen by remember { mutableStateOf(false) }
    val messages by chatViewModel.messages.collectAsState()
    val isLoading by chatViewModel.isLoading.collectAsState()
    var currentInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

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
                    message = currentInput,
                    onMessageChange = { currentInput = it },
                    onSend = {
                        if (currentInput.isNotBlank()) {
                            chatViewModel.sendMessage(currentInput)
                            currentInput = ""
                        }
                    },
                    isStreaming = isLoading
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
        }
    }
}

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
            modifier = Modifier.size(48.dp),
            shape = CircleShape,
            color = ChatColors.Accent
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "AI",
                    color = ChatColors.TextOnAccent,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "What can I help with?",
            color = ChatColors.TextPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(32.dp))

        val examples = listOf(
            "Explain quantum computing simply",
            "Write a Python web scraper",
            "Help me plan a healthy dinner",
            "Tell me about Vietnamese culture"
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
