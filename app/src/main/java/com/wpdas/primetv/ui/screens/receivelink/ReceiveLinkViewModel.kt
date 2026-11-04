package com.wpdas.primetv.ui.screens.receivelink

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wpdas.primetv.core.di.ApplicationScope
import com.wpdas.primetv.data.link.LinkCodes
import com.wpdas.primetv.domain.link.LinkEvent
import com.wpdas.primetv.domain.link.LinkException
import com.wpdas.primetv.domain.link.LinkReceiver
import com.wpdas.primetv.domain.link.LinkSession
import com.wpdas.primetv.domain.model.SourceType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReceiveLinkUiState(
    /** Pedindo um código ao site ("Gerando código…"). */
    val creating: Boolean = false,
    /** Não deu para gerar o código; texto do servidor, ou vazio para a mensagem padrão. */
    val createError: String? = null,
    val code: String = "",
    val qrUrl: String = "",
    val secondsLeft: Int = 0,
    val expired: Boolean = false,
    /** Sem resposta do site no momento; a espera continua sozinha. */
    val offline: Boolean = false,
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
class ReceiveLinkViewModel @Inject constructor(
    private val receiver: LinkReceiver,
    @ApplicationScope private val appScope: CoroutineScope,
) : ViewModel() {

    private val _state = MutableStateFlow(ReceiveLinkUiState())
    val state: StateFlow<ReceiveLinkUiState> = _state.asStateFlow()

    private var sessionJob: Job? = null
    private var timerJob: Job? = null
    /** Sessão aberta no servidor, até ser confirmada ou descartada. */
    private var current: LinkSession? = null

    init {
        newCode()
    }

    fun newCode() {
        sessionJob?.cancel()
        timerJob?.cancel()
        releaseCurrent()
        _state.value = ReceiveLinkUiState(creating = true)
        sessionJob = viewModelScope.launch {
            val session = try {
                receiver.newSession()
            } catch (e: CancellationException) {
                throw e
            } catch (e: LinkException) {
                _state.value = ReceiveLinkUiState(createError = e.userMessage.orEmpty())
                return@launch
            }
            current = session
            _state.value = ReceiveLinkUiState(
                code = session.code,
                qrUrl = session.url,
                secondsLeft = ((session.expiresAt - System.currentTimeMillis()) / 1000).toInt().coerceAtLeast(0),
            )
            startTimer()
            receiver.events(session).collect { event ->
                when (event) {
                    LinkEvent.Waiting -> _state.update { it.copy(offline = false) }
                    LinkEvent.Offline -> _state.update { it.copy(offline = true) }
                    is LinkEvent.Received -> {
                        // O receptor já confirma o recebimento; nada mais a descartar.
                        current = null
                        _state.update { it.copy(receivedLink = event.link, offline = false) }
                    }
                    LinkEvent.Expired -> {
                        current = null
                        _state.update { it.copy(expired = true, secondsLeft = 0, offline = false) }
                    }
                }
            }
        }
    }

    private fun startTimer() {
        timerJob = viewModelScope.launch {
            while (_state.value.secondsLeft > 0 && _state.value.receivedLink == null) {
                delay(1_000)
                _state.update { it.copy(secondsLeft = (it.secondsLeft - 1).coerceAtLeast(0), expired = it.secondsLeft - 1 <= 0) }
            }
        }
    }

    /** Descarta a sessão no servidor. Fora do escopo da tela: também roda quando ela já fechou. */
    private fun releaseCurrent() {
        val session = current ?: return
        current = null
        appScope.launch { receiver.release(session) }
    }

    override fun onCleared() {
        releaseCurrent()
    }
}
