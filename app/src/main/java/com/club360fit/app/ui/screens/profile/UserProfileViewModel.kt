package com.club360fit.app.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.club360fit.app.data.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MyProfileUiState(
    val displayName: String = "",
    val email: String = "",
    val roleLabel: String = "Client",
    val isLoading: Boolean = true,
    val error: String? = null
)

class UserProfileViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(MyProfileUiState())
    val uiState: StateFlow<MyProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val user = SupabaseClient.client.auth.currentUserOrNull()
                if (user == null) {
                    _uiState.value = MyProfileUiState(
                        isLoading = false,
                        error = "Not signed in"
                    )
                    return@launch
                }
                val name = (user.userMetadata?.get("name")?.toString()?.takeIf { it != "null" }
                    ?: user.email?.substringBefore("@") ?: "").ifBlank { "Member" }
                val role = user.userMetadata?.get("role")?.toString()?.trim('"') ?: "client"
                val roleLabel = if (role == "admin") "Admin" else "Client"
                _uiState.value = MyProfileUiState(
                    displayName = name,
                    email = user.email.orEmpty(),
                    roleLabel = roleLabel,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load profile"
                )
            }
        }
    }
}
