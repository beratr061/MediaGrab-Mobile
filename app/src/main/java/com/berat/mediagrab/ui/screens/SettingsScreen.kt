package com.berat.mediagrab.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import com.berat.mediagrab.data.DownloadQuality
import com.berat.mediagrab.data.VideoFormat
import com.berat.mediagrab.ui.theme.Anthracite
import com.berat.mediagrab.ui.theme.Primary
import com.berat.mediagrab.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onNavigateBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val context = LocalContext.current

    // Collect settings
    val downloadQuality by viewModel.downloadQuality.collectAsState()
    val preferredFormat by viewModel.preferredFormat.collectAsState()
    val darkMode by viewModel.darkMode.collectAsState()
    val autoDownload by viewModel.autoDownload.collectAsState()
    val showNotifications by viewModel.showNotifications.collectAsState()
    val wifiOnly by viewModel.wifiOnly.collectAsState()
    val autoClearCache by viewModel.autoClearCache.collectAsState()

    // Dialog states
    var showQualityDialog by remember { mutableStateOf(false) }
    var showFormatDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    Scaffold(
            topBar = {
                TopAppBar(
                        title = { Text("Ayarlar", fontWeight = FontWeight.Bold) },
                        navigationIcon = {
                            IconButton(onClick = onNavigateBack) {
                                Icon(
                                        Icons.AutoMirrored.Filled.ArrowBack,
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
        LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Download Settings Section
            item {
                SettingsSectionHeader(icon = Icons.Default.Download, title = "İndirme Ayarları")
            }

            item {
                SettingsClickableItem(
                        icon = Icons.Default.HighQuality,
                        title = "İndirme Kalitesi",
                        subtitle = downloadQuality.displayName,
                        onClick = { showQualityDialog = true }
                )
            }

            item {
                SettingsClickableItem(
                        icon = Icons.Default.VideoFile,
                        title = "Tercih Edilen Format",
                        subtitle = preferredFormat.displayName,
                        onClick = { showFormatDialog = true }
                )
            }

            item {
                SettingsSwitchItem(
                        icon = Icons.Default.Wifi,
                        title = "Sadece WiFi'de İndir",
                        subtitle = "Mobil veri kullanmadan indir",
                        checked = wifiOnly,
                        onCheckedChange = { viewModel.setWifiOnly(it) }
                )
            }

            item {
                SettingsSwitchItem(
                        icon = Icons.Default.PlayArrow,
                        title = "Otomatik İndirme",
                        subtitle = "URL yapıştırıldığında otomatik indir",
                        checked = autoDownload,
                        onCheckedChange = { viewModel.setAutoDownload(it) }
                )
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }

            // Notification Settings Section
            item {
                SettingsSectionHeader(icon = Icons.Default.Notifications, title = "Bildirimler")
            }

            item {
                SettingsSwitchItem(
                        icon = Icons.Default.NotificationsActive,
                        title = "Bildirimleri Göster",
                        subtitle = "İndirme durumu bildirimleri",
                        checked = showNotifications,
                        onCheckedChange = { viewModel.setShowNotifications(it) }
                )
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }

            // Appearance Settings Section
            item { SettingsSectionHeader(icon = Icons.Default.Palette, title = "Görünüm") }

            item {
                SettingsSwitchItem(
                        icon = Icons.Default.DarkMode,
                        title = "Karanlık Mod",
                        subtitle = "Koyu tema kullan",
                        checked = darkMode,
                        onCheckedChange = { viewModel.setDarkMode(it) }
                )
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }

            // Storage Settings Section
            item { SettingsSectionHeader(icon = Icons.Default.Storage, title = "Depolama") }

            item {
                SettingsSwitchItem(
                        icon = Icons.Default.CleaningServices,
                        title = "Otomatik Önbellek Temizleme",
                        subtitle = "Uygulama kapatılınca önbelleği temizle",
                        checked = autoClearCache,
                        onCheckedChange = { viewModel.setAutoClearCache(it) }
                )
            }

            item {
                SettingsClickableItem(
                        icon = Icons.Default.DeleteSweep,
                        title = "Önbelleği Temizle",
                        subtitle = "Geçici dosyaları sil",
                        onClick = {
                            context.cacheDir.deleteRecursively()
                            Toast.makeText(context, "Önbellek temizlendi", Toast.LENGTH_SHORT)
                                    .show()
                        }
                )
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }

            // About Section
            item { SettingsSectionHeader(icon = Icons.Default.Info, title = "Hakkında") }

            item {
                SettingsClickableItem(
                        icon = Icons.Default.Info,
                        title = "Uygulama Hakkında",
                        subtitle = "MediaGrab v1.0.0",
                        onClick = { showAboutDialog = true }
                )
            }

            item {
                SettingsClickableItem(
                        icon = Icons.Default.Code,
                        title = "GitHub",
                        subtitle = "Kaynak koda göz at",
                        onClick = {
                            val intent =
                                    Intent(
                                            Intent.ACTION_VIEW,
                                            "https://github.com/beratr061/MediaGrab-Mobile".toUri()
                                    )
                            context.startActivity(intent)
                        }
                )
            }

            item {
                SettingsClickableItem(
                        icon = Icons.Default.Star,
                        title = "Değerlendir",
                        subtitle = "Play Store'da puanla",
                        onClick = {
                            Toast.makeText(context, "Yakında Play Store'da!", Toast.LENGTH_SHORT)
                                    .show()
                        }
                )
            }
        }
    }

    // Quality Selection Dialog
    if (showQualityDialog) {
        SelectionDialog(
                title = "İndirme Kalitesi",
                options = DownloadQuality.entries.map { it.displayName },
                selectedIndex = DownloadQuality.entries.indexOf(downloadQuality),
                onSelect = { index ->
                    viewModel.setDownloadQuality(DownloadQuality.entries[index])
                    showQualityDialog = false
                },
                onDismiss = { showQualityDialog = false }
        )
    }

    // Format Selection Dialog
    if (showFormatDialog) {
        SelectionDialog(
                title = "Tercih Edilen Format",
                options = VideoFormat.entries.map { it.displayName },
                selectedIndex = VideoFormat.entries.indexOf(preferredFormat),
                onSelect = { index ->
                    viewModel.setPreferredFormat(VideoFormat.entries[index])
                    showFormatDialog = false
                },
                onDismiss = { showFormatDialog = false }
        )
    }

    // About Dialog
    if (showAboutDialog) {
        AlertDialog(
                onDismissRequest = { showAboutDialog = false },
                icon = {
                    Icon(
                            Icons.Default.Download,
                            contentDescription = null,
                            tint = Primary,
                            modifier = Modifier.size(48.dp)
                    )
                },
                title = { Text("MediaGrab", fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text("Versiyon: 1.0.0")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("YouTube, TikTok, Instagram ve daha fazla platformdan video indirin.")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("yt-dlp ile güçlendirilmiştir.", fontSize = 12.sp, color = Color.Gray)
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showAboutDialog = false }) { Text("Tamam") }
                }
        )
    }
}

@Composable
fun SettingsSectionHeader(icon: ImageVector, title: String) {
    Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 8.dp)
    ) {
        Icon(icon, contentDescription = null, tint = Primary, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Primary)
    }
}

