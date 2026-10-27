package com.abu00salman.novadrive.core

import androidx.compose.ui.graphics.Color

enum class Body { SEDAN, SUV, HATCH, COUPE, PICKUP }
enum class Lights { WRAP, BAR, VERT }

data class CarModel(val id: String, val name: String, val body: Body, val lights: Lights) {
    /** Arabic model name (alphanumeric model codes stay as they are). */
    val nameAr: String get() = MODEL_AR[id] ?: name
}

/** "Hyundai Sonata 2019" / "هيونداي سوناتا 2019" — one language per line, never mixed. */
fun carDisplayName(brand: CarBrand, model: CarModel, arabic: Boolean): String =
    if (arabic) "${brand.ar} ${model.nameAr}" else "${brand.en} ${model.name}"

private val MODEL_AR = mapOf(
    "sonata19" to "سوناتا 2019",
    "elantra" to "إلنترا",
    "accent" to "أكسنت",
    "azera" to "أزيرا",
    "tucson" to "توسان",
    "santafe" to "سنتافي",
    "palisade" to "باليسيد",
    "camry" to "كامري",
    "corolla" to "كورولا",
    "yaris" to "يارس",
    "lc" to "لاندكروزر",
    "prado" to "برادو",
    "fortuner" to "فورتشنر",
    "hilux" to "هايلكس",
    "k5" to "K5",
    "cerato" to "سيراتو",
    "sportage" to "سبورتاج",
    "sorento" to "سورينتو",
    "telluride" to "تيلورايد",
    "altima" to "ألتيما",
    "sunny" to "صني",
    "xtrail" to "إكس تريل",
    "patrol" to "باترول",
    "navara" to "نافارا",
    "accord" to "أكورد",
    "civic" to "سيفيك",
    "crv" to "CR-V",
    "mustang" to "موستانج",
    "explorer" to "إكسبلورر",
    "expedition" to "إكسبيديشن",
    "f150" to "F-150",
    "malibu" to "ماليبو",
    "camaro" to "كمارو",
    "tahoe" to "تاهو",
    "suburban" to "سوبربان",
    "silverado" to "سلفرادو",
    "yukon" to "يوكن",
    "sierra" to "سييرا",
    "model3" to "موديل 3",
    "models" to "موديل S",
    "modely" to "موديل Y",
    "modelx" to "موديل X",
    "cybertruck" to "سايبرتراك",
    "cclass" to "الفئة C",
    "eclass" to "الفئة E",
    "sclass" to "الفئة S",
    "gle" to "GLE",
    "gclass" to "الفئة G",
    "3series" to "الفئة الثالثة",
    "5series" to "الفئة الخامسة",
    "7series" to "الفئة السابعة",
    "x5" to "X5",
    "x7" to "X7",
    "es" to "ES",
    "is" to "IS",
    "rx" to "RX",
    "lx" to "LX",
    "g70" to "G70",
    "g80" to "G80",
    "gv80" to "GV80",
    "a4" to "A4",
    "a6" to "A6",
    "q7" to "Q7"
)
data class CarBrand(val id: String, val en: String, val ar: String, val models: List<CarModel>)
data class CarPaint(val en: String, val ar: String, val color: Color)

private fun m(id: String, name: String, b: Body, l: Lights = if (b == Body.SUV || b == Body.PICKUP) Lights.VERT else Lights.WRAP) =
    CarModel(id, name, b, l)

val CarPaints = listOf(
    CarPaint("Pearl White", "أبيض لؤلؤي", Color(0xFFF4F5F7)),
    CarPaint("Silver", "فضي", Color(0xFFC3C8D0)),
    CarPaint("Graphite", "رمادي غرافيت", Color(0xFF5B616C)),
    CarPaint("Obsidian Black", "أسود", Color(0xFF14161B)),
    CarPaint("Deep Blue", "أزرق عميق", Color(0xFF1F4FB0)),
    CarPaint("Racing Red", "أحمر", Color(0xFFC21F33)),
    CarPaint("Forest Green", "أخضر", Color(0xFF1E5A42)),
    CarPaint("Desert Sand", "رملي", Color(0xFFC9B38C)),
)

