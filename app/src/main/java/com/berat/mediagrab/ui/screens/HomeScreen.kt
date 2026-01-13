package com.berat.mediagrab.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
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

        Scaffold(
                topBar = {
                        TopAppBar(
                                title = { Text("MediaGrab", fontWeight = FontWeight.Bold) },
                                colors =
                                        TopAppBarDefaults.topAppBarColors(
                                                containerColor = Anthracite,
                                                titleContentColor = Color.White
                                        ),
                                actions = {
                                        IconButton(onClick = onNavigateToDownloads) {
                                                Icon(
                                                        Icons.Default.History,
                                                        contentDescription = "Geçmiş",
                                                        tint = Color.White
                                                )
                                        }
                                        IconButton(onClick = onNavigateToSettings) {
                                                Icon(
                                                        Icons.Default.Settings,
                                                        contentDescription = "Ayarlar",
                                                        tint = Color.White
                                                )
                                        }
                                }
                        )
                }
        ) { paddingValues ->
                Column(
                        modifier =
                                Modifier.fillMaxSize()
                                        .padding(paddingValues)
                                        .verticalScroll(rememberScrollState())
                                        .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                ) {
                        // Hero Section
                        Text(
                                text = "Video İndirin",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                                text = "1000+ platformdan video indirin",
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.outline,
                                textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // URL Input
                        UrlInputField(
                                value = uiState.url,
                                onValueChange = { viewModel.updateUrl(it) },
                                platform = platform,
                                onPaste = {
                                        clipboardManager.getText()?.text?.let {
                                                viewModel.updateUrl(it)
                                        }
                                },
                                onClear = { viewModel.updateUrl("") }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Format and Quality Selectors
                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                                FormatSelector(
                                        selectedFormat = uiState.selectedFormat,
                                        onFormatSelected = { viewModel.updateFormat(it) },
                                        modifier = Modifier.weight(1f)
                                )

                                QualitySelector(
                                        availableQualities = uiState.mediaInfo?.availableQualities
                                                        ?: emptyList(),
                                        selectedQuality = uiState.selectedQuality,
                                        onQualitySelected = { viewModel.updateQuality(it) },
                                        isVideoFormat = uiState.selectedFormat.startsWith("video"),
                                        modifier = Modifier.weight(1f)
                                )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Download Button
                        Button(
                                onClick = { viewModel.startDownload() },
                                enabled =
                                        uiState.url.isNotBlank() &&
                                                !uiState.isLoading &&
                                                !uiState.isDownloading,
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors =
                                        ButtonDefaults.buttonColors(
                                                containerColor = Primary,
                                                disabledContainerColor = Primary.copy(alpha = 0.5f)
                                        )
                        ) {
                                if (uiState.isDownloading) {
                                        Text(
                                                "İNDİRİLİYOR...",
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold
                                        )
                                } else if (uiState.isLoading) {
                                        CircularProgressIndicator(
                                                modifier = Modifier.size(24.dp),
                                                color = Color.White,
                                                strokeWidth = 2.5.dp
                                        )
                                } else {
                                        Icon(
                                                Icons.Default.Download,
                                                contentDescription = null,
                                                modifier = Modifier.size(22.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                                "İNDİR",
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold
                                        )
                                }
                        }

                        // Download Progress Bar
                        if (uiState.isDownloading) {
                                Spacer(modifier = Modifier.height(16.dp))

                                Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        colors =
                                                CardDefaults.cardColors(
                                                        containerColor =
                                                                MaterialTheme.colorScheme
                                                                        .surfaceVariant
                                                )
                                ) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                                Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement =
                                                                Arrangement.SpaceBetween,
                                                        verticalAlignment =
                                                                Alignment.CenterVertically
                                                ) {
                                                        Text(
                                                                uiState.statusMessage
                                                                        ?: "İndiriliyor...",
                                                                fontWeight = FontWeight.Medium,
                                                                fontSize = 14.sp
                                                        )
                                                        Text(
                                                                "${(uiState.downloadProgress * 100).toInt()}%",
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = 16.sp,
                                                                color = Primary
                                                        )
                                                }

                                                Spacer(modifier = Modifier.height(8.dp))

                                                LinearProgressIndicator(
                                                        progress = { uiState.downloadProgress },
                                                        modifier =
                                                                Modifier.fillMaxWidth()
                                                                        .height(8.dp)
                                                                        .clip(
                                                                                RoundedCornerShape(
                                                                                        4.dp
                                                                                )
                                                                        ),
                                                        color = Primary,
                                                        trackColor =
                                                                MaterialTheme.colorScheme.outline
                                                                        .copy(alpha = 0.3f),
                                                )

                                                // Speed display
                                                uiState.downloadSpeed?.let { speedBytes ->
                                                        Spacer(modifier = Modifier.height(8.dp))
                                                        Text(
                                                                text = formatSpeed(speedBytes),
                                                                fontSize = 12.sp,
                                                                color =
                                                                        MaterialTheme.colorScheme
                                                                                .outline
                                                        )
                                                }
                                        }
                                }
                        }

                        // Show video info if available
                        uiState.mediaInfo?.let { info ->
                                Spacer(modifier = Modifier.height(16.dp))

                                Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        colors =
                                                CardDefaults.cardColors(
                                                        containerColor =
                                                                MaterialTheme.colorScheme
                                                                        .surfaceVariant
                                                )
                                ) {
                                        Row(
                                                modifier = Modifier.padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                        ) {
                                                // Thumbnail
                                                info.thumbnail?.let { thumb ->
                                                        AsyncImage(
                                                                model = thumb,
                                                                contentDescription = null,
                                                                modifier =
                                                                        Modifier.size(80.dp, 45.dp)
                                                                                .clip(
                                                                                        RoundedCornerShape(
                                                                                                8.dp
                                                                                        )
                                                                                ),
                                                                contentScale = ContentScale.Crop
                                                        )
                                                        Spacer(modifier = Modifier.width(12.dp))
                                                }

                                                Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                                info.title,
                                                                fontWeight = FontWeight.SemiBold,
                                                                fontSize = 14.sp,
                                                                maxLines = 2,
                                                                overflow = TextOverflow.Ellipsis
                                                        )
                                                        Spacer(modifier = Modifier.height(4.dp))
                                                        Text(
                                                                buildString {
                                                                        append(
                                                                                info.formattedDuration
                                                                        )
                                                                        info.uploader?.let {
                                                                                append(" • $it")
                                                                        }
                                                                },
                                                                fontSize = 12.sp,
                                                                color =
                                                                        MaterialTheme.colorScheme
                                                                                .outline
                                                        )
                                                }
                                        }
                                }
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        // Platform Grid
                        PlatformGrid()

                        Spacer(modifier = Modifier.height(32.dp))

                        // Recent Downloads Section
                        RecentDownloadsSection(
                                downloads = recentDownloads,
                                onViewAll = onNavigateToDownloads,
                                onDelete = { viewModel.deleteDownload(it) }
                        )
                }
        }
}

