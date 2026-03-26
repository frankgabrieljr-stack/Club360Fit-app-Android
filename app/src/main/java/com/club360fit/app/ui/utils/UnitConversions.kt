package com.club360fit.app.ui.utils

import kotlin.math.roundToInt

/** Postgres stores metric; UI is US customary (lbs, ft/in). */
private const val LBS_PER_KG = 2.2046226218

fun Int.toFeetInches(): Pair<Int, Int> {
    val totalInches = (this / 2.54).roundToInt()
    val feet = totalInches / 12
    val inches = totalInches % 12
    return feet to inches
}

fun fromFeetInches(feet: Int, inches: Int): Int {
    val totalInches = feet * 12 + inches
    return (totalInches * 2.54).roundToInt()
}

/** Stored kg → pounds (exact). */
fun Double.kgToPounds(): Double = this * LBS_PER_KG

/** User-entered pounds → kg for `weight_kg` columns. */
fun Double.poundsToKg(): Double = this / LBS_PER_KG

/** Display line for a check-in / metric stored as kg (`progress_check_ins.weight_kg`). */
fun formatWeightLbsFromKg(weightKg: Double?): String? {
    if (weightKg == null) return null
    return "${weightKg.kgToPounds().roundToInt()} lbs"
}

fun formatWeightLbsFromKg(weightKg: Int?): String? {
    if (weightKg == null) return null
    return formatWeightLbsFromKg(weightKg.toDouble())
}

/** `clients.weight_kg` is whole kg — display as lbs. */
fun Int.kgToPoundsDisplay(): Int = (this.toDouble() * LBS_PER_KG).roundToInt()

/** @deprecated Prefer [kgToPoundsDisplay] — name was ambiguous. */
fun Int.toPounds(): Int = kgToPoundsDisplay()

/** Whole pounds (profile / forms) → whole kg for `clients.weight_kg`. */
fun fromPounds(pounds: Int): Int = (pounds.toDouble() / LBS_PER_KG).roundToInt()