val CarBrands = listOf(
    CarBrand("hyundai", "Hyundai", "هيونداي", listOf(
        m("sonata19", "Sonata 2019", Body.SEDAN), m("elantra", "Elantra", Body.SEDAN), m("accent", "Accent", Body.SEDAN),
        m("azera", "Azera", Body.SEDAN), m("tucson", "Tucson", Body.SUV), m("santafe", "Santa Fe", Body.SUV),
        m("palisade", "Palisade", Body.SUV))),
    CarBrand("toyota", "Toyota", "تويوتا", listOf(
        m("camry", "Camry", Body.SEDAN), m("corolla", "Corolla", Body.SEDAN), m("yaris", "Yaris", Body.HATCH),
        m("lc", "Land Cruiser", Body.SUV), m("prado", "Prado", Body.SUV), m("fortuner", "Fortuner", Body.SUV),
        m("hilux", "Hilux", Body.PICKUP))),
    CarBrand("kia", "Kia", "كيا", listOf(
        m("k5", "K5", Body.SEDAN, Lights.BAR), m("cerato", "Cerato", Body.SEDAN), m("sportage", "Sportage", Body.SUV),
        m("sorento", "Sorento", Body.SUV), m("telluride", "Telluride", Body.SUV))),
    CarBrand("nissan", "Nissan", "نيسان", listOf(
        m("altima", "Altima", Body.SEDAN), m("sunny", "Sunny", Body.SEDAN), m("xtrail", "X-Trail", Body.SUV),
        m("patrol", "Patrol", Body.SUV), m("navara", "Navara", Body.PICKUP))),
    CarBrand("honda", "Honda", "هوندا", listOf(
        m("accord", "Accord", Body.SEDAN), m("civic", "Civic", Body.SEDAN), m("crv", "CR-V", Body.SUV))),
    CarBrand("ford", "Ford", "فورد", listOf(
        m("mustang", "Mustang", Body.COUPE, Lights.VERT), m("explorer", "Explorer", Body.SUV),
        m("expedition", "Expedition", Body.SUV), m("f150", "F-150", Body.PICKUP))),
    CarBrand("chevrolet", "Chevrolet", "شيفروليه", listOf(
        m("malibu", "Malibu", Body.SEDAN), m("camaro", "Camaro", Body.COUPE), m("tahoe", "Tahoe", Body.SUV),
        m("suburban", "Suburban", Body.SUV), m("silverado", "Silverado", Body.PICKUP))),
    CarBrand("gmc", "GMC", "جي إم سي", listOf(
        m("yukon", "Yukon", Body.SUV), m("sierra", "Sierra", Body.PICKUP))),
    CarBrand("tesla", "Tesla", "تسلا", listOf(
        m("model3", "Model 3", Body.SEDAN, Lights.BAR), m("models", "Model S", Body.HATCH, Lights.BAR),
        m("modely", "Model Y", Body.SUV, Lights.BAR), m("modelx", "Model X", Body.SUV, Lights.BAR),
        m("cybertruck", "Cybertruck", Body.PICKUP, Lights.BAR))),
    CarBrand("mercedes", "Mercedes-Benz", "مرسيدس", listOf(
        m("cclass", "C-Class", Body.SEDAN), m("eclass", "E-Class", Body.SEDAN), m("sclass", "S-Class", Body.SEDAN, Lights.BAR),
        m("gle", "GLE", Body.SUV), m("gclass", "G-Class", Body.SUV))),
    CarBrand("bmw", "BMW", "بي إم دبليو", listOf(
        m("3series", "3 Series", Body.SEDAN), m("5series", "5 Series", Body.SEDAN), m("7series", "7 Series", Body.SEDAN, Lights.BAR),
        m("x5", "X5", Body.SUV), m("x7", "X7", Body.SUV))),
    CarBrand("lexus", "Lexus", "لكزس", listOf(
        m("es", "ES", Body.SEDAN, Lights.BAR), m("is", "IS", Body.SEDAN), m("rx", "RX", Body.SUV, Lights.BAR),
        m("lx", "LX", Body.SUV, Lights.BAR))),
    CarBrand("genesis", "Genesis", "جينيسيس", listOf(
        m("g70", "G70", Body.SEDAN), m("g80", "G80", Body.SEDAN), m("gv80", "GV80", Body.SUV))),
    CarBrand("audi", "Audi", "أودي", listOf(
        m("a4", "A4", Body.SEDAN, Lights.BAR), m("a6", "A6", Body.SEDAN, Lights.BAR), m("q7", "Q7", Body.SUV, Lights.BAR))),
)

fun carBrand(id: String) = CarBrands.firstOrNull { it.id == id } ?: CarBrands[0]
fun carModel(brand: String, model: String): CarModel {
    val b = carBrand(brand)
    return b.models.firstOrNull { it.id == model } ?: b.models[0]
}
