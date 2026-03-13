package com.club360fit.app.data

import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
        client.postgrest["workout_plans"].upsert(plan)
    }
}
