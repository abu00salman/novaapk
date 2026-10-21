package com.abu00salman.novadrive.ui.settings

import android.Manifest
import android.app.role.RoleManager
import android.os.Build
import android.util.DisplayMetrics
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PhoneIphone
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abu00salman.novadrive.AppContainer
import com.abu00salman.novadrive.BuildConfig
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.unit.sp
import com.abu00salman.novadrive.core.CarBrands
import com.abu00salman.novadrive.core.CarPaints
import com.abu00salman.novadrive.core.carBrand
import com.abu00salman.novadrive.core.carModel
import com.abu00salman.novadrive.ui.home.drawCar
import com.abu00salman.novadrive.core.CrashGuard
import com.abu00salman.novadrive.core.Lang
import com.abu00salman.novadrive.core.NovaSettings
import com.abu00salman.novadrive.core.ThemeMode
import com.abu00salman.novadrive.data.AppEntry
import com.abu00salman.novadrive.data.MediaRepository
import com.abu00salman.novadrive.data.VehicleData
import com.abu00salman.novadrive.ui.components.AppPickerDialog
import com.abu00salman.novadrive.ui.components.Glass
import com.abu00salman.novadrive.ui.components.Pill
import com.abu00salman.novadrive.ui.home.rememberShortcuts
import com.abu00salman.novadrive.ui.theme.Accents
import com.abu00salman.novadrive.ui.theme.LocalArabic
import com.abu00salman.novadrive.ui.theme.LocalNova
import com.abu00salman.novadrive.ui.theme.NovaType
import com.abu00salman.novadrive.ui.theme.tr
import java.util.Locale

private enum class Section { CAR, APPEARANCE, SHORTCUTS, CARPLAY, DRIVING, VEHICLE, PROFILES, SETUP, LAUNCHER, ABOUT }

