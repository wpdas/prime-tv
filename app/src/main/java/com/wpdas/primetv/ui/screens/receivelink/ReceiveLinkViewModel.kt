package com.wpdas.primetv.ui.screens.receivelink

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wpdas.primetv.data.link.LinkCodes
import com.wpdas.primetv.domain.link.LinkEvent
import com.wpdas.primetv.domain.link.LinkReceiver
import com.wpdas.primetv.domain.link.LinkSession
import com.wpdas.primetv.domain.model.SourceType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReceiveLinkUiState(
    val code: String = "",
    val qrUrl: String = "",
    val secondsLeft: Int = 0,
    val expired: Boolean = false,
    val receivedLink: String? = null,
) {
    val formattedCode: String get() = LinkCodes.format(code)
    val detected: SourceType? get() = receivedLink?.let { link ->
        when {
            link.contains("username=") && link.contains("password=") -> SourceType.XTREAM
            link.contains(".m3u") || link.contains("get.php") -> SourceType.M3U
            else -> null
        }
    }
    /** URL com usuário e senha ocultos para exibição. */
    val maskedLink: String? get() = receivedLink?.replace(Regex("(username|password)=[^&]*"), "$1=•••••")
}

@HiltViewModel
class ReceiveLinkViewModel @Inject constructor(private val receiver: LinkReceiver) : ViewModel() {

    private val _state = MutableStateFlow(ReceiveLinkUiState())
    val state: StateFlow<ReceiveLinkUiState> = _state.asStateFlow()

    private var sessionJob: Job? = null
    private var timerJob: Job? = null

    init {
        newCode()
    }

    fun newCode() {
        sessionJob?.cancel()
        timerJob?.cancel()
        val session: LinkSession = receiver.newSession()
        _state.value = ReceiveLinkUiState(code = session.code, qrUrl = session.url, secondsLeft = ((session.expiresAt - System.currentTimeMillis()) / 1000).toInt())
        sessionJob = receiver.events(session).onEach { event ->
            when (event) {
                LinkEvent.Waiting -> Unit
                is LinkEvent.Received -> _state.update { it.copy(receivedLink = event.link) }
                LinkEvent.Expired -> _state.update { it.copy(expired = true, secondsLeft = 0) }
            }
        }.launchIn(viewModelScope)
        timerJob = viewModelScope.launch {
            while (_state.value.secondsLeft > 0 && _state.value.receivedLink == null) {
                delay(1_000)
                _state.update { it.copy(secondsLeft = (it.secondsLeft - 1).coerceAtLeast(0), expired = it.secondsLeft - 1 <= 0) }
            }
        }
    }
}
