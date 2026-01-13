package com.berat.mediagrab.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.berat.mediagrab.ui.theme.*

data class PlatformItem(
    val name: String,
    val icon: ImageVector,
    val color: Color
)

private val platforms = listOf(
    PlatformItem("YouTube", Icons.Default.PlayCircle, YouTubeColor),
    PlatformItem("Instagram", Icons.Default.CameraAlt, InstagramColor),
    PlatformItem("TikTok", Icons.Default.MusicNote, TikTokColor),
    PlatformItem("Twitter", Icons.Default.Close, Color(0xFF0F172A))
)

@Composable
fun PlatformGrid(modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = "Supported Platforms",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.outline,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(start = 4.dp, bottom = 16.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            platforms.forEach { platform ->
                PlatformIcon(
                    icon = platform.icon,
                    label = platform.name,
                    color = platform.color
                )
            }
        }
    }
}

@Composable
private fun PlatformIcon(
    icon: ImageVector,
    label: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            modifier = Modifier.size(64.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = color,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.outline
        )
    }
}