@Composable
fun SettingsScreen(c: AppContainer, s: NovaSettings, driving: Boolean, onClose: () -> Unit) {
    val p = LocalNova.current
    var section by rememberSaveable { mutableStateOf(Section.CAR) }

    Box(
        Modifier.fillMaxSize().background(p.bgBottom.copy(alpha = 0.97f))
            .clickable(remember { MutableInteractionSource() }, null) { }
    ) {
        Row(Modifier.fillMaxSize().padding(20.dp)) {
            // Navigation column
            Column(Modifier.width(250.dp).fillMaxHeight()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Glass(Modifier.size(48.dp), corner = 16.dp, onClick = onClose) {
                        Icon(Icons.Rounded.Close, tr("Close", "إغلاق"), tint = p.text, modifier = Modifier.align(Alignment.Center))
                    }
                    Spacer(Modifier.width(14.dp))
                    Text(tr("Settings", "الإعدادات"), style = NovaType.display.copy(fontSize = NovaType.title.fontSize * 1.5f), color = p.text)
                }
                Spacer(Modifier.height(18.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    val items = listOf(
                        Triple(Section.CAR, Icons.Rounded.DirectionsCar, Pair("My car", "سيارتي")),
                        Triple(Section.APPEARANCE, Icons.Rounded.Palette, Pair("Appearance", "المظهر")),
                        Triple(Section.SHORTCUTS, Icons.Rounded.Widgets, Pair("Shortcuts", "الاختصارات")),
                        Triple(Section.CARPLAY, Icons.Rounded.PhoneIphone, Pair("CarPlay", "كاربلاي")),
                        Triple(Section.DRIVING, Icons.Rounded.DirectionsCar, Pair("Driving", "القيادة")),
                        Triple(Section.VEHICLE, Icons.Rounded.Speed, Pair("Vehicle (OBD-II)", "المركبة (OBD-II)")),
                        Triple(Section.PROFILES, Icons.Rounded.Person, Pair("Profiles", "الملفات الشخصية")),
                        Triple(Section.SETUP, Icons.Rounded.VerifiedUser, Pair("Permissions", "الأذونات")),
                        Triple(Section.LAUNCHER, Icons.Rounded.Restore, Pair("Launcher", "المشغّل")),
                        Triple(Section.ABOUT, Icons.Rounded.Info, Pair("About", "حول")),
                    )
                    items.forEach { (sec, icon, names) ->
                        item {
                            NavItem(icon, tr(names.first, names.second), section == sec) { section = sec }
                        }
                    }
                }
            }
            Spacer(Modifier.width(20.dp))
            Glass(Modifier.weight(1f).fillMaxHeight(), corner = 28.dp) {
                if (driving && section != Section.ABOUT) {
                    Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Rounded.Lock, null, tint = p.accent, modifier = Modifier.size(56.dp))
                        Spacer(Modifier.height(12.dp))
                        Text(tr("Settings are available when parked", "الإعدادات متاحة عند التوقف"), style = NovaType.title, color = p.text)
                        Text(tr("Keep your eyes on the road.", "انتبه للطريق."), style = NovaType.body, color = p.textDim)
                    }
                } else {
                    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 26.dp, vertical = 22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        item {
                            when (section) {
                                Section.CAR -> CarSection(c, s)
                                Section.APPEARANCE -> Appearance(c, s)
                                Section.SHORTCUTS -> ShortcutsSection(c, s)
                                Section.CARPLAY -> CarPlaySection(c, s)
                                Section.DRIVING -> DrivingSection(c, s)
                                Section.VEHICLE -> VehicleSection(c, s)
                                Section.PROFILES -> ProfilesSection(c, s)
                                Section.SETUP -> PermissionsSection(c)
                                Section.LAUNCHER -> LauncherSection(c)
                                Section.ABOUT -> AboutSection(c)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------------ building blocks

@Composable
private fun NavItem(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    val p = LocalNova.current
    Row(
        Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(16.dp))
            .background(if (selected) p.accent.copy(alpha = 0.18f) else p.accent.copy(alpha = 0f))
            .clickable(onClick = onClick).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = if (selected) p.accent else p.textDim, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(14.dp))
        Text(label, style = NovaType.body, color = if (selected) p.text else p.textDim, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun Header(title: String, subtitle: String? = null) {
    val p = LocalNova.current
    Column(Modifier.padding(top = 6.dp, bottom = 6.dp)) {
        Text(title, style = NovaType.title, color = p.text)
        if (subtitle != null) Text(subtitle, style = NovaType.label, color = p.textDim)
    }
}

@Composable
private fun RowItem(title: String, subtitle: String? = null, onClick: (() -> Unit)? = null, trailing: @Composable () -> Unit = {}) {
    val p = LocalNova.current
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .background(p.text.copy(alpha = 0.04f))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = NovaType.body, color = p.text)
            if (subtitle != null) Text(subtitle, style = NovaType.label, color = p.textDim)
        }
        Spacer(Modifier.width(12.dp))
        trailing()
    }
}

@Composable
private fun Toggle(title: String, subtitle: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    val p = LocalNova.current
    RowItem(title, subtitle, onClick = { onChange(!checked) }) {
        Switch(
            checked, onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = p.accent, checkedThumbColor = p.bgBottom)
        )
    }
}

@Composable
private fun <T> Choice(options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit) {
    val p = LocalNova.current
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (value, label) ->
            Pill(label, if (value == selected) p.accent else p.textDim) { onSelect(value) }
        }
    }
}

@Composable
private fun Gap() = Spacer(Modifier.height(10.dp))

@Composable
private fun Field(label: String, value: String, onChange: (String) -> Unit) {
    val p = LocalNova.current
    var text by remember(value) { mutableStateOf(value) }
    OutlinedTextField(
        value = text, onValueChange = { text = it; onChange(it) },
        label = { Text(label) }, singleLine = true, modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = p.text, unfocusedTextColor = p.text,
            focusedBorderColor = p.accent, unfocusedBorderColor = p.stroke,
            focusedLabelColor = p.accent, unfocusedLabelColor = p.textDim, cursorColor = p.accent,
        )
    )
}

