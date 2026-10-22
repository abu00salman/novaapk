package com.abu00salman.novadrive

import com.abu00salman.novadrive.core.HomeMode
import com.abu00salman.novadrive.core.ModeMachine
import com.abu00salman.novadrive.core.ModeState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeModeTest {
    @Test fun defaultsToCarWithoutRoute() {
        assertEquals(HomeMode.CAR, ModeMachine.restore(null, routeActive = false, autoSwitch = true).mode)
        assertEquals(HomeMode.CAR, ModeMachine.restore(HomeMode.NAVIGATION, routeActive = false, autoSwitch = true).mode)
    }

    @Test fun routeStartPromotesCarToNavigation() {
        val s = ModeMachine.routeStarted(ModeState(), autoSwitch = true)
        assertEquals(HomeMode.NAVIGATION, s.mode)
        assertTrue(s.autoPromoted)
    }

    @Test fun routeStartRespectsDisabledAutoSwitch() {
        assertEquals(HomeMode.CAR, ModeMachine.routeStarted(ModeState(), autoSwitch = false).mode)
    }

    @Test fun routeEndReturnsNavigationToCar() {
        val nav = ModeMachine.routeStarted(ModeState(), true)
        assertEquals(HomeMode.CAR, ModeMachine.routeEnded(nav).mode)
        // a manually opened navigation view also returns to car when guidance finishes
        assertEquals(HomeMode.CAR, ModeMachine.routeEnded(ModeMachine.select(HomeMode.NAVIGATION)).mode)
    }

    @Test fun pinnedMusicAndSplitSurviveRouteStartAndEnd() {
        for (m in listOf(HomeMode.MUSIC, HomeMode.SPLIT)) {
            val pinned = ModeMachine.select(m)
            assertTrue(pinned.pinned)
            assertEquals(m, ModeMachine.routeStarted(pinned, true).mode)
            assertEquals(m, ModeMachine.routeEnded(ModeMachine.routeStarted(pinned, true)).mode)
        }
    }

    @Test fun manualCarDuringRouteIsKept() {
        val s = ModeMachine.select(HomeMode.CAR)
        assertFalse(s.pinned)
        // no transition happens until the route state changes again
        assertEquals(HomeMode.CAR, s.mode)
    }

    @Test fun restoreKeepsPinnedModeEvenWithActiveRoute() {
        assertEquals(HomeMode.MUSIC, ModeMachine.restore(HomeMode.MUSIC, routeActive = true, autoSwitch = true).mode)
        assertEquals(HomeMode.NAVIGATION, ModeMachine.restore(HomeMode.CAR, routeActive = true, autoSwitch = true).mode)
    }
}
