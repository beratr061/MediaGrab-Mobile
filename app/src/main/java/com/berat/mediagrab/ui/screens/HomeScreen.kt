package com.berat.mediagrab.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.ui.res.painterResource
import com.berat.mediagrab.R
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import com.berat.mediagrab.data.database.DownloadEntity
import com.berat.mediagrab.ui.components.*
import com.berat.mediagrab.ui.theme.*
import com.berat.mediagrab.ui.viewmodel.HomeViewModel
import com.berat.mediagrab.util.PlatformDetector

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToDownloads: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val recentDownloads by viewModel.recentDownloads.collectAsState()
    val clipboardManager = LocalClipboardManager.current
    val platform = remember(uiState.url) { PlatformDetector.detect(uiState.url) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 448.dp)
                .align(Alignment.TopCenter)
        ) {
            // Header
            HomeHeader(
                onHistoryClick = onNavigateToDownloads,
                onSettingsClick = onNavigateToSettings
            )

            // Main Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(32.dp)
            ) {
                // Input Section
                InputSection(
                    url = uiState.url,
                    onUrlChange = { viewModel.updateUrl(it) },
                    selectedFormat = uiState.selectedFormat,
                    onFormatChange = { viewModel.updateFormat(it) },
                    selectedQuality = uiState.selectedQuality,
                    availableQualities = uiState.mediaInfo?.availableQualities ?: emptyList(),
                    onQualityChange = { viewModel.updateQuality(it) },
                    isLoading = uiState.isLoading,
                    isDownloading = uiState.isDownloading,
                    onDownloadClick = { viewModel.startDownload() },
                    onPaste = {
                        clipboardManager.getText()?.text?.let { viewModel.updateUrl(it) }
                    },
                    onClear = { viewModel.updateUrl("") },
                    platform = platform
                )

                // Download Progress
                if (uiState.isDownloading) {
                    DownloadProgressCard(
                        progress = uiState.downloadProgress,
                        statusMessage = uiState.statusMessage,
                        downloadSpeed = uiState.downloadSpeed
                    )
                }

                // Media Info Preview
                uiState.mediaInfo?.let { info ->
                    MediaInfoCard(info = info)
                }

                // Supported Platforms
                SupportedPlatformsSection()

                // Recent Downloads
                RecentDownloadsSection(
                    downloads = recentDownloads,
                    onClearAll = { viewModel.clearAllDownloads() },
                    onItemClick = { /* Open file */ },
                    onItemDelete = { viewModel.deleteDownload(it) }
                )
            }
        }
    }
}

@Composable
private fun HomeHeader(
    onHistoryClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.95f),
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Logo and Title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Logo Icon
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .shadow(8.dp, RoundedCornerShape(12.dp), spotColor = Primary.copy(alpha = 0.3f))
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(Primary, PrimaryDark)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.mipmap.ic_launcher_foreground),
                        contentDescription = null,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Text(
                    text = "MediaGrab",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            // Action Buttons
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(
                    onClick = onHistoryClick,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.History,
                        contentDescription = "Geçmiş",
                        tint = MaterialTheme.colorScheme.outline
                    )
                }

                IconButton(
                    onClick = onSettingsClick,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = "Ayarlar",
                        tint = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}


@Composable
private fun InputSection(
    url: String,
    onUrlChange: (String) -> Unit,
    selectedFormat: String,
    onFormatChange: (String) -> Unit,
    selectedQuality: String,
    availableQualities: List<com.berat.mediagrab.data.model.VideoQuality>,
    onQualityChange: (String) -> Unit,
    isLoading: Boolean,
    isDownloading: Boolean,
    onDownloadClick: () -> Unit,
    onPaste: () -> Unit,
    onClear: () -> Unit,
    platform: com.berat.mediagrab.util.SupportedPlatform
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // URL Input Field
        UrlInputField(
            value = url,
            onValueChange = onUrlChange,
            platform = platform,
            onPaste = onPaste,
            onClear = onClear
        )

        // Format and Quality Selectors
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            FormatSelector(
                selectedFormat = selectedFormat,
                onFormatSelected = onFormatChange,
                modifier = Modifier.weight(1f)
            )

            QualitySelector(
                availableQualities = availableQualities,
                selectedQuality = selectedQuality,
                onQualitySelected = onQualityChange,
                isVideoFormat = selectedFormat.startsWith("video"),
                modifier = Modifier.weight(1f)
            )
        }

        // Download Button
        Button(
            onClick = onDownloadClick,
            enabled = url.isNotBlank() && !isLoading && !isDownloading,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Primary,
                disabledContainerColor = Primary.copy(alpha = 0.5f)
            ),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 8.dp,
                pressedElevation = 4.dp
            )
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.White,
                    strokeWidth = 2.5.dp
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "İndir",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun DownloadProgressCard(
    progress: Float,
    statusMessage: String?,
    downloadSpeed: Long?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = statusMessage ?: "İndiriliyor...",
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${(progress * 100).toInt()}%",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Primary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = Primary,
                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            )

            downloadSpeed?.let { speed ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = formatSpeed(speed),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}


