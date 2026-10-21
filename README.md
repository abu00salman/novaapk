# NOVA DRIVE — Next‑Generation Smart Car Launcher

Native Android car launcher (Kotlin + Jetpack Compose) designed for the
**Tech Caravan Android 16 head unit** (Allwinner A133, 4 GB RAM, 1280×720).

**Developed by abu00salman**

---

## العربية — طريقة البناء والتثبيت

### ١) بناء ملف APK تلقائياً على GitHub (بدون كمبيوتر)
1. أنشئ مستودعاً جديداً على GitHub باسم `novadrive` في حساب **abu00salman**.
2. ارفع محتويات هذا المجلد كما هي (بما فيها مجلد `.github`).
3. افتح تبويب **Actions** ← سيبدأ «Build NOVA DRIVE APK» تلقائياً (≈ ٦–١٠ دقائق).
4. عند انتهائه افتح التشغيل ← قسم **Artifacts** ← حمّل `NovaDrive-APK`.
5. لإصدار رسمي: أنشئ وسماً `v1.0.0` وسيظهر الـ APK في صفحة **Releases**.

### ٢) التثبيت على شاشة السيارة
1. انقل ملف الـ APK للشاشة (فلاشة USB أو حمّله من Chrome على الشاشة).
2. افتحه من مدير الملفات واسمح بـ «التثبيت من مصادر غير معروفة».
3. اضغط زر **Home** ← اختر **NOVA DRIVE** ← **دائماً**.
4. أول تشغيل: اسمح بالموقع (للسرعة والوضع الليلي والطقس).
5. من الإعدادات ← الأذونات: فعّل «الوصول لمعلومات الوسائط» و«تعديل إعدادات النظام».

### ٣) الرجوع للواجهة الأصلية
الإعدادات ← **المشغّل** ← «اختر تطبيق الشاشة الرئيسية الافتراضي» أو افتح المشغّل الأصلي مباشرة.
أو من إعدادات أندرويد: التطبيقات ← التطبيقات الافتراضية ← تطبيق الشاشة الرئيسية.
لا يتم تعديل أي شيء في النظام — حذف التطبيق يعيد كل شيء كما كان.

---

## English — Build & install

### Build (GitHub Actions)
Push this folder to a GitHub repo → **Actions** builds `assembleRelease` →
download the **NovaDrive-APK** artifact. Tag `v*` to attach the APK to a Release.

### Build (Android Studio)
Open the folder (Ladybug 2024.2+ / JDK 17) → *Build › Build APK(s)*, or
`./gradlew :app:assembleRelease` → `app/build/outputs/apk/release/`.

### Stable signing (recommended)
Without a keystore each CI run is signed with a fresh debug key, so an update
requires uninstalling first. To keep updates seamless, create a key once:

```bash
keytool -genkeypair -v -keystore nova.jks -alias nova -keyalg RSA -keysize 2048 -validity 10000
base64 -w0 nova.jks   # copy output
```
Add repo secrets: `NOVA_KEYSTORE_BASE64`, `NOVA_KEYSTORE_PASSWORD`, `NOVA_KEY_ALIAS` (=nova), `NOVA_KEY_PASSWORD`.

---

## What's real (no fake data)

| Feature | Android API | Notes |
|---|---|---|
| Speed | `LocationManager` GPS `Location.speed` | distance/time fallback |
| Auto day/night | Offline solar‑altitude calculation | uses last GPS position |
| Weather | Open‑Meteo (no key) | cached, 30‑min refresh, offline‑first |
| Media | `MediaSessionManager` + media keys | keys work without permission (BT Music) |
| CarPlay | Launches ZLink/DiPlay; USB vendor 0x05AC detection | wireless CarPlay can't be observed |
| Split screen | `FLAG_ACTIVITY_LAUNCH_ADJACENT` | depends on unit's multi‑window |
| Volume / brightness | `AudioManager` / `Settings.System` | brightness falls back to window‑only |
| Vehicle data | ELM327 Bluetooth, SAE J1979 Mode 01 | **read‑only**, opt‑in |
| Voice | `RecognizerIntent` + AR/EN command parser | needs a recognizer (Google app) |
| Profiles | Per‑profile theme, language, favorites, shortcuts | |
| Crash recovery | Crash counter → Safe Mode + restore stock launcher | |

## Architecture
```
core/   Settings (per-profile), CrashGuard, SunCalc
data/   Apps, Location, Weather, Media, CarPlay, SystemControls, OBD, Voice
ui/     theme · components (Glass) · home (Horizon, widgets, dock, split)
        apps (library) · quick (controls) · settings
```
Performance choices for the A133/Mali‑G31: no real‑time blur (layered gradients instead),
single‑Canvas Horizon animation that invalidates draw only (never recomposition),
112 px icon cache, lazy subsystems, R8 minify + resource shrinking.

## Safety
NOVA never touches vehicle controls, CAN bus or the head unit's MCU. OBD support only
reads standard diagnostic values. Settings, search and editing lock above 8 km/h.
