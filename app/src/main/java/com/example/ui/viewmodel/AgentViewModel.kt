package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.SettingsDataStore
import com.example.services.GeminiAgentService
import com.example.services.PendingAgentAction
import com.example.services.SpeechVoiceManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

enum class ActionStatus {
    PENDING,
    APPROVED,
    CANCELLED,
}

data class AgentChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val isUser: Boolean,
    val text: String,
    val proposedAction: PendingAgentAction? = null,
    val actionStatus: ActionStatus = ActionStatus.PENDING,
    val isError: Boolean = false,
)

data class AgentUiState(
    val showGuide: Boolean = true,
    val messages: List<AgentChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val isListening: Boolean = false,
    val partialTranscript: String = "",
    val errorMessage: String? = null,
)

class AgentViewModel : ViewModel() {
    private val geminiService = GeminiAgentService()

    private val _state = MutableStateFlow(AgentUiState())
    val state: StateFlow<AgentUiState> = _state.asStateFlow()

    fun openChat() {
        _state.update { it.copy(showGuide = false) }
    }

    fun openGuide() {
        _state.update { it.copy(showGuide = true) }
    }

    fun clearChat() {
        _state.update { it.copy(messages = emptyList(), errorMessage = null) }
    }

    fun sendPrompt(
        prompt: String,
        appState: MainUiState,
        settingsStore: SettingsDataStore,
        speechManager: SpeechVoiceManager,
    ) {
        val clean = prompt.trim()
        if (clean.isBlank()) return
        if (!appState.settings.aiAgentEnabled) {
            _state.update {
                it.copy(
                    showGuide = false,
                    errorMessage = "AI Agent is turned off in Settings.",
                )
            }
            return
        }

        val userMsg = AgentChatMessage(isUser = true, text = clean)
        val historyPairs = _state.value.messages
            .filter { !it.isError }
            .windowed(size = 2, step = 2, partialWindows = false)
            .mapNotNull { pair ->
                if (pair[0].isUser && !pair[1].isUser) pair[0].text to pair[1].text else null
            }

        _state.update {
            it.copy(
                showGuide = false,
                messages = it.messages + userMsg,
                isLoading = true,
                errorMessage = null,
            )
        }

        viewModelScope.launch {
            try {
                val result = geminiService.ask(
                    userMessage = clean,
                    historyPairs = historyPairs,
                    wallets = appState.wallets,
                    transactions = appState.transactions,
                    categories = appState.categories,
                    budgets = appState.budgets,
                    recurringRules = appState.recurringRules,
                    plans = appState.plans,
                )
                settingsStore.addAiTokensUsed(result.estimatedTokens)

                val aiMsg = AgentChatMessage(
                    isUser = false,
                    text = result.replyText,
                    proposedAction = result.proposedAction,
                )
                _state.update {
                    it.copy(
                        messages = it.messages + aiMsg,
                        isLoading = false,
                    )
                }
                if (appState.settings.aiVoiceReplyEnabled) {
                    speechManager.speak(result.replyText)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val msg = e.localizedMessage ?: "Unable to reach Gemini AI right now."
                _state.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = msg,
                        messages = it.messages + AgentChatMessage(
                            isUser = false,
                            text = msg,
                            isError = true,
                        ),
                    )
                }
            }
        }
    }

    fun updatePendingAction(messageId: String, updatedAction: PendingAgentAction) {
        _state.update { st ->
            st.copy(
                messages = st.messages.map { msg ->
                    if (msg.id == messageId) msg.copy(proposedAction = updatedAction) else msg
                }
            )
        }
    }

    fun approveAction(messageId: String, executor: (PendingAgentAction) -> String, speechManager: SpeechVoiceManager, speakReply: Boolean) {
        val msg = _state.value.messages.firstOrNull { it.id == messageId } ?: return
        val action = msg.proposedAction ?: return
        val confirmation = executor(action)
        _state.update { st ->
            st.copy(
                messages = st.messages.map { m ->
                    if (m.id == messageId) m.copy(actionStatus = ActionStatus.APPROVED) else m
                } + AgentChatMessage(isUser = false, text = confirmation)
            )
        }
        if (speakReply) {
            speechManager.speak(confirmation)
        }
    }

    fun cancelAction(messageId: String) {
        _state.update { st ->
            st.copy(
                messages = st.messages.map { m ->
                    if (m.id == messageId) m.copy(actionStatus = ActionStatus.CANCELLED) else m
                }
            )
        }
    }

    fun setListening(listening: Boolean, partial: String = "") {
        _state.update { it.copy(isListening = listening, partialTranscript = partial) }
    }
}
