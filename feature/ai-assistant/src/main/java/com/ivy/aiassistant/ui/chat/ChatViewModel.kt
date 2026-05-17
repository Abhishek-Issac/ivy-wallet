package com.ivy.aiassistant.ui.chat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.ivy.aiassistant.data.repository.AiChatRepository
import com.ivy.aiassistant.data.repository.AiSettingsRepository
import com.ivy.aiassistant.domain.ChatMessage
import com.ivy.aiassistant.domain.ChatOrchestrator
import com.ivy.aiassistant.domain.ChatRole
import com.ivy.aiassistant.domain.Conversation
import com.ivy.aiassistant.domain.StreamingChunk
import com.ivy.ui.ComposeViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val settings: AiSettingsRepository,
    private val chatRepo: AiChatRepository,
    private val orchestrator: ChatOrchestrator,
) : ComposeViewModel<ChatState, ChatEvent>() {

    private var aiEnabled by mutableStateOf(false)
    private var providerName by mutableStateOf("")
    private var modelName by mutableStateOf("")
    private var streaming by mutableStateOf(true)
    private var messages by mutableStateOf(persistentListOf<ChatMessage>())
    private var draft by mutableStateOf("")
    private var sending by mutableStateOf(false)
    private var conversationTitle by mutableStateOf("New chat")
    private var errorBanner by mutableStateOf<String?>(null)
    private var totalTokens by mutableIntStateOf(0)

    private var conversationId: String? = null
    private var streamingJob: Job? = null
    private var observeMessagesJob: Job? = null

    @Composable
    override fun uiState(): ChatState {
        LaunchedEffect(Unit) { ensureInitialized() }
        return ChatState(
            aiEnabled = aiEnabled,
            providerName = providerName,
            modelName = modelName,
            streaming = streaming,
            messages = messages,
            draft = draft,
            sending = sending,
            conversationTitle = conversationTitle,
            errorBanner = errorBanner,
            totalTokens = totalTokens,
        )
    }

    override fun onEvent(event: ChatEvent) {
        when (event) {
            is ChatEvent.DraftChanged -> draft = event.text
            ChatEvent.SendDraft -> sendDraft()
            ChatEvent.StopGeneration -> stopGeneration()
            ChatEvent.NewConversation -> startNewConversation()
            ChatEvent.ClearMessages -> clearConversation()
            ChatEvent.DismissError -> errorBanner = null
            ChatEvent.OpenSettings -> Unit // handled by screen
        }
    }

    private fun ensureInitialized() {
        viewModelScope.launch {
            settings.configFlow.collectLatest { config ->
                aiEnabled = config.enabled
                providerName = config.provider.displayName
                modelName = config.model
                streaming = config.streaming
                if (conversationId == null) {
                    val newId = ensureConversation(config.provider.displayName, config.model)
                    conversationId = newId
                    observeMessages(newId)
                }
            }
        }
    }

    private suspend fun ensureConversation(
        providerName: String,
        model: String,
    ): String {
        val existingId = conversationId
        if (existingId != null) return existingId
        val conv = Conversation(
            title = "New chat",
            providerName = providerName,
            model = model,
        )
        chatRepo.saveConversation(conv)
        return conv.id
    }

    private fun observeMessages(id: String) {
        observeMessagesJob?.cancel()
        observeMessagesJob = viewModelScope.launch {
            chatRepo.observeMessages(id).collectLatest { list ->
                messages = list.toPersistentList()
                totalTokens = list.sumOf { (it.tokensIn ?: 0) + (it.tokensOut ?: 0) }
            }
        }
    }

    private fun sendDraft() {
        val text = draft.trim()
        val id = conversationId
        when {
            text.isEmpty() || sending -> Unit
            !aiEnabled -> errorBanner = "Enable the AI assistant in settings first."
            id == null -> Unit
            else -> startTurn(id, text)
        }
    }

    private fun startTurn(id: String, text: String) {
        val userMessage = ChatMessage(role = ChatRole.USER, content = text)
        draft = ""
        sending = true
        streamingJob = viewModelScope.launch {
            try {
                orchestrator.sendMessage(id, userMessage).collect { chunk ->
                    if (chunk is StreamingChunk.Error) {
                        errorBanner = chunk.message
                    }
                }
            } finally {
                sending = false
            }
        }
    }

    private fun stopGeneration() {
        streamingJob?.cancel()
        sending = false
    }

    private fun startNewConversation() {
        stopGeneration()
        conversationId = null
        messages = persistentListOf()
        totalTokens = 0
        conversationTitle = "New chat"
        viewModelScope.launch {
            val newId = ensureConversation(providerName, modelName)
            conversationId = newId
            observeMessages(newId)
        }
    }

    private fun clearConversation() {
        val id = conversationId ?: return
        viewModelScope.launch {
            chatRepo.clearMessages(id)
        }
    }
}
