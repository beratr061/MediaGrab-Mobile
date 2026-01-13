package com.berat.mediagrab.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import com.berat.mediagrab.ui.theme.Primary
import com.berat.mediagrab.util.SupportedPlatform

@Composable
fun UrlInputField(
        value: String,
        onValueChange: (String) -> Unit,
        platform: SupportedPlatform,
        onPaste: () -> Unit,
        onClear: () -> Unit,
        modifier: Modifier = Modifier
) {
    val hasText = value.isNotEmpty()
    val context = LocalContext.current

    Box(
            modifier =
                    modifier.fillMaxWidth()
                            .height(64.dp)
                            .background(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    RoundedCornerShape(16.dp)
                            )
                            .border(
                                    1.dp,
                                    if (hasText) Primary.copy(alpha = 0.3f) else Color.Transparent,
                                    RoundedCornerShape(16.dp)
                            )
                            .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            // Platform Icon
            Box(
                    modifier =
                            Modifier.size(40.dp)
                                    .background(platform.color.copy(alpha = 0.15f), CircleShape),
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
                            imageVector = Icons.Default.Link,
                            contentDescription = platform.displayName,
                            tint = platform.color,
                            modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Text Input
            Box(modifier = Modifier.weight(1f)) {
                if (value.isEmpty()) {
                    Text(
                            "URL'yi buraya yapıştırın...",
                            color = MaterialTheme.colorScheme.outline,
                            fontSize = 15.sp
                    )
                }

                BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        singleLine = true,
                        textStyle =
                                TextStyle(
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 15.sp
                                ),
                        cursorBrush = SolidColor(Primary),
                        modifier = Modifier.fillMaxWidth()
                )
            }

            // Action Buttons
            if (hasText) {
                IconButton(onClick = onClear, modifier = Modifier.size(32.dp)) {
                    Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Temizle",
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(18.dp)
                    )
                }
            } else {
                IconButton(onClick = onPaste, modifier = Modifier.size(32.dp)) {
                    Icon(
                            imageVector = Icons.Default.ContentPaste,
                            contentDescription = "Yapıştır",
                            tint = Primary,
                            modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
