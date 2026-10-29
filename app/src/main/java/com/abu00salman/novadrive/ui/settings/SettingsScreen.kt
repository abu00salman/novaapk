package com.abu00salman.novadrive.ui.settings

import com.abu00salman.novadrive.core.carDisplayName
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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Mic
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
import androidx.compose.ui.graphics.Color
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
import com.abu00salman.novadrive.core.CarImageStore
import com.abu00salman.novadrive.ui.components.CarImage
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.abu00salman.novadrive.core.CrashGuard
import com.abu00salman.novadrive.core.DOCK_CHOICES
import com.abu00salman.novadrive.core.DOCK_MAX
import com.abu00salman.novadrive.core.CUSTOM_PREFIX
import com.abu00salman.novadrive.core.replaceDockEntry
import com.abu00salman.novadrive.ui.home.dockTitle
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ArrowDownward
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

private enum class Section { CAR, APPEARANCE, SHORTCUTS, VOICE, CARPLAY, DRIVING, VEHICLE, PROFILES, SETUP, LAUNCHER, EXPERIMENTAL, ABOUT }

@Composable
fun SettingsScreen(c: AppContainer, s: NovaSettings, driving: Boolean, start: String? = null, onClose: () -> Unit) {
    val p = LocalNova.current
    var section by rememberSaveable(start) {
        mutableStateOf(when (start) { "vehicle" -> Section.VEHICLE; "shortcuts" -> Section.SHORTCUTS; "appearance" -> Section.APPEARANCE; else -> Section.CAR })
    }

    Box(
        Modifier.fillMaxSize().background(p.bgBottom.copy(alpha = 0.97f))
            .clickable(remember { MutableInteractionSource() }, null) { }
            // Only Settings makes room for the keyboard; the rest of NovaDrive keeps its size.
            .windowInsetsPadding(androidx.compose.foundation.layout.WindowInsets.ime)
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
                        Triple(Section.CAR, Icons.Rounded.DirectionsCar, Pair("Car", "السيارة")),
                        Triple(Section.APPEARANCE, Icons.Rounded.Palette, Pair("Appearance", "المظهر")),
                        Triple(Section.SHORTCUTS, Icons.Rounded.Widgets, Pair("Shortcuts", "الاختصارات")),
                        Triple(Section.VOICE, Icons.Rounded.Mic, Pair("Ask me (voice)", "أمرني")),
                        Triple(Section.CARPLAY, Icons.Rounded.PhoneIphone, Pair("CarPlay", "كاربلاي")),
                        Triple(Section.DRIVING, Icons.Rounded.DirectionsCar, Pair("Driving", "القيادة")),
                        Triple(Section.VEHICLE, Icons.Rounded.Speed, Pair("Vehicle (OBD-II)", "المركبة (OBD-II)")),
                        Triple(Section.PROFILES, Icons.Rounded.Person, Pair("Profiles", "الملفات الشخصية")),
                        Triple(Section.SETUP, Icons.Rounded.VerifiedUser, Pair("Permissions", "الأذونات")),
                        Triple(Section.LAUNCHER, Icons.Rounded.Restore, Pair("Launcher", "المشغّل")),
                        Triple(Section.EXPERIMENTAL, Icons.Rounded.Science, Pair("Experimental", "تجريبي")),
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
                                Section.VOICE -> VoiceSection(c, s)
                                Section.EXPERIMENTAL -> ExperimentalSection(c, s)
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

/** Light/dark, accent, wallpaper and font of one of the eight new interfaces, saved per interface. */
/** BMW keeps its design; only night/day and YouTube in the bottom apps can be changed. */
@Composable
private fun BmwOptions(c: AppContainer) {
    val all by c.styleOptions.all.collectAsStateWithLifecycle()
    val o = all["bmw"] ?: com.abu00salman.novadrive.core.StyleOptions()
    Column {
        Header(tr("BMW interface", "واجهة BMW"), tr("Saved for this interface only.", "يُحفظ لهذه الواجهة فقط."))
        Choice(listOf(true to tr("Night", "ليلي"), false to tr("Day", "نهاري")), o.dark ?: true) { v ->
            c.styleOptions.update("bmw") { it.copy(dark = v) }
        }
        Gap()
        Toggle(tr("YouTube in the bottom apps", "YouTube ضمن التطبيقات بالأسفل"),
            tr("Next to Music; hidden while driving.", "بجانب الموسيقى، ويختفي أثناء القيادة."), o.showYoutube) { v ->
            c.styleOptions.update("bmw") { it.copy(showYoutube = v) }
        }
    }
}

@Composable
private fun StyleCustomizer(c: AppContainer, style: com.abu00salman.novadrive.core.UiStyle) {
    val p = LocalNova.current
    val ar = LocalArabic.current
    val all by c.styleOptions.all.collectAsStateWithLifecycle()
    val o = all[style.id] ?: com.abu00salman.novadrive.core.StyleOptions()
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    fun set(block: (com.abu00salman.novadrive.core.StyleOptions) -> com.abu00salman.novadrive.core.StyleOptions) = c.styleOptions.update(style.id, block)
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            val ok = withContext(Dispatchers.IO) { CarImageStore.import(ctx, uri, CarImageStore.wallpaperName(style.id)) }
            if (ok) set { it.copy(wallpaper = com.abu00salman.novadrive.core.StyleOptions.WALLPAPER_IMAGE, wallpaperVersion = System.currentTimeMillis()) }
            else com.abu00salman.novadrive.ui.toast(ctx, if (ar) "تعذّر قراءة الصورة" else "Couldn't read that image")
        }
    }
    Column {
        Header(tr("Customise ", "تخصيص ") + (if (ar) style.ar else style.en), tr("Saved for this interface only.", "يُحفظ لهذه الواجهة فقط."))
        Choice(listOf(0 to tr("Interface default", "افتراضي الواجهة"), 1 to tr("Light", "فاتح"), 2 to tr("Dark", "داكن")),
            when (o.dark) { null -> 0; false -> 1; true -> 2 }) { v -> set { it.copy(dark = when (v) { 1 -> false; 2 -> true; else -> null }) } }
        Gap()
        Text(tr("Colour", "اللون"), style = NovaType.label, color = p.textDim)
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            style.accents.forEachIndexed { i, col ->
                Box(
                    Modifier.size(48.dp).clip(CircleShape).background(androidx.compose.ui.graphics.Color(col))
                        .border(if (o.accent == i) 3.dp else 0.dp, p.text, CircleShape).clickable { set { it.copy(accent = i) } }
                ) { if (o.accent == i) Icon(Icons.Rounded.Check, null, tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.align(Alignment.Center)) }
            }
        }
        Gap()
        Text(tr("Background", "الخلفية"), style = NovaType.label, color = p.textDim)
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Pill(tr("Default", "الافتراضية"), if (o.wallpaper == 0) p.accent else p.textDim) { set { it.copy(wallpaper = 0) } }
            com.abu00salman.novadrive.ui.styles.WallpaperPresets.forEachIndexed { i, colors ->
                Box(
                    Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(androidx.compose.ui.graphics.Brush.verticalGradient(colors))
                        .border(if (o.wallpaper == i + 1) 3.dp else 1.dp, if (o.wallpaper == i + 1) p.accent else p.stroke, RoundedCornerShape(12.dp))
                        .clickable { set { it.copy(wallpaper = i + 1) } }
                )
            }
            Pill(tr("My photo", "صورتي"), if (o.wallpaper == com.abu00salman.novadrive.core.StyleOptions.WALLPAPER_IMAGE) p.accent else p.textDim) {
                picker.launch(arrayOf("image/png", "image/webp", "image/jpeg", "image/*"))
            }
        }
        if (style.id == "classic" || style.id == "ford") {
            Gap()
            val apps by c.apps.apps.collectAsStateWithLifecycle()
            var picking by remember { mutableStateOf(false) }
            val chosen = o.bigApp?.let { pkg -> apps.firstOrNull { it.packageName == pkg } }
            Text(tr("Big shortcut", "الاختصار الكبير"), style = NovaType.label, color = p.textDim)
            Text(
                when {
                    o.bigApp == null -> tr("Phone (default)", "الهاتف (افتراضي)")
                    chosen == null -> tr("Chosen app isn't installed", "التطبيق المختار غير مثبت")
                    else -> chosen.label
                } + tr(" — separate from the Phone button in the bar", " — مستقل عن زر الهاتف في الشريط"),
                style = NovaType.body, color = p.text,
            )
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Pill(tr("Choose app", "اختيار تطبيق"), p.accent) { picking = true }
                if (o.bigApp != null) Pill(tr("Restore phone", "استعادة الهاتف"), p.textDim) { set { it.copy(bigApp = null) } }
                Pill(if (o.bigSize == 1) tr("Large icon ✓", "أيقونة كبيرة ✓") else tr("Large icon", "أيقونة كبيرة"), if (o.bigSize == 1) p.accent else p.textDim) {
                    set { it.copy(bigSize = if (it.bigSize == 1) 0 else 1) }
                }
            }
            if (picking) com.abu00salman.novadrive.ui.components.AppPickerDialog(
                tr("Customise shortcut", "تخصيص الاختصار"), apps, allowNone = true,
                onPick = { a -> set { it.copy(bigApp = a?.packageName) }; picking = false },
                onDismiss = { picking = false },
                noneLabel = tr("Restore the default phone", "استعادة الهاتف الافتراضي"),
            )
        }
        Gap()
        Text(tr("Font", "الخط"), style = NovaType.label, color = p.textDim)
        Spacer(Modifier.height(6.dp))
        Choice(listOf(0 to tr("NovaDrive", "خط NovaDrive"), 1 to tr("System", "خط النظام")), o.font) { v -> set { it.copy(font = v) } }
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
        val ctx = LocalContext.current
        val scope = rememberCoroutineScope()
        val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) scope.launch {
                val ok = withContext(Dispatchers.IO) { CarImageStore.import(ctx, uri) }
                if (ok) c.settings.update { it.copy(carImageVersion = System.currentTimeMillis()) }
                else com.abu00salman.novadrive.ui.toast(ctx, if (ar) "تعذّر قراءة الصورة" else "Couldn't read that image")
            }
        }
        Header(tr("Customize car image", "تخصيص صورة السيارة"),
            tr("PNG or WebP with a transparent background works best. Shown fitted, never cropped.",
                "يفضّل PNG أو WebP بخلفية شفافة. تُعرض كاملة دون قص أو تشويه."))
        Glass(Modifier.fillMaxWidth().height(220.dp), corner = 26.dp) {
            CarImage(s.carImageVersion, Modifier.fillMaxSize().padding(16.dp))
        }
        Gap()
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Pill(tr("Choose image", "اختيار صورة"), p.accent) { picker.launch(arrayOf("image/png", "image/webp", "image/jpeg", "image/*")) }
            if (s.carImageVersion != 0L) Pill(tr("Remove image", "إزالة الصورة"), p.danger) {
                CarImageStore.clear(ctx); c.settings.update { it.copy(carImageVersion = 0L) }
            }
        }
        Gap()
        Text("${carDisplayName(brand, model, ar)}  ·  ${if (ar) paint.ar else paint.en}", style = NovaType.label, color = p.textDim)
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
                Chip(if (ar) m.nameAr else m.name, m.id == model.id) { c.settings.update { it.copy(carModel = m.id) } }
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
        Header(tr("Interface", "الواجهات"),
            tr("Thirteen home interfaces. Tesla and BMW keep their design (BMW offers day/night); the others can be customised below. Your choice is saved.",
                "ثلاث عشرة واجهة. Tesla وBMW بتصميمهما (مع ليلي/نهاري لـBMW)، وبقية الواجهات قابلة للتخصيص بالأسفل. يُحفظ اختيارك تلقائيًا."))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            com.abu00salman.novadrive.core.UiStyles.all.chunked(4).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { st ->
                        Pill(if (LocalArabic.current) st.ar else st.en, if (st.id == s.uiStyle) p.accent else p.textDim) {
                            c.settings.update { it.copy(uiStyle = st.id) }
                        }
                    }
                }
            }
        }
        val current = com.abu00salman.novadrive.core.UiStyles.get(s.uiStyle)
        if (current.customizable) {
            Gap()
            StyleCustomizer(c, current)
        } else if (current.id == "bmw") {
            Gap()
            BmwOptions(c)
        }
        Gap()
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
        Header(tr("Side bar position", "موضع القائمة الجانبية"))
        Choice(listOf(false to tr("Left", "يسار"), true to tr("Right", "يمين")), s.railRight) { v -> c.settings.update { it.copy(railRight = v) } }
        Gap()
        Header(tr("Text size", "حجم الخط"))
        Choice(listOf(1f to "100%", 1.15f to "115%", 1.3f to "130%", 1.45f to "145%"), s.textScale) { v -> c.settings.update { it.copy(textScale = v) } }
        Gap()
        Header(tr("Icon size", "حجم الأيقونات"))
        Choice(listOf(1f to "100%", 1.15f to "115%", 1.3f to "130%", 1.45f to "145%"), s.iconScale) { v -> c.settings.update { it.copy(iconScale = v) } }
        Gap()
        Header(tr("Icon colour", "لون الأيقونات"), tr("Original keeps each app's own colours.", "«الأصلي» يُبقي ألوان التطبيقات كما هي."))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            com.abu00salman.novadrive.ui.components.IconTints.forEachIndexed { i, col ->
                Box(
                    Modifier.size(48.dp).clip(CircleShape)
                        .background(if (col == null) androidx.compose.ui.graphics.Brush.sweepGradient(listOf(Color(0xFFFF4D6D), Color(0xFFFFC53D), Color(0xFF2EE6C0), Color(0xFF3D8BFF), Color(0xFF8B5CFF), Color(0xFFFF4D6D)))
                            else androidx.compose.ui.graphics.SolidColor(col))
                        .border(if (s.iconTint == i) 3.dp else 1.dp, if (s.iconTint == i) p.text else p.stroke, CircleShape)
                        .clickable { c.settings.update { it.copy(iconTint = i) } },
                    contentAlignment = Alignment.Center
                ) { if (s.iconTint == i) Icon(Icons.Rounded.Check, null, tint = if (col == null) Color.White else Color(0xFF0B1630)) }
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
        DockEditor(s, apps, ar,
            onChange = { list -> c.settings.update { it.copy(dock = list) } },
            onAddApp = { picking = "dock" })
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
                    "dock" -> if (pkg == null) it else it.copy(dock = replaceDockEntry(it.dock, it.dock.size, pkg))
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
        Gap()
        Toggle(tr("Show navigation while guiding", "عرض الملاحة أثناء التوجيه"),
            tr("Switches home to Navigation when Maps/Waze starts guidance and back to Car when it ends. Pinned Music or Split stay.",
                "ينتقل للملاحة عند بدء التوجيه في الخرائط أو Waze ويعود للسيارة عند انتهائه. وضعا الموسيقى والتقسيم المثبتان لا يتغيران."),
            s.autoNavMode) { v -> c.settings.update { it.copy(autoNavMode = v) } }
        Gap()
        Toggle(tr("Allow editing while moving", "السماح بالتعديل أثناء الحركة"),
            tr("Search, settings and dock changes stay available above 8 km/h. Video apps remain blocked while moving.",
                "يبقى البحث والإعدادات وتعديل الشريط متاحاً فوق 8 كم/س. تبقى تطبيقات الفيديو محجوبة أثناء الحركة."),
            s.editWhileDriving) { v -> c.settings.update { it.copy(editWhileDriving = v) } }
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
        Text(tr("Developed by iRaMzi", "تطوير iRaMzi"), style = NovaType.title, color = p.text)
        Spacer(Modifier.height(6.dp))
        Pill("X · @iRaMzi7", p.accent) {
            runCatching {
                ctx.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://x.com/iRaMzi7"))
                    .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        }
    }
}


