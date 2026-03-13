package com.club360fit.app.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.LocalDate

@Serializable
data class ProgressCheckInDto(
    val id: String? = null,
    @SerialName("client_id") val clientId: String,
    @Serializable(with = LocalDateSerializer::class)
    @SerialName("check_in_date") val checkInDate: LocalDate,
    @SerialName("weight_kg") val weightKg: Double? = null,
    val notes: String = "",
    @SerialName("workout_done") val workoutDone: Boolean = false,
    @SerialName("meals_followed") val mealsFollowed: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null
)
