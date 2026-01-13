package com.berat.mediagrab.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import com.berat.mediagrab.util.PlatformDetector
import com.berat.mediagrab.util.SupportedPlatform

@Composable
fun PlatformGrid(modifier: Modifier = Modifier) {
    val platforms = PlatformDetector.getAllPlatforms()

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
                "Desteklenen Platformlar",
                fontSize = 12.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.outline,
                letterSpacing = 0.5.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors =
                        CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
        ) {
            // Grid of platforms - 5 columns for compact display
            Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                platforms.chunked(5).forEach { rowPlatforms ->
                    Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        rowPlatforms.forEach { platform -> PlatformItem(platform) }
                        // Fill empty spaces if row is not complete
                        repeat(5 - rowPlatforms.size) { Spacer(modifier = Modifier.width(56.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlatformItem(platform: SupportedPlatform) {
    val context = LocalContext.current

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(56.dp)) {
        Box(
                modifier =
                        Modifier.size(40.dp)
                                .background(platform.color.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
        ) {
            if (platform.iconAsset.isNotEmpty()) {
                val painter =
                        rememberAsyncImagePainter(
                                model =
                                        ImageRequest.Builder(context)
                                                .data(
                                                        "file:///android_asset/icons/${platform.iconAsset}"
                                                )
                                                .decoderFactory(SvgDecoder.Factory())
                                                .build()
                        )
                Image(
                        painter = painter,
                        contentDescription = platform.displayName,
                        modifier = Modifier.size(22.dp)
                )
            } else {
                Icon(
                        Icons.Default.Link,
                        contentDescription = platform.displayName,
                        tint = platform.color,
                        modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
                platform.displayName,
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
        )
    }
}
