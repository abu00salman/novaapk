package com.abu00salman.novadrive

import com.abu00salman.novadrive.data.SpeedCamera
import com.abu00salman.novadrive.data.SpeedCameraRepository
import com.abu00salman.novadrive.data.nav.LatLon
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class SpeedCameraTest {
    private val here = LatLon(24.7000, 46.7000)
    // ~330 m north of the car.
    private val north = SpeedCamera(LatLon(24.7030, 46.7000), 80, null)

    @Test fun cameraAheadIsReported() {
        val a = SpeedCameraRepository.ahead(here, 0f, listOf(north))
        assertNotNull(a); assertEquals(333.0, a!!.distanceM, 5.0)
    }
    @Test fun cameraBehindIsIgnored() = assertNull(SpeedCameraRepository.ahead(here, 180f, listOf(north)))
    @Test fun oppositeDirectionCameraIsIgnored() =
        assertNull(SpeedCameraRepository.ahead(here, 0f, listOf(north.copy(direction = 180f))))
    @Test fun noHeadingNoAlert() = assertNull(SpeedCameraRepository.ahead(here, null, listOf(north)))
    @Test fun angleWraps() = assertEquals(20f, SpeedCameraRepository.angle(350f, 10f), 0.01f)
}
