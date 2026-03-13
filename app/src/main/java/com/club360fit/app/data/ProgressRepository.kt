package com.club360fit.app.data

import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ProgressRepository {
    private val client = SupabaseClient.client

    /** All check-ins for a client (e.g. admin viewing). Newest first. */
    suspend fun getForClient(clientId: String): List<ProgressCheckInDto> = withContext(Dispatchers.IO) {
        client.postgrest["progress_check_ins"]
            .select {
                filter { eq("client_id", clientId) }
                order("check_in_date", order = Order.DESCENDING)
            }
            .decodeList<ProgressCheckInDto>()
    }

    /** Client's own check-ins (client_id resolved via clients table / RLS). */
    suspend fun getOwnCheckIns(clientId: String): List<ProgressCheckInDto> = withContext(Dispatchers.IO) {
        client.postgrest["progress_check_ins"]
            .select {
                filter { eq("client_id", clientId) }
                order("check_in_date", order = Order.DESCENDING)
            }
            .decodeList<ProgressCheckInDto>()
    }

    suspend fun addCheckIn(checkIn: ProgressCheckInDto) = withContext(Dispatchers.IO) {
        client.postgrest["progress_check_ins"].insert(checkIn)
    }

    suspend fun updateCheckIn(checkIn: ProgressCheckInDto) = withContext(Dispatchers.IO) {
        client.postgrest["progress_check_ins"].upsert(checkIn)
    }
}
