package com.wpdas.primetv

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wpdas.primetv.domain.repository.PlaylistRepository
import com.wpdas.primetv.ui.navigation.HomeRoute
import com.wpdas.primetv.ui.navigation.SetupRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.take
import javax.inject.Inject

/** Decide a tela inicial: Configuração inicial sem lista salva, Início com lista. */
@HiltViewModel
class MainViewModel @Inject constructor(playlistRepository: PlaylistRepository) : ViewModel() {
    val startDestination: StateFlow<Any?> = playlistRepository.source
        .take(1)
        .map<Any?, Any?> { source -> if (source == null) SetupRoute else HomeRoute() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
}
