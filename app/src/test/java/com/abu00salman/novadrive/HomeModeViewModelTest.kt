package com.abu00salman.novadrive

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.abu00salman.novadrive.core.HomeMode
import com.abu00salman.novadrive.core.SettingsRepository
import com.abu00salman.novadrive.ui.home.HomeModeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Route lifecycle through the ViewModel: promote, grace period on end, pinned modes, persistence. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = Application::class)
class HomeModeViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    private fun repo() = SettingsRepository(ApplicationProvider.getApplicationContext()).also {
        it.update { s -> s.copy(homeMode = "CAR", autoNavMode = true) }
    }

    @Test fun routeStartAndEnd() = runTest(dispatcher) {
        val route = MutableStateFlow(false)
        val vm = HomeModeViewModel(repo(), route, endGraceMs = 5_000)
        runCurrent()
        assertEquals(HomeMode.CAR, vm.state.value.mode)
        route.value = true; runCurrent()
        assertEquals(HomeMode.NAVIGATION, vm.state.value.mode)
        // a brief gap between guidance notifications must not drop the navigation view
        route.value = false; advanceTimeBy(2_000); runCurrent()
        route.value = true; runCurrent()
        assertEquals(HomeMode.NAVIGATION, vm.state.value.mode)
        route.value = false; advanceTimeBy(6_000); runCurrent()
        assertEquals(HomeMode.CAR, vm.state.value.mode)
    }

    @Test fun pinnedMusicIsKeptAndPersisted() = runTest(dispatcher) {
        val settings = repo()
        val route = MutableStateFlow(false)
        val vm = HomeModeViewModel(settings, route, endGraceMs = 1_000)
        runCurrent()
        vm.select(HomeMode.MUSIC)
        route.value = true; runCurrent()
        assertEquals(HomeMode.MUSIC, vm.state.value.mode)
        route.value = false; advanceTimeBy(2_000); runCurrent()
        assertEquals(HomeMode.MUSIC, vm.state.value.mode)
        assertEquals("MUSIC", settings.current.homeMode)
        // a new ViewModel (e.g. after process restart) restores the pin
        assertEquals(HomeMode.MUSIC, HomeModeViewModel(settings, MutableStateFlow(false)).state.value.mode)
    }
}
