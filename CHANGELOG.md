# Changelog

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
