package com.club360fit.app.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.club360fit.app.data.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Auth form state. Used for both sign-in and create-account; optional fields only for create-account.
 */
data class AuthUiState(
    val name: String = "",
    val age: String = "",
    val height: String = "",
    val weight: String = "",
    val phone: String = "",
    val email: String = "",
    val password: String = "",
    val medicalConditions: String = "",
    val foodRestrictions: String = "",
    val mealsPerDay: String = "",
    val workoutFrequency: String = "",
    val overallGoal: String = "",
    val isAdmin: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class AuthViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun updateName(value: String) = _uiState.update { it.copy(name = value) }
    fun updateAge(value: String) = _uiState.update { it.copy(age = value) }
    fun updateHeight(value: String) = _uiState.update { it.copy(height = value) }
    fun updateWeight(value: String) = _uiState.update { it.copy(weight = value) }
    fun updatePhone(value: String) = _uiState.update { it.copy(phone = value) }
    fun updateEmail(value: String) = _uiState.update { it.copy(email = value) }
    fun updatePassword(value: String) = _uiState.update { it.copy(password = value) }
    fun updateMedicalConditions(value: String) = _uiState.update { it.copy(medicalConditions = value) }
    fun updateFoodRestrictions(value: String) = _uiState.update { it.copy(foodRestrictions = value) }
    fun updateMealsPerDay(value: String) = _uiState.update { it.copy(mealsPerDay = value) }
    fun updateWorkoutFrequency(value: String) = _uiState.update { it.copy(workoutFrequency = value) }
    fun updateOverallGoal(value: String) = _uiState.update { it.copy(overallGoal = value) }
    fun updateIsAdmin(value: Boolean) = _uiState.update { it.copy(isAdmin = value) }
    fun clearError() = _uiState.update { it.copy(errorMessage = null) }

    fun submit(isSignIn: Boolean, onSuccess: (Boolean) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val state = _uiState.value
            if (state.email.isBlank() || state.password.isBlank()) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "Email and password are required")
                }
                return@launch
            }
            withContext(Dispatchers.IO) {
                try {
                    val supabase = SupabaseClient.client
                    if (isSignIn) {
                        supabase.auth.signInWith(Email) {
                            email = state.email.trim()
                            password = state.password
                        }
                    } else {
                        supabase.auth.signUpWith(Email) {
                            email = state.email.trim()
                            password = state.password
                            data = buildMap {
                                put("name", state.name)
                                put("age", state.age)
                                put("height", state.height)
                                put("weight", state.weight)
                                put("phone", state.phone)
                                put("medical_conditions", state.medicalConditions)
                                put("food_restrictions", state.foodRestrictions)
                                put("meals_per_day", state.mealsPerDay)
                                put("workout_frequency", state.workoutFrequency)
                                put("overall_goal", state.overallGoal)
                                put("role", if (state.isAdmin) "admin" else "client")
                            }
                        }
                    }
                    val user = supabase.auth.currentUserOrNull()
                    val role = user?.userMetadata?.get("role") as? String
                    val isAdmin = role == "admin"
                    _uiState.update { it.copy(isLoading = false) }
                    onSuccess(isAdmin)
                } catch (e: Exception) {
                    val message = e.message ?: "Authentication failed"
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = message)
                    }
                }
            }
        }
    }
}