/**
 * The dock in its real order: move each button with the arrows or remove it, and add any of the
 * standard buttons or any installed app below. Max 6.
 */
@Composable
private fun DockEditor(s: NovaSettings, apps: List<AppEntry>, ar: Boolean, onChange: (List<String>) -> Unit, onAddApp: () -> Unit) {
    val p = LocalNova.current
    fun title(id: String) = if (id.startsWith(CUSTOM_PREFIX)) apps.firstOrNull { it.packageName == id.removePrefix(CUSTOM_PREFIX) }?.label ?: id.removePrefix(CUSTOM_PREFIX)
        else dockTitle(id, ar)
    Header(tr("Dock buttons", "أزرار الشريط السفلي"), tr("In order from the driver's side. Up to 6.", "بالترتيب من جهة السائق. حتى 6 أزرار."))
    s.dock.forEachIndexed { i, id ->
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(p.text.copy(alpha = 0.05f)).padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(34.dp).clip(CircleShape).background(p.accent.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
                Text("${i + 1}", style = NovaType.label, color = p.accent)
            }
            Spacer(Modifier.width(12.dp))
            Text(title(id), style = NovaType.body, color = p.text, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            EditButton(Icons.Rounded.ArrowUpward, tr("Move earlier", "تقديم"), enabled = i > 0) {
                onChange(s.dock.toMutableList().also { l -> l.removeAt(i); l.add(i - 1, id) })
            }
            EditButton(Icons.Rounded.ArrowDownward, tr("Move later", "تأخير"), enabled = i < s.dock.lastIndex) {
                onChange(s.dock.toMutableList().also { l -> l.removeAt(i); l.add(i + 1, id) })
            }
            EditButton(Icons.Rounded.Close, tr("Remove", "إزالة"), enabled = true, tint = p.danger) { onChange(s.dock - id) }
        }
        Spacer(Modifier.height(6.dp))
    }
    if (s.dock.size < DOCK_MAX) {
        Spacer(Modifier.height(6.dp))
        Text(tr("Add a button", "إضافة زر"), style = NovaType.label, color = p.textDim)
        Spacer(Modifier.height(6.dp))
        androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val missing = DOCK_CHOICES.filter { it !in s.dock }
            items(missing.size) { i -> Pill("+ " + dockTitle(missing[i], ar), p.accent) { onChange(s.dock + missing[i]) } }
            item { Pill(tr("+ Any app…", "+ تطبيق آخر…"), p.accent2, onClick = onAddApp) }
        }
    }
}