// ------------------------------------------------------------------ sections

@Composable
private fun Chip(text: String, selected: Boolean, onClick: () -> Unit) {
    val p = LocalNova.current
    Glass(Modifier.height(42.dp), corner = 21.dp, tint = if (selected) p.accent else null, onClick = onClick) {
        Text(text, style = NovaType.label, color = p.text, maxLines = 1, modifier = Modifier.align(Alignment.Center).padding(horizontal = 18.dp))
    }
}

@Composable
private fun CarSection(c: AppContainer, s: NovaSettings) {
    val p = LocalNova.current
    val ar = LocalArabic.current
    val brand = carBrand(s.carBrand)
    val model = carModel(s.carBrand, s.carModel)
    val paint = CarPaints.getOrElse(s.carPaint) { CarPaints[0] }
    Column {
        Header(tr("My car", "سيارتي"), tr("Shown on your dashboard.", "تظهر في لوحة القيادة."))
        Glass(Modifier.fillMaxWidth().height(210.dp), corner = 26.dp) {
            androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
                val w = minOf(size.width * 0.5f, size.height * 0.95f)
                drawCar(model, paint.color, size.width / 2, size.height * 0.92f, w, p.dark, p.accent)
            }
            Text(
                "${if (ar) brand.ar else brand.en}  ${model.name}  ·  ${if (ar) paint.ar else paint.en}",
                style = NovaType.label, color = p.textDim, modifier = Modifier.align(Alignment.TopCenter).padding(top = 12.dp)
            )
        }
        Gap()
        Header(tr("Brand", "الشركة"))
        androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(CarBrands.size) { i ->
                val b = CarBrands[i]
                Chip(if (ar) b.ar else b.en, b.id == s.carBrand) {
                    c.settings.update { it.copy(carBrand = b.id, carModel = b.models[0].id) }
                }
            }
        }
        Gap()
        Header(tr("Model", "الموديل"))
        androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(brand.models.size) { i ->
                val m = brand.models[i]
                Chip(m.name, m.id == model.id) { c.settings.update { it.copy(carModel = m.id) } }
            }
        }
        Gap()
        Header(tr("Color", "اللون"))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            CarPaints.forEachIndexed { i, cp ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier.size(48.dp).clip(CircleShape).background(cp.color)
                            .border(if (s.carPaint == i) 3.dp else 1.dp, if (s.carPaint == i) p.accent else p.stroke, CircleShape)
                            .clickable { c.settings.update { it.copy(carPaint = i) } }
                    )
                    Text(tr(cp.en, cp.ar), style = NovaType.label.copy(fontSize = 10.sp), color = p.textDim, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun Appearance(c: AppContainer, s: NovaSettings) {
    val p = LocalNova.current
    Column {
        Header(tr("Theme", "السمة"), tr("Auto follows sunrise and sunset at your location.", "التلقائي يتبع الشروق والغروب حسب موقعك."))
        Choice(
            listOf(ThemeMode.AUTO to tr("Auto", "تلقائي"), ThemeMode.LIGHT to tr("Day", "نهاري"),
                ThemeMode.DARK to tr("Night", "ليلي"), ThemeMode.SYSTEM to tr("System", "النظام")),
            s.themeMode
        ) { m -> c.settings.update { it.copy(themeMode = m) } }
        Gap()
        Header(tr("Accent color", "اللون المميّز"))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Accents.forEachIndexed { i, a ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier.size(52.dp).clip(CircleShape)
                            .background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(a.color, a.second)))
                            .border(if (s.accent == i) 3.dp else 0.dp, p.text, CircleShape)
                            .clickable { c.settings.update { it.copy(accent = i) } }
                    ) { if (s.accent == i) Icon(Icons.Rounded.Check, null, tint = p.bgBottom, modifier = Modifier.align(Alignment.Center)) }
                    Text(tr(a.en, a.ar), style = NovaType.label, color = p.textDim, maxLines = 1)
                }
            }
        }
        Gap()
        Header(tr("Language", "اللغة"))
        Choice(listOf(Lang.AR to "العربية", Lang.EN to "English"), s.language) { l -> c.settings.update { it.copy(language = l) } }
        Gap()
        Toggle(tr("24-hour clock", "نظام 24 ساعة"), null, s.use24h) { v -> c.settings.update { it.copy(use24h = v) } }
        Gap()
        Toggle(tr("Weather on dashboard", "الطقس في لوحة القيادة"), tr("Open-Meteo, refreshed every 30 min, cached offline", "من Open-Meteo، يتحدث كل 30 دقيقة ومحفوظ دون اتصال"), s.showWeather) { v ->
            c.settings.update { it.copy(showWeather = v) }
        }
    }
}

