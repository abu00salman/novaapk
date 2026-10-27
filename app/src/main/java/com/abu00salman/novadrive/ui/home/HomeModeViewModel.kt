package com.abu00salman.novadrive.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.abu00salman.novadrive.core.HomeMode
import com.abu00salman.novadrive.core.ModeMachine
import com.abu00salman.novadrive.core.ModeState
import com.abu00salman.novadrive.core.SettingsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch

/**
 * Holds the home-screen mode. [routeActive] must come from a verified guidance source (the
 * navigation app's ongoing guidance notification) — never from "Maps is in the foreground".
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeModeViewModel(
    private val settings: SettingsRepository,
    routeActive: Flow<Boolean>,
    endGraceMs: Long = 6_000,
) : ViewModel() {
    private val _state = MutableStateFlow(ModeMachine.restore(savedMode(), routeActive = false, autoSwitch = settings.current.autoNavMode))
    val state: StateFlow<ModeState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            // Guidance notifications are briefly re-posted between steps; only treat a route as
            // ended once it has stayed gone for a short grace period.
            routeActive.distinctUntilChanged().transformLatest { active ->
                if (!active) delay(endGraceMs)
                emit(active)
            }.distinctUntilChanged().collect { active ->
                _state.value = if (active) ModeMachine.routeStarted(_state.value, settings.current.autoNavMode)
                else ModeMachine.routeEnded(_state.value)
            }
        }
    }

    fun select(mode: HomeMode) {
        _state.value = ModeMachine.select(mode)
        settings.update { it.copy(homeMode = mode.name) }
    }

    private fun savedMode(): HomeMode? = runCatching { HomeMode.valueOf(settings.current.homeMode) }.getOrNull()

    class Factory(private val settings: SettingsRepository, private val route: Flow<Boolean>) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = HomeModeViewModel(settings, route) as T
    }
}
