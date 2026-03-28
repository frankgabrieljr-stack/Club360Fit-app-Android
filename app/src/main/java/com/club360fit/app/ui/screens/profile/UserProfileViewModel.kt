package com.club360fit.app.ui.screens.profile

import android.graphics.Bitmap
import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.club360fit.app.data.SupabaseClient
import com.club360fit.app.ui.utils.SubmitResultMessages
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.TimeZone
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class UserProfileViewModel : ViewModel() {

    private val _state = MutableStateFlow(UserProfileUiState())
    val uiState: StateFlow<UserProfileUiState> = _state.asStateFlow()

    private val supabase = SupabaseClient.client

    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)
            try {
                val user = withContext(Dispatchers.IO) {
                    supabase.auth.currentUserOrNull()
                } ?: run {
                    _state.value = UserProfileUiState(
                        isLoading = false,
                        errorMessage = "Not signed in"
                    )
                    return@launch
                }
                _state.value = buildStateFromUser(user).copy(isLoading = false)
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Failed to load profile"
                )
            }
        }
    }

    private fun buildStateFromUser(user: io.github.jan.supabase.gotrue.user.UserInfo): UserProfileUiState {
        val meta = user.userMetadata

        // Resolve first/last name from metadata or split "name"
        fun metaString(key: String): String? =
            meta?.get(key)?.jsonPrimitive?.content?.takeIf { it != "null" }?.trim('"')

        val nameStr = metaString("name")
        val firstName = metaString("first_name")
            ?: nameStr?.substringBefore(" ")?.takeIf { it.isNotBlank() }
            ?: ""
        val lastName = metaString("last_name")
            ?: nameStr?.substringAfter(" ", "")?.takeIf { it.isNotBlank() }
            ?: ""

        // Avatar URL — blank strings in metadata behave like “no photo” (fixes empty avatar_url blocking uploads until “Remove”).
        val rawAvatar = metaString("avatar_url") ?: metaString("picture")
        val avatarUrl = rawAvatar?.takeIf { it.isNotBlank() }

        // Role
        val role = metaString("role") ?: "client"
        val roleLabel = if (role == "admin") "Admin" else "Client"

        // Last login formatted (lastSignInAt is Instant in gotrue-kt)
        val lastLoginFormatted = try {
            val lastSignIn = user.lastSignInAt
            if (lastSignIn != null) {
                val javaInstant = java.time.Instant.ofEpochMilli(lastSignIn.toEpochMilliseconds())
                val zoned = javaInstant.atZone(ZoneId.systemDefault())
                zoned.format(DateTimeFormatter.ofPattern("MMM dd yyyy 'at' h:mm a"))
            } else null
        } catch (_: Exception) { null }

        return UserProfileUiState(
            firstName = firstName,
            lastName = lastName,
            bio = metaString("bio") ?: "",
            location = metaString("location") ?: "",
            timezone = metaString("timezone") ?: TimeZone.currentSystemDefault().id,
            phone = metaString("phone") ?: "",
            coachHeadline = metaString("coach_headline") ?: "",
            coachSpecialties = metaString("coach_specialties") ?: "",
            coachAvailability = metaString("coach_availability") ?: "",
            email = user.email ?: "",
            lastLoginFormatted = lastLoginFormatted,
            avatarUrl = avatarUrl,
            roleLabel = roleLabel
        )
    }

    fun uploadAvatar(context: Context, uri: Uri) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isUploadingAvatar = true, errorMessage = null)
            try {
                val user = supabase.auth.currentUserOrNull()
                    ?: throw IllegalStateException("Not signed in")

                val bytes = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                } ?: throw IllegalStateException("Could not read image")

                // Match Supabase auth.uid() casing (Kotlin UUID string may differ from Postgres text).
                val path = "${user.id.lowercase()}/avatar.jpg"
                val bucket = supabase.storage.from(SupabaseClient.AVATARS_BUCKET)

                withContext(Dispatchers.IO) {
                    bucket.upload(path, bytes, upsert = true)
                }

                val publicUrl = bucket.publicUrl(path)

                mergeUserMetadata(mapOf("avatar_url" to JsonPrimitive(publicUrl)))

                loadProfile()
                _state.value = _state.value.copy(
                    isUploadingAvatar = false,
                    uploadSuccessMessage = SubmitResultMessages.UPLOAD_SUCCESS
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isUploadingAvatar = false,
                    errorMessage = e.message ?: "Upload failed"
                )
            }
        }
    }

    fun uploadAvatarBitmap(bitmap: Bitmap) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isUploadingAvatar = true, errorMessage = null)
            try {
                val user = supabase.auth.currentUserOrNull()
                    ?: throw IllegalStateException("Not signed in")

                val bytes = withContext(Dispatchers.IO) {
                    java.io.ByteArrayOutputStream().use { out ->
                        check(bitmap.compress(Bitmap.CompressFormat.JPEG, 88, out)) { "Could not encode image" }
                        out.toByteArray()
                    }
                }

                val path = "${user.id.lowercase()}/avatar.jpg"
                val bucket = supabase.storage.from(SupabaseClient.AVATARS_BUCKET)

                withContext(Dispatchers.IO) {
                    bucket.upload(path, bytes, upsert = true)
                }

                val publicUrl = bucket.publicUrl(path)

                mergeUserMetadata(mapOf("avatar_url" to JsonPrimitive(publicUrl)))

                loadProfile()
                _state.value = _state.value.copy(
                    isUploadingAvatar = false,
                    uploadSuccessMessage = SubmitResultMessages.UPLOAD_SUCCESS
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isUploadingAvatar = false,
                    errorMessage = e.message ?: "Upload failed"
                )
            }
        }
    }

    fun removeAvatar() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isUploadingAvatar = true, errorMessage = null)
            try {
                mergeUserMetadata(
                    mapOf(
                        "avatar_url" to JsonPrimitive(""),
                        "picture" to JsonPrimitive("")
                    )
                )
                loadProfile()
                _state.value = _state.value.copy(
                    isUploadingAvatar = false,
                    uploadSuccessMessage = SubmitResultMessages.SAVED_SUCCESS
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isUploadingAvatar = false,
                    errorMessage = e.message ?: "Could not remove photo"
                )
            }
        }
    }

    fun saveProfileDetails(
        firstName: String,
        lastName: String,
        bio: String,
        location: String,
        timezone: String,
        phone: String,
        coachHeadline: String? = null,
        coachSpecialties: String? = null,
        coachAvailability: String? = null
    ) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isSavingProfile = true, errorMessage = null)
            val mergedName = "${firstName.trim()} ${lastName.trim()}".trim()
            try {
                val updates = mutableMapOf<String, JsonElement>(
                    "first_name" to JsonPrimitive(firstName.trim()),
                    "last_name" to JsonPrimitive(lastName.trim()),
                    "name" to JsonPrimitive(mergedName),
                    "bio" to JsonPrimitive(bio.trim()),
                    "location" to JsonPrimitive(location.trim()),
                    "timezone" to JsonPrimitive(timezone.trim()),
                    "phone" to JsonPrimitive(phone.trim())
                )
                coachHeadline?.let { updates["coach_headline"] = JsonPrimitive(it.trim()) }
                coachSpecialties?.let { updates["coach_specialties"] = JsonPrimitive(it.trim()) }
                coachAvailability?.let { updates["coach_availability"] = JsonPrimitive(it.trim()) }
                mergeUserMetadata(
                    updates
                )
                loadProfile()
                _state.value = _state.value.copy(
                    isSavingProfile = false,
                    profileSavedMessage = SubmitResultMessages.SAVED_SUCCESS
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isSavingProfile = false,
                    errorMessage = e.message ?: "Could not save profile"
                )
            }
        }
    }

    fun clearUploadSuccessMessage() {
        _state.value = _state.value.copy(uploadSuccessMessage = null)
    }

    fun clearProfileSavedMessage() {
        _state.value = _state.value.copy(profileSavedMessage = null)
    }

    private suspend fun mergeUserMetadata(updates: Map<String, JsonElement>) = withContext(Dispatchers.IO) {
        val user = supabase.auth.currentUserOrNull() ?: throw IllegalStateException("Not signed in")
        val existing = user.userMetadata?.jsonObject ?: buildJsonObject {}
        val merged = buildJsonObject {
            existing.forEach { (k, v) -> put(k, v) }
            updates.forEach { (k, v) -> put(k, v) }
        }
        supabase.auth.updateUser { data = merged }
    }
}
