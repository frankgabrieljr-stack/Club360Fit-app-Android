package com.club360fit.app.data

import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

object ClientRepository {
    private val client = SupabaseClient.client

    suspend fun getClients(): List<ClientDto> = withContext(Dispatchers.IO) {
        client.postgrest["clients"].select().decodeList<ClientDto>()
    }

    suspend fun getClient(id: String): ClientDto = withContext(Dispatchers.IO) {
        client.postgrest["clients"].select {
            filter {
                eq("id", id)
            }
        }.decodeSingle<ClientDto>()
    }

    suspend fun upsertClient(dto: ClientDto) = withContext(Dispatchers.IO) {
        client.postgrest["clients"].upsert(dto)
    }

    suspend fun updateUserRole(userId: String, isAdmin: Boolean) = withContext(Dispatchers.IO) {
        // Note: Updating auth.users metadata usually requires a service_role key or a specific edge function
        // For this MVP, we'll assume the user is updating their own metadata or we're using a workaround
        client.auth.updateUser {
            data = buildJsonObject {
                put("role", JsonPrimitive(if (isAdmin) "admin" else "client"))
            }
        }
    }
}
