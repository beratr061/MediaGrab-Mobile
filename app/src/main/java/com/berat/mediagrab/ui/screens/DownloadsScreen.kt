package com.berat.mediagrab.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.*
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
import com.berat.mediagrab.ui.theme.Primary
import com.berat.mediagrab.ui.viewmodel.DownloadsViewModel
import com.berat.mediagrab.util.VideoMetadataExtractor
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

private fun formatDateDownloads(timestamp: Long): String {
    return SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("tr", "TR")).format(Date(timestamp))
}

private fun getRelativeTimeDownloads(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    val minutes = diff / 60000
    val hours = minutes / 60
    val days = hours / 24
    return when {
        minutes < 1 -> "Az önce"
        minutes < 60 -> "$minutes dk önce"
        hours < 24 -> "$hours saat önce"
        days == 1L -> "Dün"
        days < 7 -> "$days gün önce"
        else -> formatDateDownloads(timestamp)
    }
}

private fun formatFileSizeDownloads(size: Long): String = when {
    size < 1024 -> "$size B"
    size < 1024 * 1024 -> "${size / 1024} KB"
    size < 1024 * 1024 * 1024 -> "${size / (1024 * 1024)} MB"
    else -> String.format(Locale.US, "%.1f GB", size / (1024.0 * 1024.0 * 1024.0))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(onNavigateBack: () -> Unit, viewModel: DownloadsViewModel = hiltViewModel()) {
    val downloads by viewModel.downloads.collectAsState(initial = emptyList())
    val context = LocalContext.current
    var selectedDownload by remember { mutableStateOf<DownloadEntity?>(null) }
    var showDeleteDialog by remember { mutableStateOf<DownloadEntity?>(null) }
    var showClearAllDialog by remember { mutableStateOf(false) }
    val activeDownloads = downloads.filter { it.isDownloading }
    val historyDownloads = downloads.filter { !it.isDownloading }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("İndirmeler", fontWeight = FontWeight.SemiBold, fontSize = 17.sp) },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .padding(start = 4.dp)
                            .size(40.dp)
                            .clip(CircleShape)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri", Modifier.size(22.dp))
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.95f)
                )
            )
        }
    ) { paddingValues ->
        if (downloads.isEmpty()) {
            DownloadsEmptyState(Modifier.padding(paddingValues))
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                if (activeDownloads.isNotEmpty()) {
                    item {
                        DownloadsSectionHeader(
                            title = "AKTİF İNDİRMELER",
                            count = activeDownloads.size
                        )
                    }
                    item {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            activeDownloads.forEach { download ->
                                ActiveDownloadCard(download)
                            }
                        }
                    }
                    item {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )
                    }
                }
                if (historyDownloads.isNotEmpty()) {
                    item {
                        DownloadsSectionHeaderWithAction(
                            title = "GEÇMİŞ",
                            actionText = "Tümünü Temizle",
                            onActionClick = { showClearAllDialog = true }
                        )
                    }
                    items(historyDownloads, key = { it.id }) { download ->
                        DownloadHistoryItem(
                            download = download,
                            onItemClick = {
                                download.filePath?.let { path ->
                                    val file = File(path)
                                    if (file.exists()) {
                                        try {
                                            val uri = FileProvider.getUriForFile(
                                                context,
                                                "${context.packageName}.fileprovider",
                                                file
                                            )
                                            context.startActivity(
                                                Intent(Intent.ACTION_VIEW).apply {
                                                    setDataAndType(uri, "video/*")
                                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                }
                                            )
                                        } catch (_: Exception) {
                                            Toast.makeText(context, "Dosya açılamadı", Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        Toast.makeText(context, "Dosya bulunamadı", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            onMoreClick = { selectedDownload = download },
                            onDeleteClick = { showDeleteDialog = download }
                        )
                    }
                }
            }
        }
    }

    selectedDownload?.let { download ->
        DownloadInfoDialog(download) { selectedDownload = null }
    }

    showDeleteDialog?.let { download ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            icon = {
                Icon(
                    Icons.Outlined.Delete,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = { Text("Silmek istediğinize emin misiniz?", fontWeight = FontWeight.Bold) },
            text = { Text("\"${download.title}\" silinecek.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteDownload(download)
                    showDeleteDialog = null
                }) {
                    Text("Sil", color = Primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("İptal")
                }
            }
        )
    }

    if (showClearAllDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllDialog = false },
            icon = {
                Icon(
                    Icons.Outlined.DeleteSweep,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = { Text("Tüm geçmişi temizle", fontWeight = FontWeight.Bold) },
            text = { Text("Tüm indirme geçmişi silinecek. Bu işlem geri alınamaz.") },
            confirmButton = {
                TextButton(onClick = {
                    historyDownloads.forEach { viewModel.deleteDownload(it) }
                    showClearAllDialog = false
                }) {
                    Text("Temizle", color = Primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllDialog = false }) {
                    Text("İptal")
                }
            }
        )
    }
}

@Composable
private fun DownloadsSectionHeader(title: String, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.outline,
            letterSpacing = 1.sp
        )
        Box(
            modifier = Modifier
                .background(Primary.copy(alpha = 0.1f), RoundedCornerShape(50))
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text(
                text = count.toString(),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Primary
            )
        }
    }
}

@Composable
private fun DownloadsSectionHeaderWithAction(
    title: String,
    actionText: String,
    onActionClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.outline,
            letterSpacing = 1.sp
        )
        Text(
            text = actionText,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = Primary,
            modifier = Modifier.clickable { onActionClick() }
        )
    }
}

