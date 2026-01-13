package com.berat.mediagrab.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.berat.mediagrab.data.database.DownloadEntity
import com.berat.mediagrab.ui.theme.Anthracite
import com.berat.mediagrab.ui.theme.Primary
import com.berat.mediagrab.ui.viewmodel.DownloadsViewModel
import com.berat.mediagrab.util.VideoMetadataExtractor
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(onNavigateBack: () -> Unit, viewModel: DownloadsViewModel = hiltViewModel()) {
    val downloads by viewModel.downloads.collectAsState(initial = emptyList())
    val context = LocalContext.current
    var selectedDownload by remember { mutableStateOf<DownloadEntity?>(null) }
    var showDeleteDialog by remember { mutableStateOf<DownloadEntity?>(null) }

    Scaffold(
            topBar = {
                TopAppBar(
                        title = { Text("Dosyalarım", fontWeight = FontWeight.Bold) },
                        navigationIcon = {
                            IconButton(onClick = onNavigateBack) {
                                Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Geri",
                                        tint = Color.White
                                )
                            }
                        },
                        colors =
                                TopAppBarDefaults.topAppBarColors(
                                        containerColor = Anthracite,
                                        titleContentColor = Color.White
                                )
                )
            }
    ) { paddingValues ->
        if (downloads.isEmpty()) {
            // Empty state
            EmptyDownloadsState(modifier = Modifier.padding(paddingValues))
        } else {
            LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(downloads, key = { it.id }) { download ->
                    DownloadItem(
                            download = download,
                            onPlayClick = {
                                download.filePath?.let { path ->
                                    val file = File(path)
                                    if (file.exists()) {
                                        try {
                                            val uri =
                                                    FileProvider.getUriForFile(
                                                            context,
                                                            "${context.packageName}.fileprovider",
                                                            file
                                                    )
                                            val intent =
                                                    Intent(Intent.ACTION_VIEW).apply {
                                                        setDataAndType(uri, "video/*")
                                                        addFlags(
                                                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                                                        )
                                                    }
                                            context.startActivity(intent)
                                        } catch (_: Exception) {
                                            Toast.makeText(
                                                            context,
                                                            "Dosya açılamadı",
                                                            Toast.LENGTH_SHORT
                                                    )
                                                    .show()
                                        }
                                    } else {
                                        Toast.makeText(
                                                        context,
                                                        "Dosya bulunamadı",
                                                        Toast.LENGTH_SHORT
                                                )
                                                .show()
                                    }
                                }
                            },
                            onInfoClick = { selectedDownload = download },
                            onDeleteClick = { showDeleteDialog = download }
                    )
                }
            }
        }
    }

    // Info Dialog
    selectedDownload?.let { download ->
        DownloadInfoDialog(download = download, onDismiss = { selectedDownload = null })
    }

    // Delete Confirmation Dialog
    showDeleteDialog?.let { download ->
        AlertDialog(
                onDismissRequest = { showDeleteDialog = null },
                title = { Text("Silmek istediğinize emin misiniz?") },
                text = { Text("\"${download.title}\" silinecek.") },
                confirmButton = {
                    TextButton(
                            onClick = {
                                viewModel.deleteDownload(download)
                                showDeleteDialog = null
                            }
                    ) { Text("Sil", color = Color.Red) }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = null }) { Text("İptal") }
                }
        )
    }
}

@Composable
private fun EmptyDownloadsState(modifier: Modifier = Modifier) {
    Column(
            modifier = modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
    ) {
        Icon(
                imageVector = Icons.Default.FolderOpen,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
                "Henüz indirme yok",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
                "İndirdiğiniz videolar burada görünecek",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.outline
        )
    }
}

