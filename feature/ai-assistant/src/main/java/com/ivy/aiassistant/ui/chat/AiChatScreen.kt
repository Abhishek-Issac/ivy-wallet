package com.ivy.aiassistant.ui.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ivy.aiassistant.domain.ChatRole
import com.ivy.aiassistant.ui.chat.components.ChatBubble
import com.ivy.aiassistant.ui.chat.components.ChatInputBar
import com.ivy.aiassistant.ui.chat.components.TypingIndicator
import com.ivy.navigation.AiSettingsScreen
import com.ivy.navigation.navigation
import com.ivy.navigation.screenScopedViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiChatScreen() {
    val viewModel: ChatViewModel = screenScopedViewModel()
    val state = viewModel.uiState()
    AiChatUi(
        state = state,
        onEvent = viewModel::onEvent,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AiChatUi(
    state: ChatState,
    onEvent: (ChatEvent) -> Unit,
) {
    val nav = navigation()
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .testTag("ai_chat_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Ivy AI",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                            ),
                        )
                        if (state.providerName.isNotBlank()) {
                            Text(
                                text = "${state.providerName} • ${state.modelName}",
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { nav.onBackPressed() }) {
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { onEvent(ChatEvent.NewConversation) }) {
                        Icon(Icons.Outlined.Add, contentDescription = "New conversation")
                    }
                    IconButton(onClick = { onEvent(ChatEvent.ClearMessages) }) {
                        Icon(Icons.Outlined.DeleteOutline, contentDescription = "Clear chat")
                    }
                    IconButton(
                        onClick = { nav.navigateTo(AiSettingsScreen) },
                        modifier = Modifier.testTag("ai_chat_settings"),
                    ) {
                        Icon(Icons.Outlined.Settings, contentDescription = "AI settings")
                    }
                },
            )
        },
        bottomBar = {
            Surface(tonalElevation = 2.dp) {
                Column(modifier = Modifier.navigationBarsPadding()) {
                    HorizontalDivider()
                    ChatInputBar(
                        draft = state.draft,
                        sending = state.sending,
                        enabled = state.aiEnabled,
                        onDraftChange = { onEvent(ChatEvent.DraftChanged(it)) },
                        onSend = { onEvent(ChatEvent.SendDraft) },
                        onStop = { onEvent(ChatEvent.StopGeneration) },
                    )
                    if (state.totalTokens > 0) {
                        Text(
                            text = "Total tokens used: ${state.totalTokens}",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            textAlign = TextAlign.End,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        },
    ) { paddingValues ->
        ChatBody(
            state = state,
            paddingValues = paddingValues,
            onEvent = onEvent,
        )
    }
}

@Composable
private fun ChatBody(
    state: ChatState,
    paddingValues: PaddingValues,
    onEvent: (ChatEvent) -> Unit,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(state.messages.size, state.sending) {
        val target = if (state.sending) state.messages.size else state.messages.lastIndex
        if (target >= 0) listState.animateScrollToItem(target.coerceAtLeast(0))
    }

    Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
        if (state.messages.isEmpty()) {
            EmptyState(aiEnabled = state.aiEnabled)
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                items(state.messages, key = { it.id }) { message ->
                    if (message.role != ChatRole.SYSTEM) {
                        ChatBubble(message = message)
                    }
                }
                if (state.sending) {
                    item { TypingIndicator() }
                }
                item { Spacer(modifier = Modifier.height(8.dp)) }
            }
        }
        if (state.errorBanner != null) {
            ErrorBanner(
                message = state.errorBanner,
                onDismiss = { onEvent(ChatEvent.DismissError) },
                modifier = Modifier.align(Alignment.TopCenter),
            )
        }
    }
}

@Composable
private fun EmptyState(aiEnabled: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = if (aiEnabled) "Start a conversation with Ivy AI" else "AI assistant is disabled",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (aiEnabled) {
                "Ask about your finances, budgets, or how to use Ivy Wallet."
            } else {
                "Open AI settings to choose a provider, enter a key, and enable the assistant."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ErrorBanner(
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        tonalElevation = 2.dp,
    ) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onDismiss) {
                Icon(Icons.Outlined.Close, contentDescription = "Dismiss")
            }
        }
    }
}
