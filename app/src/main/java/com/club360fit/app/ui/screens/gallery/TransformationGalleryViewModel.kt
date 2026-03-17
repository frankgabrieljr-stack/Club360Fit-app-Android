package com.club360fit.app.ui.screens.gallery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.club360fit.app.data.SupabaseClient
import com.club360fit.app.data.TransformationGalleryRepository
import com.club360fit.app.data.TransformationImage
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

data class TransformationGalleryUiState(
    val isLoading: Boolean = true,
    val isAdmin: Boolean = false,
    val images: List<TransformationImage> = emptyList(),
    val error: String? = null
)

class TransformationGalleryViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(TransformationGalleryUiState())
    val uiState: StateFlow<TransformationGalleryUiState> = _uiState

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                val (isAdmin, images) = withContext(Dispatchers.IO) {
                    val client = SupabaseClient.client
                    val user = client.auth.currentUserOrNull()
                    val role = user?.userMetadata?.get("role")?.jsonPrimitive?.contentOrNull
                    val admin = role == "admin"
                    val imgs = TransformationGalleryRepository.listImages()
                    admin to imgs
                }
                _uiState.value = TransformationGalleryUiState(
                    isLoading = false,
                    isAdmin = isAdmin,
                    images = images,
                    error = null
                )
            } catch (e: Exception) {
                _uiState.value = TransformationGalleryUiState(
                    isLoading = false,
                    isAdmin = _uiState.value.isAdmin,
                    images = emptyList(),
                    error = e.message ?: "Failed to load gallery"
                )
            }
        }
    }

    fun addImage(bytes: ByteArray, name: String) {
        viewModelScope.launch {
            try {
                val img = TransformationGalleryRepository.uploadImage(bytes, name)
                _uiState.value = _uiState.value.copy(images = _uiState.value.images + img)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message ?: "Upload failed")
            }
        }
    }

    fun deleteImage(image: TransformationImage) {
        viewModelScope.launch {
            try {
                TransformationGalleryRepository.deleteImage(image.path)
                _uiState.value = _uiState.value.copy(images = _uiState.value.images.filterNot { it.path == image.path })
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message ?: "Delete failed")
            }
        }
    }
}

