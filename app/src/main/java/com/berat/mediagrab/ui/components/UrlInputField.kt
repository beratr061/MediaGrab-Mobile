package com.berat.mediagrab.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
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
    var isFocused by remember { mutableStateOf(false) }
    val hasText = value.isNotEmpty()
    val context = LocalContext.current
    
    // Platform icon painter (only when platform is detected)
    val platformIconPainter = if (platform != SupportedPlatform.UNKNOWN && platform.iconAsset.isNotEmpty()) {
        rememberAsyncImagePainter(
            model = ImageRequest.Builder(context)
                .data("file:///android_asset/icons/${platform.iconAsset}")
                .decoderFactory(SvgDecoder.Factory())
                .build()
        )
    } else null

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shadowElevation = if (isFocused) 2.dp else 0.dp,
        border = if (isFocused) {
            androidx.compose.foundation.BorderStroke(2.dp, Primary)
        } else {
            androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant
            )
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Platform Icon or Link Icon
            if (platformIconPainter != null) {
                Image(
                    painter = platformIconPainter,
                    contentDescription = platform.displayName,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Link,
                    contentDescription = null,
                    tint = if (isFocused) Primary else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Text Input
            Box(modifier = Modifier.weight(1f)) {
                if (value.isEmpty()) {
                    Text(
                        text = "Video linkini buraya yapıştır",
                        color = MaterialTheme.colorScheme.outline,
                        fontSize = 15.sp
                    )
                }

                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = TextStyle(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 15.sp
                    ),
                    cursorBrush = SolidColor(Primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { isFocused = it.isFocused }
                )
            }

            // Action Button - Paste or Clear
            IconButton(
                onClick = if (hasText) onClear else onPaste,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = if (hasText) Icons.Default.Clear else Icons.Default.ContentPaste,
                    contentDescription = if (hasText) "Temizle" else "Yapıştır",
                    tint = if (hasText) MaterialTheme.colorScheme.outline else Primary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
