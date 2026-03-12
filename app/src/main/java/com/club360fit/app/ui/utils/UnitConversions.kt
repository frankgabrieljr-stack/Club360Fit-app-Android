package com.club360fit.app.ui.utils

import kotlin.math.roundToInt

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

fun Int.toPounds(): Int = (this * 2.20462).roundToInt()

fun fromPounds(pounds: Int): Int = (pounds / 2.20462).roundToInt()
