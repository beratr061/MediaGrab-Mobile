package com.berat.mediagrab.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.berat.mediagrab.data.model.VideoQuality

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QualitySelector(
        availableQualities: List<VideoQuality>,
        selectedQuality: String,
        onQualitySelected: (String) -> Unit,
        isVideoFormat: Boolean,
        modifier: Modifier = Modifier
) {
    @Suppress("ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE")
    var expanded by remember { mutableStateOf(false) }

    // Get video or audio qualities from availableQualities
    val videoQualities =
            availableQualities.filter { it.isVideo }.sortedByDescending { it.height ?: 0 }
    val audioQualities =
            availableQualities.filter { !it.isVideo }.sortedByDescending { it.bitrate ?: 0 }

    // Generate quality options - use actual labels from availableQualities (which include fps)
    val qualityOptions =
            if (isVideoFormat) {
                if (videoQualities.isNotEmpty()) {
                    videoQualities.map { quality ->
                        QualityOption(quality.label, quality.height ?: 0, true)
                    }
                } else {
                    // Fallback if no qualities parsed
                    listOf(
                            QualityOption("En İyi", 0, true),
                            QualityOption("720p", 720, true),
                            QualityOption("480p", 480, true),
                            QualityOption("360p", 360, true)
                    )
                }
            } else {
                if (audioQualities.isNotEmpty()) {
                    audioQualities.map { quality ->
                        QualityOption(quality.label, quality.bitrate ?: 0, false)
                    }
                } else {
                    // Fallback if no qualities parsed
                    listOf(
                            QualityOption("En İyi", 0, false),
                            QualityOption("192kbps", 192, false),
                            QualityOption("128kbps", 128, false)
                    )
                }
            }

    // Find selected or default to first (highest)
    val selectedOption =
            qualityOptions.find { it.label == selectedQuality } ?: qualityOptions.firstOrNull()

    // Auto-select first quality when list changes
    LaunchedEffect(qualityOptions, selectedQuality) {
        if (qualityOptions.isNotEmpty() &&
                        qualityOptions.find { it.label == selectedQuality } == null
        ) {
            qualityOptions.firstOrNull()?.let { first -> onQualitySelected(first.label) }
        }
    }

    ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = modifier
    ) {
        OutlinedTextField(
                value = selectedOption?.label ?: "Kalite Seç",
                onValueChange = {},
                readOnly = true,
                enabled = qualityOptions.isNotEmpty(),
                label = { Text("Kalite", fontSize = 12.sp) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                leadingIcon = {
                    Icon(
                            if (isVideoFormat) Icons.Default.HighQuality
                            else Icons.Default.MusicNote,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                    )
                },
                modifier = Modifier.menuAnchor().fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
        )

        if (qualityOptions.isNotEmpty()) {
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                qualityOptions.forEach { quality ->
                    DropdownMenuItem(
                            text = { Text(quality.label) },
                            onClick = {
                                onQualitySelected(quality.label)
                                expanded = false
                            },
                            leadingIcon = {
                                Icon(
                                        if (quality.isVideo) Icons.Default.Hd
                                        else Icons.Default.MusicNote,
                                        contentDescription = null
                                )
                            }
                    )
                }
            }
        }
    }
}

private data class QualityOption(
        val label: String,
        val value: Int, // height for video, bitrate for audio
        val isVideo: Boolean
)