@Composable
private fun EditButton(icon: ImageVector, label: String, enabled: Boolean, tint: androidx.compose.ui.graphics.Color? = null, onClick: () -> Unit) {
    val p = LocalNova.current
    Box(
        Modifier.size(48.dp).clip(CircleShape).then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center
    ) { Icon(icon, label, tint = if (!enabled) p.textDim.copy(alpha = 0.3f) else tint ?: p.text, modifier = Modifier.size(26.dp)) }
}


/** Nova AI › AI provider: exactly one agent answers general questions; saved across restarts. */
@Composable
private fun AiEngineSettings(c: AppContainer) {
    val p = LocalNova.current
    val cfg by c.ai.config.collectAsStateWithLifecycle()
    Header(tr("AI provider", "وكيل الذكاء الاصطناعي"), tr("General questions go only to the provider chosen here — never to several, never switched automatically.",
        "الأسئلة العامة تُرسل فقط للوكيل المختار هنا — ليس لعدة خدمات، ولا يتم التبديل تلقائيًا."))
    Choice(
        listOf(com.abu00salman.novadrive.data.ai.AiProvider.LOCAL to tr("Google / local voice", "Google / الصوت المحلي"),
            com.abu00salman.novadrive.data.ai.AiProvider.OPENAI to "ChatGPT",
            com.abu00salman.novadrive.data.ai.AiProvider.ANTHROPIC to "Claude"),
        cfg.provider,
    ) { v -> c.ai.update { it.copy(provider = v) } }
    Spacer(Modifier.height(6.dp))
    Text(
        when (cfg.provider) {
            com.abu00salman.novadrive.data.ai.AiProvider.LOCAL -> tr("Google is used here to turn speech into text, and Nova's local commands run it. It is not a chat assistant: open questions need ChatGPT or Claude.",
                "يُستخدم Google هنا لتحويل الكلام إلى نص، وتنفّذه أوامر نوفا المحلية. ليس وكيل محادثة: الأسئلة العامة تحتاج ChatGPT أو Claude.")
            com.abu00salman.novadrive.data.ai.AiProvider.OPENAI -> tr("ChatGPT (OpenAI API) answers questions; car commands still run locally.", "ChatGPT (OpenAI API) يجيب على الأسئلة، وأوامر السيارة تبقى محلية.")
            com.abu00salman.novadrive.data.ai.AiProvider.ANTHROPIC -> tr("Claude (Anthropic API) answers questions; car commands still run locally.", "Claude (Anthropic API) يجيب على الأسئلة، وأوامر السيارة تبقى محلية.")
        }, style = NovaType.small, color = p.textDim,
    )
    if (cfg.provider != com.abu00salman.novadrive.data.ai.AiProvider.LOCAL) {
        val openAi = cfg.provider == com.abu00salman.novadrive.data.ai.AiProvider.OPENAI
        Gap()
        KeyField(
            if (openAi) tr("OpenAI API key", "مفتاح OpenAI API") else tr("Anthropic API key", "مفتاح Anthropic API"),
            if (openAi) cfg.hasOpenAiKey else cfg.hasClaudeKey,
        ) { key -> c.ai.setKey(cfg.provider, key) }
        Gap()
        Field(tr("Model", "النموذج"), if (openAi) cfg.openAiModel else cfg.claudeModel) { v ->
            c.ai.update { if (openAi) it.copy(openAiModel = v.trim()) else it.copy(claudeModel = v.trim()) }
        }
        Gap()
        Field(tr("Backend proxy URL (optional)", "رابط خادم وسيط (اختياري)"), cfg.proxyUrl) { v -> c.ai.update { it.copy(proxyUrl = v) } }
        Gap()
        Toggle(tr("Share location with Nova AI", "مشاركة الموقع مع Nova AI"),
            tr("Off: the AI never receives your city or coordinates (navigation still works on the unit).",
                "إيقاف: لا يصل للذكاء الاصطناعي مدينتك أو إحداثياتك (الملاحة تعمل على الجهاز كالمعتاد)."),
            cfg.shareLocation) { v -> c.ai.update { it.copy(shareLocation = v) } }
        Gap()
        Text(
            tr("Keys are stored encrypted on this unit only (Android Keystore), never logged, never backed up and not part of the APK. Or point to your own backend proxy that holds the key. API usage is billed by OpenAI/Anthropic separately from ChatGPT Plus or Claude Pro subscriptions.",
                "يُحفظ المفتاح مشفّرًا على هذا الجهاز فقط (Android Keystore)، لا يُكتب في السجلات ولا يُنسخ احتياطيًا وليس داخل ملف APK، أو استخدم خادمًا وسيطًا خاصًا بك يحفظ المفتاح. استخدام الـ API يُحاسَب لدى OpenAI أو Anthropic بشكل منفصل عن اشتراكات ChatGPT Plus أو Claude Pro."),
            style = NovaType.small, color = p.textDim,
        )
    }
}

