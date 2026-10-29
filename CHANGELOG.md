# Changelog

## 2.3.0-beta2

- «أمرني» is back on the voice button: Google's official speech recognition with local commands, or Google Assistant / Gemini; other questions open in the Google app. No API.
- Nova AI moved to Settings › Experimental and is off by default.
- BMW: day mode (night stays the default) and YouTube beside Music in its bottom apps.
- New Widgets interface: 18 live gauges/readings, phone-style sizes and flow, edit mode (add, remove, resize, reorder), GPS trip computer, Hijri date, sunrise/sunset.

## 2.3.0-beta1 — Nova AI voice repair

- Voice session state machine with numbered sessions, timeouts and cancellation: the microphone never stays stuck on "listening" or "speaking".
- Speech recognition inside NovaDrive (no Google window; the window remains as a fallback option).
- Replies are spoken reliably: per-utterance completion, best installed Arabic voice, rate/volume/channel settings, audio focus.
- One selected provider (ChatGPT or Claude), restored after restart; clear API error messages; Stop cancels the request.
- Offline mode: local commands without internet or API credit, typing and quick commands when speech needs internet.
- Local commands match whole words and leave general questions to the AI; compound commands supported.
- New Nova AI panel and settings with real diagnostics. Acceptance tests for the voice pipeline.

## 2.2.0 — NovaDrive NEXT (official)

**New**
- **Nova AI** replaces «أمرني»: local commands first (instant, offline), then — optionally — ChatGPT (OpenAI) or Claude (Anthropic) with real tools: open apps, play/pause/next, play a song or station by name, volume, brightness, navigate to an address or the nearest station/pharmacy/café…, trip status, switch interface/theme, weather, news headlines, open the dialer for a contact (you press Call). Shows listening / thinking / doing / speaking, can be stopped mid-answer, remembers the last few turns for follow-ups, and works in Arabic (incl. Saudi dialect). «يا نوفا …» prefix supported.
- AI keys are entered by the user in Settings › Nova AI and stored encrypted with the Android Keystore — no key ships inside the APK. Optional backend proxy URL. Location is shared with the AI only when enabled.
- Tesla: «صف الشاشة» is now «تقسيم»; YouTube icon next to Music in the bar; Maps / YouTube switch in the map card (YouTube beside NovaDrive in split screen, or full screen; disabled while driving).
- Classic (and Ford): the big phone shortcut is customizable — long-press › choose any app, restore the default phone, large-icon option; also from Settings. Independent of the dock's phone icon; a removed app shows a clear message.
- Fixed YouTube icon added to the fixed CarPlay / Music / Phone / Maps icons.
- Mercedes, Ford, Apple (concept), Jetour, BYD, Haval and TANK redesigned closer to their real systems (full-screen map layer, SYNC tiles, CarPlay-like sidebar, speed arc, real inclinometer from the device sensor in TANK…).

**Fixed**
- Dock hides apps that are not installed (unless an app store is available to install them).
- Voice commands that weren't understood are no longer answered with a wrong action.

**Unchanged by design**: Tesla and BMW layouts (pixel comparison: only the label, the YouTube icon and the Maps/YouTube switch differ in Tesla).

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