@Composable
private fun MediaInfoCard(info: com.berat.mediagrab.data.model.MediaInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail
            info.thumbnail?.let { thumb ->
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    AsyncImage(
                        model = thumb,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    // Play overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = info.title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = buildString {
                        append(info.formattedDuration)
                        info.uploader?.let { append(" • $it") }
                    },
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
private fun SupportedPlatformsSection() {
    Column {
        Text(
            text = "Desteklenen Platformlar",
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
            PlatformIconSvg(
                iconPath = "file:///android_asset/icons/YouTube Logo.svg",
                label = "YouTube"
            )
            PlatformIconSvg(
                iconPath = "file:///android_asset/icons/Instagram Logo.svg",
                label = "Instagram"
            )
            PlatformIconSvg(
                iconPath = "file:///android_asset/icons/TikTok Logo.svg",
                label = "TikTok"
            )
            PlatformIconSvg(
                iconPath = "file:///android_asset/icons/X Logo.svg",
                label = "Twitter"
            )
        }
    }
}

@Composable
private fun PlatformIconSvg(
    iconPath: String,
    label: String
) {
    val context = LocalContext.current
    val painter = rememberAsyncImagePainter(
        model = ImageRequest.Builder(context)
            .data(iconPath)
            .decoderFactory(SvgDecoder.Factory())
            .build()
    )
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            modifier = Modifier.size(64.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant
            )
        ) {
            Box(contentAlignment = Alignment.Center) {
                Image(
                    painter = painter,
                    contentDescription = label,
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


@Composable
private fun RecentDownloadsSection(
    downloads: List<DownloadEntity>,
    onClearAll: () -> Unit,
    onItemClick: (DownloadEntity) -> Unit,
    onItemDelete: (Long) -> Unit
) {
    Column {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Son İndirilenler",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.outline,
                letterSpacing = 1.sp
            )
            TextButton(
                onClick = onClearAll,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Tümünü Temizle",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Primary
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (downloads.isEmpty()) {
            // Empty state
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.DownloadDone,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Henüz indirme yok",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                downloads.take(3).forEach { download ->
                    RecentDownloadItem(
                        download = download,
                        onClick = { onItemClick(download) },
                        onMoreClick = { onItemDelete(download.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun RecentDownloadItem(
    download: DownloadEntity,
    onClick: () -> Unit,
    onMoreClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                if (download.thumbnail != null) {
                    AsyncImage(
                        model = download.thumbnail,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (download.format.contains("audio", ignoreCase = true))
                                Icons.Default.Headphones
                            else Icons.Default.Image,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // Play overlay for videos
                if (!download.format.contains("audio", ignoreCase = true) && download.thumbnail != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = download.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Format badge
                    val formatLabel = when {
                        download.format.contains("mp4", ignoreCase = true) -> "MP4"
                        download.format.contains("mp3", ignoreCase = true) -> "MP3"
                        download.format.contains("mkv", ignoreCase = true) -> "MKV"
                        download.format.contains("audio", ignoreCase = true) -> "Audio"
                        else -> "Video"
                    }
                    val isAudio = download.format.contains("audio", ignoreCase = true) ||
                            download.format.contains("mp3", ignoreCase = true)

                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (isAudio)
                            Color(0xFFF97316).copy(alpha = 0.1f)
                        else
                            Primary.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = formatLabel,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isAudio) Color(0xFFF97316) else Primary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    // File size
                    if (download.fileSize != null && download.fileSize > 0) {
                        Text(
                            text = formatFileSize(download.fileSize),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            // More button
            IconButton(
                onClick = onMoreClick,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Daha fazla",
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

private fun formatSpeed(bytesPerSecond: Long): String {
    return when {
        bytesPerSecond >= 1024 * 1024 -> {
            val mbps = bytesPerSecond / (1024.0 * 1024.0)
            String.format(java.util.Locale.US, "%.2f MB/s", mbps)
        }
        bytesPerSecond >= 1024 -> {
            val kbps = bytesPerSecond / 1024.0
            String.format(java.util.Locale.US, "%.1f KB/s", kbps)
        }
        else -> "$bytesPerSecond B/s"
    }
}

fun formatFileSize(bytes: Long): String {
    return when {
        bytes >= 1024 * 1024 * 1024 -> {
            val gb = bytes / (1024.0 * 1024.0 * 1024.0)
            String.format(java.util.Locale.US, "%.1f GB", gb)
        }
        bytes >= 1024 * 1024 -> {
            val mb = bytes / (1024.0 * 1024.0)
            String.format(java.util.Locale.US, "%.1f MB", mb)
        }
        bytes >= 1024 -> {
            val kb = bytes / 1024.0
            String.format(java.util.Locale.US, "%.1f KB", kb)
        }
        bytes > 0 -> "$bytes B"
        else -> ""
    }
}