/** «أمرني»: Google's official voice → local commands, or Google Assistant / Gemini. No API. */
@Composable
private fun AskMeSettings(c: AppContainer, s: NovaSettings) {
    val p = LocalNova.current
    val ctx = LocalContext.current
    val ar = LocalArabic.current
    val cfg by c.ai.config.collectAsStateWithLifecycle()
    val tts by c.speaker.status.collectAsStateWithLifecycle()
    var ttsR by remember { mutableStateOf<com.abu00salman.novadrive.data.voice.DiagResult?>(null) }
    val scope = rememberCoroutineScope()
    androidx.compose.runtime.LaunchedEffect(Unit) { c.speaker.warmUp() }
    Header(tr("Ask me", "أمرني"), tr("Voice commands through Google's official speech recognition — no API key or subscription. Car commands run on the unit, even offline.",
        "أوامر صوتية عبر التعرف الصوتي الرسمي من Google — دون مفتاح API أو اشتراك. أوامر السيارة تُنفّذ على الجهاز، حتى دون إنترنت."))
    if (cfg.novaEnabled) {
        Text(tr("Nova AI (experimental) is on and handles the voice button. Turn it off in Settings › Experimental to use these options.",
            "Nova AI (تجريبي) مفعّل ويتولى زر الصوت. أوقفه من الإعدادات ← تجريبي لاستخدام هذه الخيارات."), style = NovaType.label, color = p.warn)
        Gap()
    }
    Text(tr("The voice button opens", "زر الصوت يفتح"), style = NovaType.label, color = p.textDim)
    Spacer(Modifier.height(6.dp))
    Choice(listOf(
        com.abu00salman.novadrive.data.ai.VoiceButton.COMMANDS to tr("Google voice + commands", "أوامر Google الصوتية"),
        com.abu00salman.novadrive.data.ai.VoiceButton.ASSISTANT to "Google Assistant",
        com.abu00salman.novadrive.data.ai.VoiceButton.GEMINI to "Gemini",
    ), cfg.voiceButton) { v -> c.ai.update { it.copy(voiceButton = v) } }
    Spacer(Modifier.height(6.dp))
    val hasGoogle = remember { com.abu00salman.novadrive.data.ai.GoogleVoice.hasGoogle(ctx) }
    val hasGemini = remember { com.abu00salman.novadrive.data.ai.GoogleVoice.hasGemini(ctx) }
    StatusLine(hasGoogle, if (hasGoogle) tr("Google app installed", "تطبيق Google مثبت") else tr("Google app not installed — install it for voice and Assistant", "تطبيق Google غير مثبت — ثبّته للتعرف الصوتي والمساعد"))
    StatusLine(hasGemini, if (hasGemini) tr("Gemini installed", "Gemini مثبت") else tr("Gemini not installed (Google Assistant is used instead)", "Gemini غير مثبت (يُستخدم Google Assistant بدلًا منه)"))
    Gap()
    Toggle(tr("Send other questions to Google", "إرسال الأسئلة الأخرى إلى Google"),
        tr("A sentence that isn't a car command (\"what's the capital of Japan?\") opens in the Google app, which answers it.",
            "الجملة التي ليست أمرًا للسيارة («كم عاصمة اليابان؟») تُفتح في تطبيق Google ليجيب عليها."),
        cfg.unknownToGoogle) { v -> c.ai.update { it.copy(unknownToGoogle = v) } }
    Gap()
    Toggle(tr("Answer out loud", "الرد بالصوت"), null, s.voiceReply) { v -> c.settings.update { it.copy(voiceReply = v) } }
    Spacer(Modifier.height(6.dp))
    StatusLine(tts.ready && !tts.failed && (!ar || tts.arabic), when {
        tts.failed -> tr("Text-to-speech engine failed to start", "تعذّر تشغيل محرك النطق")
        !tts.ready -> tr("Text-to-speech starting…", "محرك النطق يبدأ…")
        else -> (tts.engine ?: "TTS") + if (tts.arabic) tr(" · Arabic voice installed", " · الصوت العربي مثبت") else tr(" · no Arabic voice", " · لا يوجد صوت عربي")
    })
    Spacer(Modifier.height(6.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Pill(tr("Test voice", "اختبار الصوت"), p.accent) { scope.launch { ttsR = com.abu00salman.novadrive.data.voice.NovaDiagnostics(c).tts() } }
        Pill(tr("Voice settings", "إعدادات النطق"), p.textDim) { c.speaker.openSettings() }
    }
    ResultLine(ttsR)
}

/** Settings › Experimental: Nova AI (ChatGPT / Claude) — off until switched on here. */
@Composable
private fun ExperimentalSection(c: AppContainer, s: NovaSettings) {
    val p = LocalNova.current
    val cfg by c.ai.config.collectAsStateWithLifecycle()
    Column {
        Header(tr("Experimental", "تجريبي"), tr("Features still being tested. They are off by default and don't affect the rest of NovaDrive.",
            "ميزات ما زالت قيد التجربة. مطفأة افتراضيًا ولا تؤثر على بقية NovaDrive."))
        Toggle("Nova AI", tr("AI voice assistant with ChatGPT or Claude (your own API key). When on, it replaces «Ask me» on the voice button.",
            "مساعد صوتي بالذكاء الاصطناعي عبر ChatGPT أو Claude (بمفتاح API خاص بك). عند تفعيله يحل محل «أمرني» في زر الصوت."),
            cfg.novaEnabled) { v -> c.ai.update { it.copy(novaEnabled = v) }; if (!v) c.assistant.close() }
        if (cfg.novaEnabled) {
            Gap()
            AiEngineSettings(c)
            Gap()
            NovaVoiceSettings(c, s)
        } else {
            Spacer(Modifier.height(6.dp))
            Text(tr("Off: the voice button uses «Ask me» (Google).", "مطفأ: زر الصوت يستخدم «أمرني» (Google)."), style = NovaType.small, color = p.textDim)
        }
    }
}

/** A diagnostic/test result line (green ✓ / red ✗). */
@Composable
private fun ResultLine(r: com.abu00salman.novadrive.data.voice.DiagResult?) {
    val p = LocalNova.current
    if (r == null) return
    Spacer(Modifier.height(4.dp))
    Text((if (r.ok) "✓ " else "✗ ") + r.detail, style = NovaType.label, color = if (r.ok) p.ok else p.danger)
}

@Composable
private fun StatusLine(ok: Boolean, text: String) {
    val p = LocalNova.current
    Text((if (ok) "● " else "○ ") + text, style = NovaType.label, color = if (ok) p.ok else p.warn)
}

/** Nova AI › recognition, voice output, conversation, connection and diagnostics. */
@Composable
private fun NovaVoiceSettings(c: AppContainer, s: NovaSettings) {
    val p = LocalNova.current
    val ar = LocalArabic.current
    val ctx = LocalContext.current
    val cfg by c.ai.config.collectAsStateWithLifecycle()
    val tts by c.speaker.status.collectAsStateWithLifecycle()
    val net by c.network.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val diag = remember { com.abu00salman.novadrive.data.voice.NovaDiagnostics(c) }
    var running by remember { mutableStateOf<String?>(null) }
    var micR by remember { mutableStateOf<com.abu00salman.novadrive.data.voice.DiagResult?>(null) }
    var recR by remember { mutableStateOf<com.abu00salman.novadrive.data.voice.DiagResult?>(null) }
    var ttsR by remember { mutableStateOf<com.abu00salman.novadrive.data.voice.DiagResult?>(null) }
    var apiR by remember { mutableStateOf<com.abu00salman.novadrive.data.voice.DiagResult?>(null) }
    var convR by remember { mutableStateOf<List<String>>(emptyList()) }
    var convDone by remember { mutableStateOf<com.abu00salman.novadrive.data.voice.DiagResult?>(null) }
    val micPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { c.refreshPermissions() }
    androidx.compose.runtime.LaunchedEffect(Unit) { c.speaker.warmUp() }
    fun run(tag: String, block: suspend () -> Unit) {
        if (running != null) return
        running = tag
        scope.launch { try { block() } finally { running = null } }
    }
    val speech = c.assistant.speech
    val online = c.assistant.online()

    // ── Voice recognition
    Header(tr("Voice recognition", "التعرف الصوتي"), tr("Speech is turned into text by the unit's recognizer (Google on most units), then routed to a local command or the AI.",
        "يُحوَّل الكلام إلى نص بخدمة التعرف في الجهاز (Google في أغلب الأجهزة)، ثم يُوجَّه لأمر محلي أو للذكاء الاصطناعي."))
    Choice(listOf(true to tr("Inside NovaDrive", "داخل NovaDrive"), false to tr("Google window", "نافذة Google")), cfg.inAppSpeech) { v -> c.ai.update { it.copy(inAppSpeech = v) } }
    Gap()
    Toggle(tr("Prefer on-device recognition", "تفضيل التعرف على الجهاز (دون إنترنت)"),
        tr("Always tried automatically when there's no internet. Works only if the unit has offline speech for the language.",
            "يُجرَّب تلقائيًا دائمًا عند انقطاع الإنترنت. يعمل فقط إذا كان الجهاز يملك التعرف دون إنترنت لهذه اللغة."),
        cfg.preferOfflineSpeech) { v -> c.ai.update { it.copy(preferOfflineSpeech = v) } }
    Gap()
    StatusLine(speech.available(), if (speech.available()) tr("Speech recognizer installed", "خدمة التعرف الصوتي مثبتة") else tr("No speech recognizer — install the Google app", "لا توجد خدمة تعرف صوتي — ثبّت تطبيق Google"))
    StatusLine(speech.onDeviceAvailable(), if (speech.onDeviceAvailable()) tr("On-device recognition available", "التعرف دون إنترنت متاح") else tr("On-device recognition not reported by this unit", "الجهاز لا يوفّر تعرفًا دون إنترنت"))
    StatusLine(speech.hasMicPermission(), if (speech.hasMicPermission()) tr("Microphone allowed", "الميكروفون مسموح") else tr("Microphone not allowed yet", "الميكروفون غير مسموح بعد"))
    if (!speech.hasMicPermission()) {
        Spacer(Modifier.height(6.dp))
        Pill(tr("Allow microphone", "السماح بالميكروفون"), p.accent) { micPerm.launch(Manifest.permission.RECORD_AUDIO) }
    }
    Gap()

    // ── Voice output
    Header(tr("Voice output", "النطق الصوتي"), tr("Replies are spoken by the unit's own text-to-speech (no paid voice service).", "تُنطق الردود بمحرك النطق في الجهاز نفسه (دون خدمة صوت مدفوعة)."))
    Toggle(tr("Answer out loud", "الرد بالصوت"), null, s.voiceReply) { v -> c.settings.update { it.copy(voiceReply = v) } }
    Gap()
    Choice(listOf(com.abu00salman.novadrive.data.VoiceLang.AUTO to tr("Automatic", "تلقائي"),
        com.abu00salman.novadrive.data.VoiceLang.ARABIC to "العربية", com.abu00salman.novadrive.data.VoiceLang.ENGLISH to "English"), cfg.voice.lang) { v ->
        c.ai.update { it.copy(voice = it.voice.copy(lang = v)) }
    }
    Gap()
    Text(tr("Voice", "الصوت"), style = NovaType.label, color = p.textDim)
    Spacer(Modifier.height(6.dp))
    androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item { Pill(tr("Best installed", "أفضل صوت مثبت"), if (cfg.voice.voiceName.isBlank()) p.accent else p.textDim) { c.ai.update { it.copy(voice = it.voice.copy(voiceName = "")) } } }
        items(tts.voices.size) { i ->
            val (name, label) = tts.voices[i]
            Pill(label, if (cfg.voice.voiceName == name) p.accent else p.textDim) { c.ai.update { it.copy(voice = it.voice.copy(voiceName = name)) } }
        }
    }
    Gap()
    Text(tr("Speed", "سرعة النطق") + " · ${"%.1f".format(cfg.voice.rate)}×", style = NovaType.label, color = p.textDim)
    androidx.compose.material3.Slider(cfg.voice.rate, { v -> c.ai.update { it.copy(voice = it.voice.copy(rate = (v * 10).toInt() / 10f)) } }, valueRange = 0.6f..1.6f)
    Text(tr("Voice volume", "مستوى صوت النطق") + " · ${(cfg.voice.volume * 100).toInt()}%", style = NovaType.label, color = p.textDim)
    androidx.compose.material3.Slider(cfg.voice.volume, { v -> c.ai.update { it.copy(voice = it.voice.copy(volume = v)) } }, valueRange = 0.2f..1f)
    Text(tr("Audio channel", "قناة الصوت"), style = NovaType.label, color = p.textDim)
    Spacer(Modifier.height(6.dp))
    Choice(listOf(com.abu00salman.novadrive.data.VoiceChannel.MEDIA to tr("Media (recommended)", "الوسائط (موصى به)"),
        com.abu00salman.novadrive.data.VoiceChannel.NAVIGATION to tr("Navigation", "الملاحة")), cfg.voice.channel) { v ->
        c.ai.update { it.copy(voice = it.voice.copy(channel = v)) }
    }
    Gap()
    StatusLine(tts.ready && !tts.failed, when {
        tts.failed -> tr("Text-to-speech engine failed to start", "تعذّر تشغيل محرك النطق")
        !tts.ready -> tr("Text-to-speech starting…", "محرك النطق يبدأ…")
        else -> (tts.engine ?: "TTS") + if (tts.arabic) tr(" · Arabic voice installed", " · الصوت العربي مثبت") else tr(" · no Arabic voice", " · لا يوجد صوت عربي")
    })
    Spacer(Modifier.height(6.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Pill(if (running == "tts") tr("Speaking…", "ينطق…") else tr("Test voice", "اختبار الصوت"), p.accent) { run("tts") { ttsR = diag.tts() } }
        Pill(tr("Voice settings", "إعدادات النطق"), p.textDim) { c.speaker.openSettings() }
    }
    ResultLine(ttsR)
    Gap()

    // ── Conversation
    Header(tr("Conversation", "المحادثة"), null)
    Toggle(tr("Keep context", "الاحتفاظ بالسياق"), tr("Follow-up questions understand what was said before (last 6 exchanges).", "أسئلة المتابعة تفهم ما قيل قبلها (آخر 6 تبادلات)."), cfg.keepContext) { v -> c.ai.update { it.copy(keepContext = v) } }
    Gap()
    Toggle(tr("Continue the conversation", "متابعة المحادثة"), tr("After a reply is spoken, Nova listens again for your next question.", "بعد نطق الرد يستمع نوفا مجددًا لسؤالك التالي."), cfg.followUp) { v -> c.ai.update { it.copy(followUp = v) } }
    Spacer(Modifier.height(6.dp))
    Text(tr("Interrupt a reply any time by pressing the microphone or Stop.", "يمكنك مقاطعة الرد في أي وقت بالضغط على الميكروفون أو «إيقاف»."), style = NovaType.small, color = p.textDim)
    Gap()
    Text(tr("Listening time", "مدة الاستماع"), style = NovaType.label, color = p.textDim)
    Spacer(Modifier.height(6.dp))
    Choice(listOf(8 to "8s", 12 to "12s", 20 to "20s"), cfg.listenTimeoutSec) { v -> c.ai.update { it.copy(listenTimeoutSec = v) } }
    Gap()
    Text(tr("Answer timeout", "مهلة الرد"), style = NovaType.label, color = p.textDim)
    Spacer(Modifier.height(6.dp))
    Choice(listOf(20 to "20s", 30 to "30s", 60 to "60s"), cfg.replyTimeoutSec) { v -> c.ai.update { it.copy(replyTimeoutSec = v) } }
    Gap()
    Pill(tr("Clear conversation", "مسح المحادثة"), p.danger) { c.assistant.clearConversation(); com.abu00salman.novadrive.ui.toast(ctx, if (ar) "تم مسح المحادثة" else "Conversation cleared") }
    Gap()

    // ── Connection
    Header(tr("Connection", "الاتصال"), null)
    StatusLine(online, if (online) tr("Internet connected", "الإنترنت متصل") + (if (net.wifi == com.abu00salman.novadrive.data.Link.CONNECTED) " · Wi-Fi" else if (net.cellular) " · 4G" else "")
        else tr("No internet — offline mode: local commands only", "لا يوجد إنترنت — وضع عدم الاتصال: الأوامر المحلية فقط"))
    StatusLine(cfg.hasOpenAiKey, "OpenAI API · " + if (cfg.hasOpenAiKey) tr("key saved (not tested)", "المفتاح محفوظ (لم يُختبر)") else tr("no key", "لا يوجد مفتاح"))
    StatusLine(cfg.hasClaudeKey, "Anthropic API · " + if (cfg.hasClaudeKey) tr("key saved (not tested)", "المفتاح محفوظ (لم يُختبر)") else tr("no key", "لا يوجد مفتاح"))
    Spacer(Modifier.height(6.dp))
    Pill(if (running == "api") tr("Testing…", "جارٍ الاختبار…") else tr("Test connection", "اختبار الاتصال"), p.accent) { run("api") { apiR = diag.api() } }
    ResultLine(apiR)
    Gap()

    // ── Diagnostics
    Header(tr("Diagnostics", "التشخيص"), tr("Each test does the real thing and reports what actually happened.", "كل اختبار ينفّذ العملية فعليًا ويعرض النتيجة الحقيقية."))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Pill(if (running == "mic") tr("Speak now…", "تكلّم الآن…") else tr("Test microphone & recognition", "اختبار الميكروفون والتعرف"), p.accent) {
            run("mic") { val (m, r) = diag.speech(); micR = m; recR = r }
        }
        Pill(tr("Test API", "اختبار API"), p.accent) { run("api") { apiR = diag.api() } }
        Pill(tr("Test speech", "اختبار النطق"), p.accent) { run("tts") { ttsR = diag.tts() } }
    }
    ResultLine(micR)
    ResultLine(recR)
    Gap()
    Pill(if (running == "conv") tr("Running 5 conversations…", "تجري 5 محادثات…") else tr("Test 5 conversations in a row", "اختبار خمس محادثات متتابعة"), p.accent2) {
        run("conv") {
            convR = emptyList(); convDone = null
            convDone = diag.conversation { i, r -> convR = convR + ((if (r.ok) "✓ " else "✗ ") + "$i · " + r.detail) }
        }
    }
    Spacer(Modifier.height(4.dp))
    Text(tr("Uses 5 small API requests with the selected provider.", "يستخدم 5 طلبات API صغيرة مع الوكيل المختار."), style = NovaType.small, color = p.textDim)
    convR.forEach { Text(it, style = NovaType.small, color = if (it.startsWith("✓")) p.ok else p.danger, maxLines = 2, overflow = TextOverflow.Ellipsis) }
    ResultLine(convDone)
}