@Composable
private fun RecentDownloadsSection(
        downloads: List<DownloadEntity>,
        onViewAll: () -> Unit,
        onDelete: (Long) -> Unit
) {
        Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                ) {
                        Text(
                                "Son İndirilenler",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.outline,
                                letterSpacing = 0.5.sp
                        )

                        TextButton(onClick = onViewAll) {
                                Text(
                                        "Tümünü Gör",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Primary
                                )
                        }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (downloads.isEmpty()) {
                        // Empty state
                        Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors =
                                        CardDefaults.cardColors(
                                                containerColor =
                                                        MaterialTheme.colorScheme.surfaceVariant
                                        )
                        ) {
                                Column(
                                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                        Icon(
                                                Icons.Default.DownloadDone,
                                                contentDescription = null,
                                                modifier = Modifier.size(48.dp),
                                                tint = MaterialTheme.colorScheme.outline
                                        )

                                        Spacer(modifier = Modifier.height(12.dp))

                                        Text(
                                                "Henüz indirme yok",
                                                fontSize = 14.sp,
                                                color = MaterialTheme.colorScheme.outline
                                        )
                                }
                        }
                } else {
                        // Show downloads
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                downloads.forEach { download ->
                                        DownloadItemCard(
                                                download = download,
                                                onDelete = { onDelete(download.id) }
                                        )
                                }
                        }
                }
        }
}

@Composable
private fun DownloadItemCard(download: DownloadEntity, onDelete: () -> Unit) {
        Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors =
                        CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
        ) {
                Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                ) {
                        // Thumbnail
                        Box(
                                modifier =
                                        Modifier.size(60.dp, 40.dp).clip(RoundedCornerShape(6.dp)),
                                contentAlignment = Alignment.Center
                        ) {
                                if (download.thumbnail != null) {
                                        AsyncImage(
                                                model = download.thumbnail,
                                                contentDescription = null,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                        )
                                } else {
                                        Surface(
                                                color = MaterialTheme.colorScheme.surface,
                                                modifier = Modifier.fillMaxSize()
                                        ) {
                                                Icon(
                                                        Icons.Default.VideoFile,
                                                        contentDescription = null,
                                                        modifier =
                                                                Modifier.padding(8.dp).size(24.dp),
                                                        tint = MaterialTheme.colorScheme.outline
                                                )
                                        }
                                }

                                // Status overlay
                                if (download.isDownloading) {
                                        Surface(
                                                color = Color.Black.copy(alpha = 0.6f),
                                                modifier = Modifier.fillMaxSize()
                                        ) {
                                                CircularProgressIndicator(
                                                        progress = { download.progress },
                                                        modifier = Modifier.size(20.dp),
                                                        color = Primary,
                                                        strokeWidth = 2.dp
                                                )
                                        }
                                }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                                Text(
                                        download.title,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.height(2.dp))

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                        // Status icon
                                        Icon(
                                                imageVector =
                                                        when {
                                                                download.isCompleted ->
                                                                        Icons.Default.CheckCircle
                                                                download.isFailed ->
                                                                        Icons.Default.Error
                                                                download.isDownloading ->
                                                                        Icons.Default.Downloading
                                                                else -> Icons.Default.Schedule
                                                        },
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp),
                                                tint =
                                                        when {
                                                                download.isCompleted ->
                                                                        Color(0xFF4CAF50)
                                                                download.isFailed ->
                                                                        Color(0xFFF44336)
                                                                else ->
                                                                        MaterialTheme.colorScheme
                                                                                .outline
                                                        }
                                        )

                                        Spacer(modifier = Modifier.width(4.dp))

                                        Text(
                                                text =
                                                        when {
                                                                download.isCompleted -> "Tamamlandı"
                                                                download.isFailed -> "Başarısız"
                                                                download.isDownloading ->
                                                                        "${(download.progress * 100).toInt()}%"
                                                                else -> "Bekliyor"
                                                        },
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.outline
                                        )

                                        download.formattedDuration.takeIf { it.isNotEmpty() }?.let {
                                                Text(
                                                        " • $it",
                                                        fontSize = 11.sp,
                                                        color = MaterialTheme.colorScheme.outline
                                                )
                                        }
                                }
                        }

                        // Delete button
                        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                                Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Sil",
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.outline
                                )
                        }
                }
        }
}

// Format download speed for display
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