@Composable
private fun ShortcutsSection(c: AppContainer, s: NovaSettings) {
    val apps by c.apps.apps.collectAsStateWithLifecycle()
    val sc = rememberShortcuts(c, s, apps)
    var picking by remember { mutableStateOf<String?>(null) }
    val auto = tr("auto", "تلقائي")
    fun label(chosen: String?, resolved: AppEntry?) =
        (resolved?.label ?: "—") + if (chosen == null) " ($auto)" else ""
    val ar = LocalArabic.current

    Column {
        Header(tr("Dashboard shortcuts", "اختصارات لوحة القيادة"), tr("Leave on automatic to detect installed apps.", "اتركها تلقائية لاكتشاف التطبيقات المثبتة."))
        RowItem(tr("Navigation", "الملاحة"), label(s.navPackage, sc.nav), onClick = { picking = "nav" })
        Gap(); RowItem(tr("Music", "الموسيقى"), label(s.musicPackage, sc.music), onClick = { picking = "music" })
        Gap(); RowItem(tr("Radio", "الراديو"), label(s.radioPackage, sc.radio), onClick = { picking = "radio" })
        Gap(); RowItem(tr("Phone", "الهاتف"), label(s.phonePackage, sc.phone), onClick = { picking = "phone" })
        Gap()
        Header(tr("Saved places", "الأماكن المحفوظة"), tr("Shown as one-tap navigation buttons.", "تظهر كأزرار توجيه بلمسة واحدة."))
        Field(tr("Home address", "عنوان البيت"), s.homeAddress) { v -> c.settings.update { it.copy(homeAddress = v) } }
        Gap()
        Field(tr("Work address", "عنوان العمل"), s.workAddress) { v -> c.settings.update { it.copy(workAddress = v) } }
    }
    picking?.let { what ->
        AppPickerDialog(if (ar) "اختر التطبيق" else "Choose app", apps, onPick = { a ->
            val pkg = a?.packageName
            c.settings.update {
                when (what) {
                    "nav" -> it.copy(navPackage = pkg)
                    "music" -> it.copy(musicPackage = pkg)
                    "radio" -> it.copy(radioPackage = pkg)
                    else -> it.copy(phonePackage = pkg)
                }
            }
            picking = null
        }, onDismiss = { picking = null })
    }
}

