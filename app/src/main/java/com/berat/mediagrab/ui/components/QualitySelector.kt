package com.berat.mediagrab.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.berat.mediagrab.data.model.VideoQuality
import com.berat.mediagrab.ui.theme.Primary

private data class QualityOption(
    val label: String,
    val value: Int,
    val isVideo: Boolean
)

@Composable
fun QualitySelector(
    availableQualities: List<VideoQuality>,
    selectedQuality: String,
    onQualitySelected: (String) -> Unit,
    isVideoFormat: Boolean,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    val videoQualities = availableQualities.filter { it.isVideo }.sortedByDescending { it.height ?: 0 }
    val audioQualities = availableQualities.filter { !it.isVideo }.sortedByDescending { it.bitrate ?: 0 }

    val qualityOptions = if (isVideoFormat) {
        if (videoQualities.isNotEmpty()) {
            videoQualities.map { QualityOption(it.label, it.height ?: 0, true) }
        } else {
            listOf(
                QualityOption("1080p HD", 1080, true),
                QualityOption("720p", 720, true),
                QualityOption("480p", 480, true),
                QualityOption("4K Ultra", 2160, true)
            )
        }
    } else {
        if (audioQualities.isNotEmpty()) {
            audioQualities.map { QualityOption(it.label, it.bitrate ?: 0, false) }
        } else {
            listOf(
                QualityOption("En İyi", 0, false),
                QualityOption("192kbps", 192, false),
                QualityOption("128kbps", 128, false)
            )
        }
    }

    val selectedOption = qualityOptions.find { it.label == selectedQuality } ?: qualityOptions.firstOrNull()

    LaunchedEffect(qualityOptions, selectedQuality) {
        if (qualityOptions.isNotEmpty() && qualityOptions.none { it.label == selectedQuality }) {
            qualityOptions.firstOrNull()?.let { onQualitySelected(it.label) }
        }
    }
    
    // Smooth rotation animation for arrow
    val rotationAngle by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "arrow_rotation"
    )

    Box(modifier = modifier) {
        // Trigger Button
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = rememberRipple(color = Primary.copy(alpha = 0.1f))
                ) { expanded = true },
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (expanded) Primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = if (isVideoFormat) Icons.Default.Hd else Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = selectedOption?.label ?: "Kalite Seç",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier
                        .size(20.dp)
                        .rotate(rotationAngle)
                )
            }
        }

        // Dropdown Menu
        if (qualityOptions.isNotEmpty()) {
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                offset = DpOffset(0.dp, 8.dp),
                modifier = Modifier
                    .shadow(
                        elevation = 16.dp,
                        shape = RoundedCornerShape(16.dp),
                        spotColor = Color.Black.copy(alpha = 0.15f)
                    )
                    .background(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clip(RoundedCornerShape(16.dp))
            ) {
                qualityOptions.forEachIndexed { index, quality ->
                    val isSelected = quality.label == selectedQuality
                    
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = quality.label,
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isSelected) Primary else MaterialTheme.colorScheme.onSurface
                            )
                        },
                        onClick = {
                            onQualitySelected(quality.label)
                            expanded = false
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = if (quality.isVideo) Icons.Default.Hd else Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = if (isSelected) Primary else MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        trailingIcon = {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        },
                        modifier = Modifier
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) Primary.copy(alpha = 0.08f) else Color.Transparent,
                                RoundedCornerShape(12.dp)
                            ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp)
                    )
                    
                    // Divider between items (except last)
                    if (index < qualityOptions.size - 1) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}
