package com.club360fit.app.data

import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
private data class DevicePushPayload(
    @SerialName("client_id") val clientId: String,
    val kind: String,
    val title: String,
    val body: String,
    @SerialName("ref_type") val refType: String? = null,
    @SerialName("ref_id") val refId: String? = null,
    @SerialName("dedupe_key") val dedupeKey: String? = null,
    @SerialName("visible_to_client") val visibleToClient: Boolean
)

object ClientNotificationRepository {
    private val client = SupabaseClient.client

    suspend fun listRecent(clientId: String, limit: Int = 40): List<ClientNotificationDto> =
        withContext(Dispatchers.IO) {
            client.postgrest["client_notifications"]
                .select {
                    filter { eq("client_id", clientId) }
                    order("created_at", order = Order.DESCENDING)
                    limit(limit.toLong())
                }
                .decodeList<ClientNotificationDto>()
                .filter { it.clientDeletedAt == null }
        }

    suspend fun unreadCount(clientId: String): Int = withContext(Dispatchers.IO) {
        listRecent(clientId, 100).count { it.readAt == null }
    }

    /** All notifications visible to the signed-in coach (newest first). Matches iOS `fetchNotificationsForCoach`. */
    suspend fun listForCoach(limit: Int = 80): List<ClientNotificationDto> =
        withContext(Dispatchers.IO) {
            client.postgrest["client_notifications"]
                .select {
                    order("created_at", order = Order.DESCENDING)
                    limit(limit.toLong())
                }
                .decodeList<ClientNotificationDto>()
                .filter { it.coachDeletedAt == null }
        }

    /**
     * Coach hub feed scoped to [clientIds] (typically `ClientRepository.getClients()` ids).
     * Defense-in-depth on top of RLS so each coach only sees their roster’s notifications.
     */
    suspend fun listForCoach(clientIds: Set<String>, limit: Int = 80): List<ClientNotificationDto> =
        withContext(Dispatchers.IO) {
            if (clientIds.isEmpty()) return@withContext emptyList()
            listForCoach((limit * 4).coerceAtMost(500).coerceAtLeast(limit))
                .filter { it.clientId in clientIds }
                .take(limit)
        }

    suspend fun coachUnreadCount(): Int = withContext(Dispatchers.IO) {
        listForCoach(400).count { it.coachReadAt == null }
    }

    /** Unread coach notifications for [clientIds] only (matches [listForCoach] scoping). */
    suspend fun coachUnreadCount(clientIds: Set<String>): Int = withContext(Dispatchers.IO) {
        if (clientIds.isEmpty()) return@withContext 0
        listForCoach(clientIds, 400).count { it.coachReadAt == null }
    }

    suspend fun markCoachReadAsCoach(notificationId: String) = withContext(Dispatchers.IO) {
        val now = Instant.now().toString()
        client.postgrest["client_notifications"].update(
            { set("coach_read_at", now) }
        ) {
            filter { eq("id", notificationId) }
        }
    }

    suspend fun markRead(notificationId: String) = withContext(Dispatchers.IO) {
        val now = java.time.Instant.now().toString()
        client.postgrest["client_notifications"].update(
            {
                set("read_at", now)
            }
        ) {
            filter { eq("id", notificationId) }
        }
    }

    suspend fun markAllRead(clientId: String) = withContext(Dispatchers.IO) {
        val now = java.time.Instant.now().toString()
        client.postgrest["client_notifications"].update(
            { set("read_at", now) }
        ) {
            filter {
                eq("client_id", clientId)
            }
        }
    }

    /** Hides a notification from the signed-in member only. */
    suspend fun deleteForMember(notificationId: String) = withContext(Dispatchers.IO) {
        val now = Instant.now().toString()
        client.postgrest["client_notifications"].update(
            { set("client_deleted_at", now) }
        ) {
            filter { eq("id", notificationId) }
        }
    }

    /** Hides a notification from coach inboxes only; the member copy remains visible. */
    suspend fun deleteForCoach(notificationId: String) = withContext(Dispatchers.IO) {
        val now = Instant.now().toString()
        client.postgrest["client_notifications"].update(
            { set("coach_deleted_at", now) }
        ) {
            filter { eq("id", notificationId) }
        }
    }

    /** Inserts; ignores duplicate dedupe_key (unique violation). */
    suspend fun tryInsert(row: ClientNotificationDto) = withContext(Dispatchers.IO) {
        try {
            client.postgrest["client_notifications"].insert(row)
            triggerDevicePush(row)
        } catch (_: Exception) {
            /* duplicate dedupe or network */
        }
    }

    private suspend fun triggerDevicePush(row: ClientNotificationDto) {
        runCatching {
            client.functions.invoke(
                function = "send-device-push",
                body = DevicePushPayload(
                    clientId = row.clientId,
                    kind = row.kind,
                    title = row.title,
                    body = row.body,
                    refType = row.refType,
                    refId = row.refId,
                    dedupeKey = row.dedupeKey,
                    visibleToClient = row.visibleToClient ?: true
                ),
                headers = Headers.build {
                    append(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                }
            )
        }
    }

    /** Coach-facing alert: hidden from member `Updates` (`visible_to_client` = false). Aligns with iOS `notifyCoachAboutClient`. */
    suspend fun notifyCoachAboutClient(
        clientId: String,
        kind: String,
        title: String,
        body: String,
        refType: String? = null,
        refId: String? = null,
        dedupeKey: String? = null
    ) {
        tryInsert(
            ClientNotificationDto(
                clientId = clientId,
                kind = kind,
                title = title,
                body = body,
                refType = refType,
                refId = refId,
                dedupeKey = dedupeKey,
                visibleToClient = false
            )
        )
    }

    /** Member-visible notification (`visible_to_client` = true). Aligns with iOS `notifyMemberFromCoach`. */
    suspend fun notifyMemberFromCoach(
        clientId: String,
        kind: String,
        title: String,
        body: String,
        refType: String? = null,
        refId: String? = null,
        dedupeKey: String? = null
    ) {
        tryInsert(
            ClientNotificationDto(
                clientId = clientId,
                kind = kind,
                title = title,
                body = body,
                refType = refType,
                refId = refId,
                dedupeKey = dedupeKey,
                visibleToClient = true
            )
        )
    }
}
