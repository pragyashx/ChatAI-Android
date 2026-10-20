package com.chatai.app.di

import android.content.Context
import com.chatai.app.data.local.ChatDatabase
import com.chatai.app.data.local.preferences.UsageLimitRepository
import com.chatai.app.data.remote.Judge0Api
import com.chatai.app.data.remote.OpenRouterApi
import com.chatai.app.data.repository.AttachmentRepository
import com.chatai.app.data.repository.ChatRepository
import com.chatai.app.data.repository.PlaygroundRepository

class AppContainer(private val context: Context) {
    val database: ChatDatabase = ChatDatabase.getDatabase(context)
    val api: OpenRouterApi = OpenRouterApi()
    val attachmentRepository: AttachmentRepository = AttachmentRepository(context)
    val repository: ChatRepository = ChatRepository(database, api, attachmentRepository)
    val judge0Api: Judge0Api = Judge0Api()
    val usageLimitRepository: UsageLimitRepository = UsageLimitRepository(context)
    val playgroundRepository: PlaygroundRepository = PlaygroundRepository(
        attemptDao = database.codeAttemptDao(),
        judge0Api = judge0Api,
        openRouterApi = api
    )
}
