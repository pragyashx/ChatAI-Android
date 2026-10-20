package com.chatai.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.chatai.app.ui.screens.chat.ChatScreen
import com.chatai.app.ui.screens.conversations.ConversationListViewModel
import com.chatai.app.ui.screens.playground.CodePlaygroundScreen
import com.chatai.app.ui.theme.ChitiCodeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ChitiCodeTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val convViewModel: ConversationListViewModel = viewModel()
                    val conversations by convViewModel.conversations.collectAsState()
                    val navController = rememberNavController()

                    NavHost(navController = navController, startDestination = "chat") {
                        composable("chat") {
                            ChatScreen(
                                conversations = conversations,
                                onNewChat = { },
                                onDeleteConversation = { },
                                onSelectConversation = { },
                                onOpenPlayground = {
                                    navController.navigate("playground") { launchSingleTop = true }
                                }
                            )
                        }
                        composable("playground") {
                            CodePlaygroundScreen(onBack = { navController.popBackStack() })
                        }
                    }
                }
            }
        }
    }
}
