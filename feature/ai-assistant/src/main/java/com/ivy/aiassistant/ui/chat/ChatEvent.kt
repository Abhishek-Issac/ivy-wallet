package com.ivy.aiassistant.ui.chat

sealed interface ChatEvent {
    data class DraftChanged(val text: String) : ChatEvent
    data object SendDraft : ChatEvent
    data object StopGeneration : ChatEvent
    data object NewConversation : ChatEvent
    data object ClearMessages : ChatEvent
    data object DismissError : ChatEvent
    data object OpenSettings : ChatEvent
}
