package com.club360fit.app.ui.utils

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val displayFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("MMM dd yyyy", Locale.getDefault())

fun LocalDate.toDisplayDate(): String = this.format(displayFormatter)

