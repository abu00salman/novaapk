package com.abu00salman.novadrive.core

/** What occupies the centre of the home screen. */
enum class HomeMode { CAR, NAVIGATION, MUSIC, SPLIT }

/**
 * @property pinned the user explicitly chose MUSIC or SPLIT; route start/end never overrides it.
 * @property autoPromoted NAVIGATION was entered because a route started (not by the user).
 */
data class ModeState(val mode: HomeMode = HomeMode.CAR, val pinned: Boolean = false, val autoPromoted: Boolean = false)

/**
 * Pure state machine for the home layout. It only decides what is drawn: nothing here touches
 * media playback, audio focus or the navigation app, so switching never interrupts either.
 *
 * Rules:
 *  - Default is CAR when there is no active route.
 *  - A verified route start promotes CAR → NAVIGATION (if auto-switching is enabled).
 *  - When guidance ends, NAVIGATION returns to CAR. Pinned MUSIC / SPLIT stay as they are.
 *  - Any manual choice wins until the next route start/end transition.
 */
object ModeMachine {
    fun select(mode: HomeMode): ModeState =
        ModeState(mode, pinned = mode == HomeMode.MUSIC || mode == HomeMode.SPLIT, autoPromoted = false)

    fun routeStarted(s: ModeState, autoSwitch: Boolean): ModeState =
        if (autoSwitch && !s.pinned && s.mode == HomeMode.CAR) ModeState(HomeMode.NAVIGATION, pinned = false, autoPromoted = true) else s

    fun routeEnded(s: ModeState): ModeState =
        if (!s.pinned && s.mode == HomeMode.NAVIGATION) ModeState(HomeMode.CAR) else s

    /** Start-up: a saved MUSIC/SPLIT pin is kept; otherwise CAR, or NAVIGATION if a route is already running. */
    fun restore(saved: HomeMode?, routeActive: Boolean, autoSwitch: Boolean): ModeState {
        val base = if (saved == HomeMode.MUSIC || saved == HomeMode.SPLIT) select(saved) else ModeState()
        return if (routeActive) routeStarted(base, autoSwitch) else base
    }
}
