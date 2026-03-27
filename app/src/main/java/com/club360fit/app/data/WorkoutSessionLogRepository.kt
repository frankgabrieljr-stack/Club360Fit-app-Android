package com.club360fit.app.data

import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.LocalDate

object WorkoutSessionLogRepository {
    private val client = SupabaseClient.client

    fun weekStartSunday(d: LocalDate): LocalDate = d.with(DayOfWeek.SUNDAY)

    suspend fun logSession(
        clientId: String,
        sessionDate: LocalDate,
        noteToCoach: String? = null
    ) = withContext(Dispatchers.IO) {
        val trimmed = noteToCoach?.trim().orEmpty()
        val note = when {
            trimmed.isEmpty() -> null
            trimmed.length > 1000 -> trimmed.take(1000)
            else -> trimmed
        }
        val row = WorkoutSessionLogDto(
            clientId = clientId,
            sessionDate = sessionDate,
            weekStart = weekStartSunday(sessionDate),
            noteToCoach = note
        )
        try {
            client.postgrest["workout_session_logs"].insert(row)
            val logged = client.postgrest["workout_session_logs"]
                .select {
                    filter {
                        eq("client_id", clientId)
                        eq("session_date", sessionDate.toString())
                    }
                    limit(1)
                }
                .decodeList<WorkoutSessionLogDto>()
                .firstOrNull()
            val day = sessionDate.toString()
            val body = if (note.isNullOrEmpty()) {
                "Member logged a session for $day."
            } else {
                "Member logged a session for $day. Note: ${note.take(500)}"
            }
            ClientNotificationRepository.notifyCoachAboutClient(
                clientId = clientId,
                kind = "workout_session_logged",
                title = "Workout session logged",
                body = body,
                refType = "workout_session",
                refId = logged?.id
            )
        } catch (_: Exception) {
            /* duplicate day */
        }
    }

    /** Coach/admin: reply to member workout note; updates `workout_session_logs` and notifies the member. */
    suspend fun replyToWorkoutNote(
        clientId: String,
        workoutSessionLogId: String?,
        replyText: String
    ) = withContext(Dispatchers.IO) {
        val trimmed = replyText.trim()
        if (trimmed.isEmpty()) return@withContext
        val preview = trimmed.take(1000)
        val now = java.time.Instant.now().toString()
        val logId = workoutSessionLogId?.trim()?.takeIf { it.isNotEmpty() }
        if (logId != null) {
            client.postgrest["workout_session_logs"].update(
                {
                    set("coach_reply", preview)
                    set("coach_replied_at", now)
                }
            ) {
                filter {
                    eq("id", logId)
                    eq("client_id", clientId)
                }
            }
        }
        ClientNotificationRepository.notifyMemberFromCoach(
            clientId = clientId,
            kind = "workout_session_reply",
            title = "Coach reply to your workout note",
            body = preview,
            refType = "workout_session",
            refId = logId
        )
    }

    suspend fun hasSessionOn(clientId: String, date: LocalDate): Boolean = withContext(Dispatchers.IO) {
        client.postgrest["workout_session_logs"]
            .select {
                filter {
                    eq("client_id", clientId)
                    eq("session_date", date.toString())
                }
                limit(1)
            }
            .decodeList<WorkoutSessionLogDto>()
            .isNotEmpty()
    }

    suspend fun countForWeek(clientId: String, weekStart: LocalDate): Int = withContext(Dispatchers.IO) {
        client.postgrest["workout_session_logs"]
            .select {
                filter {
                    eq("client_id", clientId)
                    eq("week_start", weekStart.toString())
                }
            }
            .decodeList<WorkoutSessionLogDto>()
            .size
    }

    suspend fun listForWeek(clientId: String, weekStart: LocalDate): List<WorkoutSessionLogDto> =
        withContext(Dispatchers.IO) {
            client.postgrest["workout_session_logs"]
                .select {
                    filter {
                        eq("client_id", clientId)
                        eq("week_start", weekStart.toString())
                    }
                    order("session_date", order = Order.DESCENDING)
                }
                .decodeList<WorkoutSessionLogDto>()
        }

    /** Fetch recent session logs for a client (coach view). */
    suspend fun fetchForClient(clientId: String, limit: Int = 20): List<WorkoutSessionLogDto> =
        withContext(Dispatchers.IO) {
            client.postgrest["workout_session_logs"]
                .select {
                    filter { eq("client_id", clientId) }
                    order("session_date", order = Order.DESCENDING)
                    limit(limit.toLong())
                }
                .decodeList<WorkoutSessionLogDto>()
        }
}