@Composable
private fun CarPlaySection(c: AppContainer, s: NovaSettings) {
    val p = LocalNova.current
    val apps by c.apps.apps.collectAsStateWithLifecycle()
    val sc = rememberShortcuts(c, s, apps)
    val iPhone by c.iPhoneConnected.collectAsStateWithLifecycle()
    var picking by remember { mutableStateOf(false) }
    Column {
        Header("Apple CarPlay", tr("NOVA launches your unit's own CarPlay app (ZLink / DiPlay). It does not replace it.",
            "نوفا يشغّل تطبيق كاربلاي الموجود في جهازك (ZLink / DiPlay) ولا يستبدله."))
        RowItem(tr("CarPlay app", "تطبيق كاربلاي"), sc.carPlay?.label ?: tr("Not found — choose manually", "غير موجود — اختره يدوياً"), onClick = { picking = true })
        Gap()
        RowItem(tr("iPhone on USB", "آيفون عبر USB"), if (iPhone) tr("Connected", "متصل") else tr("Not detected", "غير متصل")) {
            Box(Modifier.size(14.dp).clip(CircleShape).background(if (iPhone) p.ok else p.textDim))
        }
        Gap()
        Toggle(tr("Open CarPlay when iPhone is plugged in", "افتح كاربلاي عند توصيل الآيفون"),
            tr("Wired only. Wireless CarPlay is handled privately by the CarPlay app.", "للتوصيل السلكي فقط. الاتصال اللاسلكي يديره تطبيق كاربلاي نفسه."),
            s.autoLaunchCarPlay) { v -> c.settings.update { it.copy(autoLaunchCarPlay = v) } }
        Gap()
        RowItem(tr("Open CarPlay now", "افتح كاربلاي الآن"), onClick = { sc.carPlay?.let { c.apps.launch(it) } })
    }
    if (picking) AppPickerDialog(tr("CarPlay app", "تطبيق كاربلاي"), apps, onPick = { a ->
        c.settings.update { it.copy(carPlayPackage = a?.packageName) }; picking = false
    }, onDismiss = { picking = false })
}

@Composable
private fun DrivingSection(c: AppContainer, s: NovaSettings) {
    Column {
        Header(tr("Distraction-free driving", "قيادة بدون تشتيت"),
            tr("Above 8 km/h: settings lock, search and editing are disabled, touch targets grow.",
                "فوق 8 كم/س: تُقفل الإعدادات ويتوقف البحث والتعديل وتكبر الأزرار."))
        Toggle(tr("Driving mode", "وضع القيادة"), tr("Uses real GPS speed", "يعتمد على سرعة GPS الحقيقية"), s.drivingMode) { v ->
            c.settings.update { it.copy(drivingMode = v) }
        }
    }
}

@Composable
private fun VehicleSection(c: AppContainer, s: NovaSettings) {
    val p = LocalNova.current
    val data by c.obd.data.collectAsStateWithLifecycle()
    var refresh by remember { mutableStateOf(0) }
    val btPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { c.refreshPermissions(); refresh++ }
    val devices = remember(refresh) { c.obd.pairedDevices() }
    Column {
        Header(tr("OBD-II adapter (optional)", "محول OBD-II (اختياري)"),
            tr("Read-only: RPM, coolant and battery from a paired ELM327 Bluetooth adapter. NOVA never writes to the vehicle.",
                "قراءة فقط: الدوران والحرارة والبطارية من محول ELM327 مقترن بالبلوتوث. نوفا لا يكتب أي شيء للمركبة."))
        Toggle(tr("Enable OBD-II", "تفعيل OBD-II"), null, s.obdEnabled) { v ->
            c.settings.update { it.copy(obdEnabled = v) } // connection is managed by NovaRoot
        }
        Gap()
        if (!c.obd.hasPermission()) {
            RowItem(tr("Allow Bluetooth access", "اسمح بالوصول للبلوتوث"), onClick = {
                if (Build.VERSION.SDK_INT >= 31) btPerm.launch(Manifest.permission.BLUETOOTH_CONNECT)
            })
        } else {
            Header(tr("Paired adapters", "المحولات المقترنة"), tr("Pair the adapter in Bluetooth settings first.", "اقرن المحول من إعدادات البلوتوث أولاً."))
            if (devices.isEmpty()) RowItem(tr("No paired devices", "لا توجد أجهزة مقترنة"), onClick = { c.controls.openBluetooth() })
            devices.forEach { (name, addr) ->
                RowItem(name, addr, onClick = {
                    c.settings.update { it.copy(obdAddress = addr, obdEnabled = true) }
                }) { if (s.obdAddress == addr) Icon(Icons.Rounded.Check, null, tint = p.accent) }
                Gap()
            }
        }
        Gap()
        RowItem(
            tr("Status", "الحالة"),
            when (data.state) {
                VehicleData.State.OFF -> tr("Off", "متوقف")
                VehicleData.State.CONNECTING -> tr("Connecting…", "جارٍ الاتصال…")
                VehicleData.State.LIVE -> "RPM ${data.rpm ?: "–"} · ${data.coolantC ?: "–"}° · ${data.voltage ?: "–"}V"
                VehicleData.State.ERROR -> data.error ?: tr("Error", "خطأ")
            }
        )
    }
}

