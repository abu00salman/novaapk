package com.abu00salman.novadrive

import android.app.Application
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.abu00salman.novadrive.core.StyleOptions
import com.abu00salman.novadrive.core.StyleOptionsRepository
import com.abu00salman.novadrive.core.UiStyles
import com.abu00salman.novadrive.data.nav.Geo
import com.abu00salman.novadrive.data.nav.Instructions
import com.abu00salman.novadrive.data.nav.LatLon
import com.abu00salman.novadrive.data.nav.RouteService
import com.abu00salman.novadrive.data.nav.RouteStep
import com.abu00salman.novadrive.ui.nav.parseCoordinates
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = Application::class)
class NavLogicTest {

    private val osrm = """
        {"code":"Ok","routes":[{"distance":1520.4,"duration":180.2,
          "geometry":{"type":"LineString","coordinates":[[42.55,16.88],[42.552,16.881],[42.556,16.884]]},
          "legs":[{"steps":[
            {"distance":400,"duration":50,"name":"King Fahd Rd","maneuver":{"type":"depart","modifier":"right","location":[42.55,16.88]}},
            {"distance":800,"duration":90,"name":"","ref":"40","maneuver":{"type":"turn","modifier":"left","location":[42.552,16.881]}},
            {"distance":300,"duration":40,"name":"Corniche","maneuver":{"type":"roundabout","modifier":"right","exit":2,"location":[42.554,16.883]}},
            {"distance":0,"duration":0,"name":"","maneuver":{"type":"arrive","location":[42.556,16.884]}}
          ]}]}]}
    """.trimIndent()

    @Test fun parsesOsrmRoute() {
        val r = RouteService.parseRoute(osrm)
        assertNotNull(r)
        r!!
        assertEquals(3, r.points.size)
        assertEquals(16.88, r.points[0].lat, 1e-9)
        assertEquals(42.55, r.points[0].lon, 1e-9)
        assertEquals(4, r.steps.size)
        assertEquals("40", r.steps[1].road)
        assertEquals(2, r.steps[2].exit)
        assertEquals(1520.4, r.distanceM, 0.01)
    }

    @Test fun rejectsFailedRoute() {
        assertNull(RouteService.parseRoute("""{"code":"NoRoute","routes":[]}"""))
        assertNull(RouteService.parseRoute("not json"))
    }

    @Test fun parsesNominatimPlaces() {
        val places = RouteService.parsePlaces("""[{"name":"جامعة جازان","display_name":"جامعة جازان, جازان, السعودية","lat":"16.95","lon":"42.62"},
            {"name":"","display_name":"Corniche, Jazan","lat":"16.88","lon":"42.54"},{"display_name":"bad"}]""")
        assertEquals(2, places.size)
        assertEquals("جامعة جازان", places[0].name)
        assertEquals("Corniche", places[1].name)
    }

    @Test fun geodesyIsAccurate() {
        // Jazan → Abha is ~ 148 km great-circle.
        val d = Geo.distance(LatLon(16.8892, 42.5511), LatLon(18.2164, 42.5053))
        assertTrue(d in 145_000.0..150_000.0)
        assertEquals(90.0, Geo.bearing(LatLon(0.0, 0.0), LatLon(0.0, 1.0)), 0.5)
        val (off, t) = Geo.toSegment(LatLon(0.0005, 0.5), LatLon(0.0, 0.0), LatLon(0.0, 1.0))
        assertEquals(55.6, off, 1.0)
        assertEquals(0.5, t, 0.001)
    }

    @Test fun formatsDistancesForDrivers() {
        assertEquals("850 م", Geo.formatDistance(840.0, true))
        assertEquals("1.5 km", Geo.formatDistance(1520.0, false))
        assertEquals("12 دقيقة", Geo.formatDuration(720.0, true))
    }

    @Test fun writesArabicInstructions() {
        fun step(type: String, mod: String, road: String = "", exit: Int? = null) = RouteStep(LatLon(0.0, 0.0), type, mod, road, 0.0, 0.0, exit)
        assertEquals("انعطف يمينًا إلى طريق الملك فهد", Instructions.text(step("turn", "right", "طريق الملك فهد"), true))
        assertEquals("في الدوار خذ المخرج الثاني", Instructions.text(step("roundabout", "right", exit = 2), true))
        assertEquals("Turn left onto Main St", Instructions.text(step("turn", "left", "Main St"), false))
        assertEquals("الوصول إلى الوجهة", Instructions.text(step("arrive", ""), true))
    }

    @Test fun parsesTypedCoordinates() {
        assertEquals(LatLon(16.88, 42.55), parseCoordinates("16.88, 42.55"))
        assertNull(parseCoordinates("جازان"))
        assertNull(parseCoordinates("99.0, 42.0"))
    }

    @Test fun styleOptionsRoundTrip() {
        val o = StyleOptions(dark = true, accent = 3, wallpaper = StyleOptions.WALLPAPER_IMAGE, font = 1, wallpaperVersion = 42)
        assertEquals(o, StyleOptionsRepository.decode(StyleOptionsRepository.encode(o)))
        assertEquals(StyleOptions(), StyleOptionsRepository.decode(null))
        assertEquals(StyleOptions(dark = null), StyleOptionsRepository.decode("|0|0|0|0"))
    }

    @Test fun novaAiToolsAreWellFormed() {
        val specs = com.abu00salman.novadrive.data.ai.NovaTools.specs
        assertEquals(specs.size, specs.map { it.name }.toSet().size)
        specs.forEach { t ->
            assertTrue(t.name, Regex("^[a-z_]{3,40}$").matches(t.name))
            assertTrue(t.name, t.description.isNotBlank())
            assertTrue(t.name, t.properties.keys.containsAll(t.required))
        }
        assertTrue(specs.any { it.name == "switch_interface" } && specs.any { it.name == "navigate_nearby" })
    }

    @Test fun bigShortcutOptionRoundTrip() {
        val o = StyleOptions(bigApp = "com.google.android.youtube", bigSize = 1)
        assertEquals(o, StyleOptionsRepository.decode(StyleOptionsRepository.encode(o)))
        assertEquals(null, StyleOptionsRepository.decode(StyleOptionsRepository.encode(StyleOptions())).bigApp)
    }

    @Test fun twelveInterfacesWithFixedTeslaAndBmw() {
        assertEquals(12, UiStyles.all.size)
        assertEquals(12, UiStyles.all.map { it.id }.toSet().size)
        assertTrue(UiStyles.all.filter { it.customizable }.map { it.id }.containsAll(listOf("classic", "mercedes", "ford", "apple", "jetour", "byd", "haval", "tank")))
        assertTrue(!UiStyles.get("tesla").customizable && !UiStyles.get("bmw").customizable)
        assertEquals("smart", UiStyles.get("unknown").id)
    }
}
