package com.club360fit.app.ui.screens.admin

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.club360fit.app.data.MealPhotoLogDto
import com.club360fit.app.data.MealPhotoRepository
import com.club360fit.app.ui.theme.BurgundyPrimary
import com.club360fit.app.ui.utils.toDisplayDate
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val coachFeedbackPresets = listOf(
    "Too much" to "Too much — consider trimming portion or lighter swaps next meal.",
    "Too little" to "Too little — add lean protein or another serving from the plan.",
    "Good balance" to "Good balance — keep this up."
)

@Composable
fun MealPhotoReviewCard(
    clientId: String,
    item: MealPhotoLogDto,
    onSaved: () -> Unit,
    onError: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val logId = item.id
    var draft by remember(item.id) { mutableStateOf(item.coachFeedback.orEmpty()) }
    LaunchedEffect(item.coachFeedback) {
        draft = item.coachFeedback.orEmpty()
    }
    var saving by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(item.logDate.toDisplayDate(), style = MaterialTheme.typography.titleSmall, color = BurgundyPrimary)
        if (!item.notes.isNullOrBlank()) {
            Text("Client: ${item.notes.orEmpty()}", style = MaterialTheme.typography.bodySmall)
        }
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(MealPhotoRepository.publicUrlFor(item.storagePath))
                .crossfade(true)
                .build(),
            contentDescription = "Client meal photo",
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
        )

        Text("Coach feedback", style = MaterialTheme.typography.labelLarge, color = BurgundyPrimary)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            coachFeedbackPresets.forEach { (label, text) ->
                AssistChip(
                    onClick = { draft = text },
                    label = { Text(label) }
                )
            }
        }
        OutlinedTextField(
            value = draft,
            onValueChange = { draft = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Feedback for your client") },
            placeholder = { Text("e.g. protein looks light — add a palm-sized portion.") },
            minLines = 2,
            maxLines = 5,
            enabled = logId != null && !saving
        )
        item.coachFeedbackUpdatedAt?.let { iso ->
            Text(
                formatCoachFeedbackTime(iso),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Button(
            onClick = {
                if (logId == null) return@Button
                scope.launch {
                    saving = true
                    try {
                        MealPhotoRepository.updateCoachFeedback(clientId, logId, draft)
                        onSaved()
                    } catch (e: Exception) {
                        onError(e.message ?: "Could not save feedback")
                    } finally {
                        saving = false
                    }
                }
            },
            enabled = logId != null && !saving,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (saving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(if (saving) "Saving…" else "Save feedback")
            }
        }
    }
}

private fun formatCoachFeedbackTime(iso: String): String =
    try {
        val instant = Instant.parse(iso)
        val z = instant.atZone(ZoneId.systemDefault())
        DateTimeFormatter.ofPattern("MMM dd yyyy · h:mm a").format(z)
    } catch (_: Exception) {
        iso
    }
