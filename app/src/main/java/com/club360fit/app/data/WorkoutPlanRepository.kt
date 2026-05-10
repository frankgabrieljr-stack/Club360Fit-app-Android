package com.club360fit.app.data

import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

object WorkoutPlanRepository {
    private val client = SupabaseClient.client

    suspend fun getCurrentPlan(clientId: String): WorkoutPlanDto? = withContext(Dispatchers.IO) {
        client.postgrest["workout_plans"]
            .select {
                filter { eq("client_id", clientId) }
                order("week_start", order = Order.DESCENDING)
                limit(1)
            }
            .decodeList<WorkoutPlanDto>()
            .firstOrNull()
    }

    /** All workout plans for a client, newest week first. */
    suspend fun getAllPlans(clientId: String): List<WorkoutPlanDto> = withContext(Dispatchers.IO) {
        client.postgrest["workout_plans"]
            .select {
                filter { eq("client_id", clientId) }
                order("week_start", order = Order.DESCENDING)
            }
            .decodeList<WorkoutPlanDto>()
    }

    suspend fun getPlanById(id: String): WorkoutPlanDto? = withContext(Dispatchers.IO) {
        client.postgrest["workout_plans"]
            .select {
                filter { eq("id", id) }
            }
            .decodeList<WorkoutPlanDto>()
            .firstOrNull()
    }

    suspend fun upsertPlan(plan: WorkoutPlanDto) = withContext(Dispatchers.IO) {
        val isNew = plan.id == null
        val persistedPlan = if (isNew) {
            plan.copy(id = UUID.randomUUID().toString())
        } else {
            plan
        }
        client.postgrest["workout_plans"].upsert(persistedPlan)
        ClientNotificationRepository.notifyMemberFromCoach(
            clientId = persistedPlan.clientId,
            kind = "workout_plan",
            title = if (isNew) "New workout plan" else "Workout plan updated",
            body = persistedPlan.title,
            refType = "workout_plan",
            refId = persistedPlan.id,
            dedupeKey = if (isNew) {
                "workout_plan_new:${persistedPlan.clientId}:${persistedPlan.weekStart}:${persistedPlan.title}"
            } else {
                null
            }
        )
    }
}