@Composable
private fun DownloadItem(
        download: DownloadEntity,
        onPlayClick: () -> Unit,
        onInfoClick: () -> Unit,
        onDeleteClick: () -> Unit
) {
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
                            Modifier.size(80.dp, 60.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .clickable(enabled = download.isCompleted) { onPlayClick() },
                    contentAlignment = Alignment.Center
            ) {
                if (download.thumbnail != null) {
                    AsyncImage(
                            model = download.thumbnail,
                            contentDescription = download.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp))
                    )
                }

                // Play overlay for completed downloads
                if (download.isCompleted) {
                    Box(
                            modifier =
                                    Modifier.size(32.dp)
                                            .background(Primary.copy(alpha = 0.9f), CircleShape),
                            contentAlignment = Alignment.Center
                    ) {
                        Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Oynat",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Status overlay for non-completed
                if (download.isDownloading) {
                    CircularProgressIndicator(
                            progress = { download.progress },
                            modifier = Modifier.size(32.dp),
                            color = Primary,
                            strokeWidth = 3.dp
                    )
                }

                if (download.isFailed) {
                    Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = "Hata",
                            tint = Color.Red,
                            modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                        download.title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Basic info row
                Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Duration
                    if (download.formattedDuration.isNotEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint = MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                    download.formattedDuration,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }

                    // Quality
                    Text(
                            download.quality.uppercase(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Primary,
                            modifier =
                                    Modifier.background(
                                                    Primary.copy(alpha = 0.1f),
                                                    RoundedCornerShape(4.dp)
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )

                    // Format
                    Text(
                            download.format.uppercase(),
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.outline
                    )
                }

                // File size
                download.fileSize?.let { size ->
                    if (size > 0) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                                formatFileSize(size),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Actions
            Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Info button
                IconButton(onClick = onInfoClick, modifier = Modifier.size(32.dp)) {
                    Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Detaylar",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                    )
                }

                // Delete button
                IconButton(onClick = onDeleteClick, modifier = Modifier.size(32.dp)) {
                    Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Sil",
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DownloadInfoDialog(download: DownloadEntity, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // Header
                Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                            "Video Detayları",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Kapat")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Thumbnail
                download.thumbnail?.let { thumb ->
                    AsyncImage(
                            model = thumb,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier =
                                    Modifier.fillMaxWidth()
                                            .height(150.dp)
                                            .clip(RoundedCornerShape(8.dp))
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Title
                Text(
                        download.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Details
                InfoRow("Yükleyen", download.uploader ?: "Bilinmiyor")
                InfoRow("Süre", download.formattedDuration.ifEmpty { "Bilinmiyor" })
                InfoRow("Format", download.format.uppercase())
                InfoRow("Kalite", download.quality.uppercase())
                InfoRow("Boyut", download.fileSize?.let { formatFileSize(it) } ?: "Bilinmiyor")
                InfoRow(
                        "Durum",
                        when {
                            download.isCompleted -> "✅ Tamamlandı"
                            download.isDownloading -> "⏳ İndiriliyor"
                            download.isFailed -> "❌ Başarısız"
                            else -> "⏸ Bekliyor"
                        }
                )
                InfoRow("İndirilme Tarihi", formatDate(download.createdAt))
                download.completedAt?.let { InfoRow("Tamamlanma Tarihi", formatDate(it)) }
                download.filePath?.let { path ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                            "Dosya Yolu:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                            path,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                    )
                }

                // Video file metadata (outside the let block to avoid type inference issues)
                val filePath = download.filePath
                if (filePath != null) {
                    val videoMetadata =
                            remember(filePath) { VideoMetadataExtractor.extractMetadata(filePath) }
                    if (videoMetadata != null) {
                        Spacer(modifier = Modifier.height(16.dp))

                        // Section header
                        Text(
                                "📹 Video Teknik Bilgileri",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        InfoRow("Çözünürlük", videoMetadata.resolution)
                        // FPS: try metadata first, fallback to quality string (e.g., "1080p60")
                        val fpsDisplay =
                                if (videoMetadata.frameRate != null && videoMetadata.frameRate > 0
                                ) {
                                    videoMetadata.formattedFps
                                } else {
                                    // Parse fps from quality string like "1080p60"
                                    val fpsMatch = Regex("(\\d+)p(\\d+)").find(download.quality)
                                    if (fpsMatch != null) {
                                        "${fpsMatch.groupValues[2]} fps"
                                    } else {
                                        // Don't assume 30fps - video could be 60fps
                                        "Bilinmiyor"
                                    }
                                }
                        InfoRow("Kare Hızı (FPS)", fpsDisplay)
                        InfoRow("Bit Hızı", videoMetadata.formattedBitrate)
                        videoMetadata.mimeType?.let { mime -> InfoRow("MIME Tipi", mime) }
                        if (videoMetadata.rotation != null && videoMetadata.rotation != 0) {
                            InfoRow("Döndürme", "${videoMetadata.rotation}°")
                        }
                        InfoRow("Gerçek Dosya Boyutu", videoMetadata.formattedFileSize)
                    }
                }

                // Error message if failed
                download.errorMessage?.let { error ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Hata: $error", fontSize = 12.sp, color = Color.Red)
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.outline)
        Text(
                value,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes >= 1_073_741_824 -> String.format(Locale.US, "%.2f GB", bytes / 1_073_741_824.0)
        bytes >= 1_048_576 -> String.format(Locale.US, "%.1f MB", bytes / 1_048_576.0)
        bytes >= 1024 -> String.format(Locale.US, "%.1f KB", bytes / 1024.0)
        else -> "$bytes B"
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("tr", "TR"))
    return sdf.format(Date(timestamp))
}
