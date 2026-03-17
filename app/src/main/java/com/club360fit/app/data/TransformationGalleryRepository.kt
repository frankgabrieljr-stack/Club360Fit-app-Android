package com.club360fit.app.data

import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class TransformationImage(
    val path: String,
    val url: String
)

object TransformationGalleryRepository {

    private val client = SupabaseClient.client

    private val bucket
        get() = client.storage.from(SupabaseClient.TRANSFORMATIONS_BUCKET)

    suspend fun listImages(): List<TransformationImage> = withContext(Dispatchers.IO) {
        bucket.list().map { file ->
            TransformationImage(
                path = file.name,
                url = bucket.publicUrl(file.name)
            )
        }.sortedBy { it.path }
    }

    suspend fun uploadImage(bytes: ByteArray, originalName: String): TransformationImage = withContext(Dispatchers.IO) {
        val uid = client.auth.currentUserOrNull()?.id ?: "anon"
        val safeName = originalName.replace("\\s+".toRegex(), "_")
        val path = "$uid/${System.currentTimeMillis()}_$safeName"
        bucket.upload(path, bytes, upsert = false)
        TransformationImage(
            path = path,
            url = bucket.publicUrl(path)
        )
    }

    suspend fun deleteImage(path: String) = withContext(Dispatchers.IO) {
        bucket.delete(path)
    }
}