@Composable
private fun ProfilesSection(c: AppContainer, s: NovaSettings) {
    val p = LocalNova.current
    var newName by remember { mutableStateOf("") }
    Column {
        Header(tr("Driver profiles", "ملفات السائقين"), tr("Each profile keeps its own theme, language, favorites and shortcuts.",
            "لكل ملف سمته ولغته ومفضلته واختصاراته."))
        s.profileNames.forEachIndexed { i, name ->
            RowItem(name, if (i == s.profileId) tr("Active", "نشط") else null, onClick = { c.settings.switchProfile(i) }) {
                if (i == s.profileId) Icon(Icons.Rounded.Check, null, tint = p.accent)
            }
            Gap()
        }
        Field(tr("Rename active profile", "إعادة تسمية الملف النشط"), s.profileName) { v -> if (v.isNotBlank()) c.settings.renameProfile(s.profileId, v) }
        Gap()
        Field(tr("New profile name", "اسم ملف جديد"), newName) { newName = it }
        Gap()
        Pill(tr("Add profile", "إضافة ملف"), p.accent) { if (newName.isNotBlank()) { c.settings.addProfile(newName); newName = "" } }
    }
}

@Composable
private fun PermissionsSection(c: AppContainer) {
    val p = LocalNova.current
    val ctx = LocalContext.current
    val perms by c.permissions.collectAsStateWithLifecycle()
    val locLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { c.refreshPermissions() }
    val hasLoc = remember(perms) { c.location.hasPermission() }
    val hasMedia = remember(perms) { MediaRepository.hasAccess(ctx) }
    val canWrite = remember(perms) { c.controls.canWriteSettings() }

    @Composable
    fun status(ok: Boolean) = Box(Modifier.size(14.dp).clip(CircleShape).background(if (ok) p.ok else p.danger))

    Column {
        Header(tr("Permissions", "الأذونات"), tr("Each one unlocks a real feature. Nothing is collected or uploaded.", "كل إذن يفعّل ميزة حقيقية. لا يتم جمع أو رفع أي بيانات."))
        RowItem(tr("Location (GPS)", "الموقع (GPS)"), tr("Speed, auto night mode, weather", "السرعة، الوضع الليلي التلقائي، الطقس"), onClick = {
            locLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }) { status(hasLoc) }
        Gap()
        RowItem(tr("Media info access", "الوصول لمعلومات الوسائط"), tr("Track title & artwork (notification access)", "اسم المقطع والصورة (الوصول للإشعارات)"),
            onClick = { c.controls.openNotificationAccess() }) { status(hasMedia) }
        Gap()
        RowItem(tr("Modify system settings", "تعديل إعدادات النظام"), tr("System-wide brightness slider", "التحكم بسطوع النظام"),
            onClick = { c.controls.requestWriteSettings() }) { status(canWrite) }
        Gap()
        RowItem(tr("Location services", "خدمات الموقع"), tr("Make sure GPS is switched on", "تأكد أن GPS مفعّل"), onClick = { c.controls.openLocationSettings() })
    }
}