@Composable
private fun ActiveDownloadCard(download: DownloadEntity) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                download.thumbnail?.let {
                    AsyncImage(
                        model = it,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.Download,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = Color.White
                    )
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .align(Alignment.CenterVertically),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = download.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${(download.progress * 100).toInt()}%",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = "İndiriliyor...",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Primary
                    )
                }
                LinearProgressIndicator(
                    progress = { download.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = Primary,
                    trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )
            }
            IconButton(
                onClick = {},
                modifier = Modifier
                    .size(32.dp)
                    .align(Alignment.CenterVertically)
                    .background(MaterialTheme.colorScheme.surface, CircleShape)
            ) {
                Icon(
                    Icons.Outlined.Pause,
                    contentDescription = "Duraklat",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun DownloadHistoryItem(
    download: DownloadEntity,
    onItemClick: () -> Unit,
    onMoreClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val isFailed = download.isFailed
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isFailed) { onItemClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    if (isFailed) Color(0xFF7F1D1D).copy(alpha = 0.2f)
                    else MaterialTheme.colorScheme.surface
                ),
            contentAlignment = Alignment.Center
        ) {
            when {
                isFailed -> Icon(
                    Icons.Outlined.ErrorOutline,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = Color(0xFFEF4444)
                )
                download.thumbnail != null -> {
                    AsyncImage(
                        model = download.thumbnail,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    if (download.formattedDuration.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(4.dp)
                                .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(2.dp))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = download.formattedDuration,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White
                            )
                        }
                    }
                }
                else -> Icon(
                    Icons.Outlined.MusicNote,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.outline
                )
            }
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = if (isFailed) "Başarısız: ${download.title}" else download.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (isFailed) Color(0xFFF87171) else MaterialTheme.colorScheme.onSurface
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isFailed) {
                    Text(
                        text = download.errorMessage ?: "Hata",
                        fontSize = 12.sp,
                        color = Color(0xFFF87171).copy(alpha = 0.7f)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(2.dp))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = download.format.uppercase(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    download.fileSize?.let { fileSize ->
                        if (fileSize > 0) {
                            Text(
                                text = formatFileSizeDownloads(fileSize),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                    Text(
                        text = "•",
                        fontSize = 8.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = getRelativeTimeDownloads(download.completedAt ?: download.createdAt),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
        IconButton(
            onClick = { if (isFailed) onDeleteClick() else onMoreClick() },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = if (isFailed) Icons.Outlined.Delete else Icons.Outlined.MoreVert,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.outline
            )
        }
    }
    HorizontalDivider(
        modifier = Modifier.padding(start = 84.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
    )
}

@Composable
private fun DownloadsEmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(Primary.copy(alpha = 0.1f), RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Outlined.FolderOpen,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = Primary
            )
        }
        Spacer(Modifier.height(20.dp))
        Text(
            text = "Henüz indirme yok",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "İndirdiğiniz videolar burada görünecek",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.outline
        )
    }
}


@Composable
private fun DownloadInfoDialog(download: DownloadEntity, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Video Detayları",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Kapat")
                    }
                }
                Spacer(Modifier.height(16.dp))
                download.thumbnail?.let { thumbnail ->
                    AsyncImage(
                        model = thumbnail,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.height(16.dp))
                }
                Text(
                    text = download.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(16.dp))
                DownloadInfoRow("Yükleyen", download.uploader ?: "Bilinmiyor")
                DownloadInfoRow("Süre", download.formattedDuration.ifEmpty { "Bilinmiyor" })
                DownloadInfoRow("Format", download.format.uppercase())
                DownloadInfoRow("Kalite", download.quality.uppercase())
                DownloadInfoRow("Boyut", download.fileSize?.let { formatFileSizeDownloads(it) } ?: "Bilinmiyor")
                DownloadInfoRow(
                    "Durum",
                    when {
                        download.isCompleted -> "✅ Tamamlandı"
                        download.isDownloading -> "⏳ İndiriliyor"
                        download.isFailed -> "❌ Başarısız"
                        else -> "⏸ Bekliyor"
                    }
                )
                DownloadInfoRow("İndirilme Tarihi", formatDateDownloads(download.createdAt))
                download.completedAt?.let { completedAt ->
                    DownloadInfoRow("Tamamlanma Tarihi", formatDateDownloads(completedAt))
                }

                download.filePath?.let { path ->
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Dosya Yolu:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = path,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    val metadata = remember(path) { VideoMetadataExtractor.extractMetadata(path) }
                    metadata?.let { meta ->
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "📹 Video Teknik Bilgileri",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Primary
                        )
                        Spacer(Modifier.height(8.dp))
                        DownloadInfoRow("Çözünürlük", meta.resolution)
                        val fpsValue = if (meta.frameRate != null && meta.frameRate > 0) {
                            meta.formattedFps
                        } else {
                            Regex("(\\d+)p(\\d+)").find(download.quality)?.let { matchResult ->
                                "${matchResult.groupValues[2]} fps"
                            } ?: "Bilinmiyor"
                        }
                        DownloadInfoRow("Kare Hızı (FPS)", fpsValue)
                        DownloadInfoRow("Bit Hızı", meta.formattedBitrate)
                        meta.mimeType?.let { mimeType ->
                            DownloadInfoRow("MIME Tipi", mimeType)
                        }
                        if (meta.rotation != null && meta.rotation != 0) {
                            DownloadInfoRow("Döndürme", "${meta.rotation}°")
                        }
                        DownloadInfoRow("Gerçek Dosya Boyutu", meta.formattedFileSize)
                    }
                }
                download.errorMessage?.let { errorMessage ->
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Hata: $errorMessage",
                        fontSize = 12.sp,
                        color = Color.Red
                    )
                }
            }
        }
    }
}

@Composable
private fun DownloadInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.outline
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
