package com.club360fit.app.ui.utils

/** One line for coach UI: age, height, weight, goal (matches iOS `ClientDTO.memberSummaryLine`). */
fun buildClientMemberSummaryLine(
    age: Int?,
    heightCm: Int?,
    weightKg: Int?,
    goal: String
): String {
    val parts = mutableListOf<String>()
    age?.let { parts.add("Age $it") }
    heightCm?.takeIf { it > 0 }?.let {
        val (ft, inc) = it.toFeetInches()
        parts.add("${ft}' ${inc}\"")
    }
    weightKg?.takeIf { it > 0 }?.let { kg ->
        formatWeightLbsFromKg(kg)?.let { parts.add(it) }
    }
    goal.trim().takeIf { it.isNotEmpty() }?.let { parts.add("Goal: $it") }
    return parts.joinToString(" · ")
}
