package com.wpdas.primetv.ui.screens.update

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wpdas.primetv.domain.update.AppUpdate
import com.wpdas.primetv.domain.update.DownloadProgress
import com.wpdas.primetv.domain.update.UpdateCheck
import com.wpdas.primetv.domain.update.UpdateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

sealed interface UpdateUiState {
    data object Checking : UpdateUiState
    data class Available(val update: AppUpdate, val needsPermission: Boolean = false) : UpdateUiState
    data class Downloading(val update: AppUpdate, val downloaded: Long, val total: Long) : UpdateUiState {
        val fraction: Float get() = if (total > 0) (downloaded.toFloat() / total).coerceIn(0f, 1f) else 0f
    }
    data class ReadyToInstall(val update: AppUpdate, val file: File) : UpdateUiState
    data object UpToDate : UpdateUiState
    data class Error(val messageKey: String, val detail: String?) : UpdateUiState
}

@HiltViewModel
class UpdateViewModel @Inject constructor(private val updates: UpdateRepository) : ViewModel() {

    private val _state = MutableStateFlow<UpdateUiState>(UpdateUiState.Checking)
    val state: StateFlow<UpdateUiState> = _state.asStateFlow()
    private var downloadJob: Job? = null

    init {
        check()
    }

    fun check() {
        _state.value = UpdateUiState.Checking
        viewModelScope.launch {
            _state.value = when (val result = updates.check()) {
                is UpdateCheck.Available -> UpdateUiState.Available(result.update)
                UpdateCheck.UpToDate -> UpdateUiState.UpToDate
                is UpdateCheck.Failed -> UpdateUiState.Error(result.messageKey, result.detail)
            }
        }
    }

    fun downloadAndInstall() {
        val update = (_state.value as? UpdateUiState.Available)?.update ?: return
        if (!updates.canInstall()) {
            _state.value = UpdateUiState.Available(update, needsPermission = true)
            return
        }
        _state.value = UpdateUiState.Downloading(update, 0, update.apkSizeBytes)
        downloadJob = viewModelScope.launch {
            updates.download(update).collect { progress ->
                _state.value = when (progress) {
                    is DownloadProgress.Running -> UpdateUiState.Downloading(update, progress.downloadedBytes, progress.totalBytes)
                    is DownloadProgress.Done -> UpdateUiState.ReadyToInstall(update, progress.file).also { updates.install(progress.file) }
                    is DownloadProgress.Failed -> UpdateUiState.Error(progress.messageKey, progress.detail)
                }
            }
        }
    }

    fun openPermission() = updates.openInstallPermission()
    fun openDateSettings() = updates.openDateSettings()

    /** Ao voltar da tela de permissão: se liberou, segue para o download. */
    fun onResumed() {
        val s = _state.value
        if (s is UpdateUiState.Available && s.needsPermission && updates.canInstall()) downloadAndInstall()
    }

    fun installAgain() {
        (_state.value as? UpdateUiState.ReadyToInstall)?.let { updates.install(it.file) }
    }

    fun cancelDownload() {
        downloadJob?.cancel()
        val update = (_state.value as? UpdateUiState.Downloading)?.update ?: return
        _state.value = UpdateUiState.Available(update)
    }
}
