package com.club360fit.app.data

import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.postgrest
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

@Serializable
private data class TransferClientBody(
    @SerialName("client_id") val clientId: String,
    @SerialName("target_coach_user_id") val targetCoachUserId: String
)

@Serializable
private data class TransferClientFnResponse(
    val ok: Boolean? = null,
    val error: String? = null
)

object ClientRepository {
    private val client = SupabaseClient.client
    private val fnJson = Json { ignoreUnknownKeys = true }

    /**
     * Moves [clientId] to another coach (`clients.coach_id`). Requires Edge Function `transfer-client`.
     * Only the current assigned coach may call; target must have `user_metadata.role` = admin.
     */
    suspend fun transferClientToCoach(clientId: String, targetCoachUserId: String) = withContext(Dispatchers.IO) {
        val trimmed = targetCoachUserId.trim().lowercase()
        require(trimmed.isNotEmpty()) { "Enter the other coach’s user ID (UUID)." }
        val body = TransferClientBody(clientId = clientId, targetCoachUserId = trimmed)
        try {
            val response = client.functions.invoke(
                function = "transfer-client",
                body = body,
                headers = Headers.build {
                    append(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                }
            )
            val text = response.bodyAsText()
            val parsed = fnJson.decodeFromString<TransferClientFnResponse>(text)
            if (parsed.ok != true) {
                throw Exception(parsed.error ?: "Transfer failed")
            }
        } catch (e: RestException) {
            throw Exception(e.message ?: "Transfer failed")
        }
    }

    /**
     * Assigns [coach_id] to the signed-in coach when still null (post-signup intake).
     * Safe if already assigned; enables meal/workout plan RLS for coaches.
     */
    suspend fun claimCoachAssignmentIfNeeded(clientId: String) = withContext(Dispatchers.IO) {
        val uid = client.auth.currentUserOrNull()?.id ?: return@withContext
        client.postgrest["clients"].update(
            {
                set("coach_id", uid)
            }
        ) {
            filter {
                eq("id", clientId)
            }
        }
    }

    suspend fun getClients(): List<ClientDto> = withContext(Dispatchers.IO) {
        // RLS handles which rows are visible
        client.auth.currentUserOrNull() ?: return@withContext emptyList()
        client.postgrest["clients"]
            .select()
            .decodeList<ClientDto>()
    }

    suspend fun getClient(id: String): ClientDto = withContext(Dispatchers.IO) {
        claimCoachAssignmentIfNeeded(id)
        client.postgrest["clients"].select {
            filter {
                eq("id", id)
            }
        }.decodeSingle<ClientDto>()
    }

    suspend fun upsertClient(dto: ClientDto) = withContext(Dispatchers.IO) {
        // Upsert the client record. In a real app, you might set the coach_id here.
        client.postgrest["clients"].upsert(dto)
    }

    suspend fun deleteClient(id: String) = withContext(Dispatchers.IO) {
        client.postgrest["clients"].delete {
            filter { eq("id", id) }
        }
    }

    /**
     * Updates the metadata for the CURRENTLY logged-in user.
     * Note: Changing other users' roles requires service_role or an Edge Function.
     */
    suspend fun updateSelfRole(isAdmin: Boolean) = withContext(Dispatchers.IO) {
        client.auth.updateUser {
            data = buildJsonObject {
                put("role", JsonPrimitive(if (isAdmin) "admin" else "client"))
            }
        }
    }
}