@Composable
fun SettingsClickableItem(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Surface(
            modifier =
                    Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(onClick = onClick),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                Text(subtitle, fontSize = 13.sp, color = MaterialTheme.colorScheme.outline)
            }
            Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
fun SettingsSwitchItem(
        icon: ImageVector,
        title: String,
        subtitle: String,
        checked: Boolean,
        onCheckedChange: (Boolean) -> Unit
) {
    Surface(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                Text(subtitle, fontSize = 13.sp, color = MaterialTheme.colorScheme.outline)
            }
            Switch(
                    checked = checked,
                    onCheckedChange = onCheckedChange,
                    colors =
                            SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Primary
                            )
            )
        }
    }
}

@Composable
fun SelectionDialog(
        title: String,
        options: List<String>,
        selectedIndex: Int,
        onSelect: (Int) -> Unit,
        onDismiss: () -> Unit
) {
    AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(title, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    options.forEachIndexed { index, option ->
                        Row(
                                modifier =
                                        Modifier.fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable { onSelect(index) }
                                                .background(
                                                        if (index == selectedIndex)
                                                                Primary.copy(alpha = 0.2f)
                                                        else Color.Transparent
                                                )
                                                .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                    selected = index == selectedIndex,
                                    onClick = { onSelect(index) },
                                    colors = RadioButtonDefaults.colors(selectedColor = Primary)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(option)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = onDismiss) { Text("İptal") } }
    )
}
