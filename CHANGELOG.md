# Changelog

## 2.1.0-beta1 — NovaDrive NEXT

**New**
- In-app navigation: destination search (OpenStreetMap Nominatim), road routing (OSRM), the route drawn on every map, turn-by-turn guidance with distance/ETA/arrival, automatic recalculation off route, Arabic/English voice prompts, full-screen interactive map (drag, pinch, zoom, follow/heading-up). The trip keeps running across interfaces and other apps and resumes after a restart. «خذني إلى …» now routes inside NovaDrive.
- Eight new interfaces: Classic, Mercedes (MBUX-inspired), Ford (SYNC-inspired), Apple Future Car, Jetour, BYD, Haval and TANK — each with its own layout. Light/dark, colour, wallpaper (presets or own photo) and font are saved per interface. Voice: «واجهة كلاسيك/تسلا/…».
- Fixed shortcut icons for CarPlay, Music, Phone and Maps in every interface, whichever app they open.

**Fixed**
- Brightness works on units that ignore Android's brightness (screen dimmer, remembered).
- Head unit Bluetooth music shows title/artist and is controllable through the player's own notification buttons.
- Maps no longer need an API key (OpenStreetMap tiles).
- Notification-access prompt in Tesla/BMW media cards; side bar on the right by default.

**Unchanged by design**: Tesla and BMW layouts (verified by before/after screenshot comparison; only live map content, clock and the fixed shortcut icons differ).

## Unreleased — Tesla and BMW interfaces

- Two more choices in Settings › Appearance › Interface style, saved per profile: **Tesla** (light, full screen: car photo with GPS speed, large live map with destination search and shortcuts, media/weather/clock cards, brightness · dock · volume bar) and **BMW** (dark cockpit: speed cluster with heading, live dark map with the guidance card, time/weather, player, CarPlay card, volume and brightness either side of the dock).
- The live map is a real street map (OpenStreetMap data, CARTO tiles) around the GPS position, cached on the unit; pinch or double-tap to zoom, tap to open the navigation app.

## Unreleased — Simple interface & spoken «أمرني»

- New **Simple interface** (Settings › Appearance › Interface style): navigation, music, clock/weather and car info side by side above a large dock. The choice is saved per profile and survives restarts.
- CarPlay dock button uses the CarPlay icon; it still launches ZLink/DiPlay exactly as before.
- «أمرني» now answers out loud (device text-to-speech, Arabic when installed) with a toggle, a voice test and a shortcut to voice settings.
- Many new voice commands: show map/music/split, simple/smart interface, settings, apps, vehicle, quick controls, Bluetooth, Wi-Fi, ETA and next turn (from the navigation app's own guidance), where am I, what's playing, mute, volume level, full/minimum brightness, auto theme, greetings and help.

## 2.0.0 — NovaDrive NEXT (2026-10)
- Home modes **Car / Navigation / Music / Split** driven by a ViewModel state machine with a saved
  preference. A verified route (the navigation app's guidance notification) promotes Car → Navigation;
  when guidance ends it returns to Car unless Music or Split was pinned. Switching modes only changes
  layout — playback and navigation audio are never touched.
- Large mode switcher (≥ 56 dp) always visible, including during playback.
- Car view: original illustration of a white 2019 Hyundai Sonata (front three-quarter), GPS
  speedometer, OBD-II readings only when connected ("--" / "غير متصل" otherwise).
- Navigation view: live turn-by-turn mirrored from Google Maps / Waze with one-tap hand-off; no
  embedded or simulated map (no licensed map SDK on this unit).
- Music view: large artwork and controls, seek only when the session supports it, helpful empty state.
- Split view: navigation and music side by side.
- App library: Favorites / Navigation / Media / Car / Tools / All tabs, 4–6 columns, two-line
  names, system apps hidden by default, favorites ordering, search only when parked.
- Dock: up to 6 configurable actions, duplicates removed (e.g. ZLink serving CarPlay and Android
  Auto), video apps hidden and blocked while driving.
- Removed infinite animations; static level meter.
- Tests: mode state machine, ViewModel route lifecycle, dock/app filtering; Roborazzi screenshots
  for all modes in day and night, empty media, no GPS/OBD, app tabs and 1024×600.