@Composable
private fun LauncherSection(c: AppContainer) {
    val p = LocalNova.current
    val ctx = LocalContext.current
    val perms by c.permissions.collectAsStateWithLifecycle()
    val roleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { c.refreshPermissions() }
    val isDefault = remember(perms) { c.controls.isDefaultLauncher() }
    val others = remember { c.controls.otherLaunchers() }
    val crash = remember { CrashGuard.lastCrash(ctx) }
    Column {
        Header(tr("Home launcher", "مشغّل الشاشة الرئيسية"))
        RowItem(
            tr("Set NOVA DRIVE as default", "اجعل نوفا درايف الافتراضي"),
            if (isDefault) tr("NOVA is your home screen", "نوفا هو الشاشة الرئيسية") else tr("Not default yet", "ليس الافتراضي بعد"),
            onClick = {
                val rm = if (Build.VERSION.SDK_INT >= 29) ctx.getSystemService(RoleManager::class.java) else null
                if (rm != null && Build.VERSION.SDK_INT >= 29 && rm.isRoleAvailable(RoleManager.ROLE_HOME) && !rm.isRoleHeld(RoleManager.ROLE_HOME)) {
                    roleLauncher.launch(rm.createRequestRoleIntent(RoleManager.ROLE_HOME))
                } else c.controls.openHomeSettings()
            }
        ) { if (isDefault) Icon(Icons.Rounded.Check, null, tint = p.ok) }
        Gap()
        Header(tr("Restore original launcher", "استعادة المشغّل الأصلي"), tr("Switch back any time — nothing in the system is modified.", "يمكنك الرجوع في أي وقت — لا يتم تعديل أي شيء في النظام."))
        RowItem(tr("Choose default home app", "اختر تطبيق الشاشة الرئيسية الافتراضي"), tr("Opens Android's Home app setting", "يفتح إعداد تطبيق الشاشة الرئيسية في أندرويد"), onClick = { c.controls.openHomeSettings() })
        Gap()
        others.forEach { ri ->
            RowItem(tr("Open ", "افتح ") + c.controls.label(ri), ri.activityInfo.packageName, onClick = { c.controls.openLauncher(ri) })
            Gap()
        }
        if (c.safeMode) {
            Header(tr("Safe mode is ON", "الوضع الآمن مفعّل"), tr("Started after repeated crashes. Optional features are paused.", "بدأ بعد أعطال متكررة. الميزات الاختيارية متوقفة."))
            Pill(tr("Exit safe mode (restart)", "الخروج من الوضع الآمن (إعادة تشغيل)"), p.accent) {
                CrashGuard.exitSafeMode(ctx)
                android.os.Process.killProcess(android.os.Process.myPid())
            }
            Gap()
        }
        if (crash != null) {
            Header(tr("Last crash report", "آخر تقرير عطل"))
            Text(crash.take(900), style = NovaType.label, color = p.textDim)
        }
    }
}

@Composable
private fun AboutSection(c: AppContainer) {
    val p = LocalNova.current
    val ctx = LocalContext.current
    val dm: DisplayMetrics = ctx.resources.displayMetrics
    Column {
        Text("NOVA DRIVE", style = NovaType.display, color = p.text)
        Text(tr("Next-generation car launcher", "مشغّل سيارات من الجيل القادم"), style = NovaType.body, color = p.accent)
        Gap()
        RowItem(tr("Version", "الإصدار"), "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
        Gap()
        RowItem(tr("Device", "الجهاز"), "${Build.MANUFACTURER} ${Build.MODEL} · Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
        Gap()
        RowItem(tr("Display", "الشاشة"), "${dm.widthPixels}×${dm.heightPixels} · ${dm.densityDpi} dpi")
        Gap()
        RowItem(tr("Memory", "الذاكرة"), String.format(Locale.US, "%.1f GB RAM", c.controls.totalRamGb()))
        Gap()
        Spacer(Modifier.height(18.dp))
        Text(tr("Developed by abu00salman", "تطوير abu00salman"), style = NovaType.title, color = p.text)
        Text("github.com/abu00salman", style = NovaType.label, color = p.textDim)
    }
}
