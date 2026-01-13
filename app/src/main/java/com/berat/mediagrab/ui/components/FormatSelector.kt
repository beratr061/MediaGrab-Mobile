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

data class FormatOption(val id: String, val label: String, val isVideo: Boolean)

private val formatOptions =
        listOf(
                // Video formats
                FormatOption("video-mp4", "MP4", true),
                FormatOption("video-webm", "WebM", true),
                FormatOption("video-mkv", "MKV", true),
                // Audio formats
                FormatOption("audio-mp3", "MP3", false),
                FormatOption("audio-aac", "AAC/M4A", false),
                FormatOption("audio-opus", "Opus", false),
                FormatOption("audio-flac", "FLAC", false),
                FormatOption("audio-wav", "WAV", false)
        )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormatSelector(
        selectedFormat: String,
        onFormatSelected: (String) -> Unit,
        modifier: Modifier = Modifier
) {
        @Suppress("ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE")
        var expanded by remember { mutableStateOf(false) }
        val selectedOption = formatOptions.find { it.id == selectedFormat } ?: formatOptions.first()

        ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
                modifier = modifier
        ) {
                OutlinedTextField(
                        value = selectedOption.label,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Format", fontSize = 12.sp) },
                        trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        },
                        leadingIcon = {
                                Icon(
                                        if (selectedOption.isVideo) Icons.Default.Videocam
                                        else Icons.Default.MusicNote,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                )
                        },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                )

                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        // Video section
                        Text(
                                "Video",
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.outline
                        )

                        formatOptions.filter { it.isVideo }.forEach { option ->
                                DropdownMenuItem(
                                        text = { Text(option.label) },
                                        onClick = {
                                                onFormatSelected(option.id)
                                                expanded = false
                                        },
                                        leadingIcon = {
                                                Icon(
                                                        Icons.Default.Videocam,
                                                        contentDescription = null
                                                )
                                        }
                                )
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                        // Audio section
                        Text(
                                "Ses",
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.outline
                        )

                        formatOptions.filter { !it.isVideo }.forEach { option ->
                                DropdownMenuItem(
                                        text = { Text(option.label) },
                                        onClick = {
                                                onFormatSelected(option.id)
                                                expanded = false
                                        },
                                        leadingIcon = {
                                                Icon(
                                                        Icons.Default.MusicNote,
                                                        contentDescription = null
                                                )
                                        }
                                )
                        }
                }
        }
}