/** Masked API-key entry; the saved key is never shown again. */
@Composable
private fun KeyField(label: String, saved: Boolean, onSave: (String?) -> Unit) {
    val p = LocalNova.current
    var text by remember { mutableStateOf("") }
    Column {
        OutlinedTextField(
            value = text, onValueChange = { text = it }, singleLine = true, modifier = Modifier.fillMaxWidth(),
            label = { Text(label + if (saved) tr(" — saved ••••", " — محفوظ ••••") else "") },
            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = p.text, unfocusedTextColor = p.text, focusedBorderColor = p.accent, unfocusedBorderColor = p.stroke,
                focusedLabelColor = p.accent, unfocusedLabelColor = p.textDim, cursorColor = p.accent,
            ),
        )
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Pill(tr("Save key", "حفظ المفتاح"), p.accent) { if (text.isNotBlank()) { onSave(text); text = "" } }
            if (saved) Pill(tr("Delete key", "حذف المفتاح"), p.danger) { onSave(null) }
        }
    }
}

@Composable
private fun VoiceSection(c: AppContainer, s: NovaSettings) {
    val p = LocalNova.current
    val ar = LocalArabic.current
    val apps by c.apps.apps.collectAsStateWithLifecycle()
    var phrase by remember { mutableStateOf("") }
    var picking by remember { mutableStateOf(false) }
    Column {
        AskMeSettings(c, s)
        Gap()
        Text(tr("Try:", "جرّب:"), style = NovaType.label, color = p.textDim)
        Spacer(Modifier.height(6.dp))
        androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val ex = if (ar) com.abu00salman.novadrive.data.VoiceCommands.EXAMPLES_AR else com.abu00salman.novadrive.data.VoiceCommands.EXAMPLES_EN
            items(ex.size) { i -> Pill("«${ex[i]}»", p.accent) }
        }
        Gap()
        Header(tr("My commands", "أوامري الخاصة"), tr("When you say the phrase, the chosen app opens.", "عندما تقول العبارة يُفتح التطبيق الذي اخترته."))
        if (s.voiceCommands.isEmpty()) Text(tr("No custom commands yet.", "لا توجد أوامر خاصة بعد."), style = NovaType.label, color = p.textDim)
        s.voiceCommands.forEach { cmd ->
            val label = apps.firstOrNull { it.packageName == cmd.packageName }?.label ?: cmd.packageName
            RowItem("«${cmd.phrase}»", tr("Opens ", "يفتح ") + label) {
                Box(Modifier.size(48.dp).clip(CircleShape).clickable {
                    c.settings.update { it.copy(voiceCommands = it.voiceCommands - cmd) }
                }, contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Close, tr("Remove", "حذف"), tint = p.danger) }
            }
            Gap()
        }
        Gap()
        Field(tr("Phrase, e.g. \"open my music\"", "العبارة، مثل: «شغل أغانيي»"), phrase) { phrase = it }
        Gap()
        Pill(tr("Choose app and save", "اختر التطبيق واحفظ"), p.accent) { if (phrase.isNotBlank()) picking = true }
    }
    if (picking) AppPickerDialog(tr("Open which app?", "أي تطبيق يُفتح؟"), apps, allowNone = false, onPick = { a ->
        if (a != null) c.settings.update {
            it.copy(voiceCommands = it.voiceCommands.filter { x -> x.phrase != phrase.trim() } + com.abu00salman.novadrive.data.CustomCommand(phrase.trim(), a.packageName))
        }
        phrase = ""; picking = false
    }, onDismiss = { picking = false })
}
