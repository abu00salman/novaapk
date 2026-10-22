# Changelog

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
