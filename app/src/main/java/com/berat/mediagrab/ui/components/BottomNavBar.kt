package com.berat.mediagrab.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.berat.mediagrab.ui.theme.Anthracite
import com.berat.mediagrab.ui.theme.Primary

@Suppress("unused") data class NavItem(val label: String, val icon: @Composable () -> Unit)

@Suppress("unused")
@Composable
fun BottomNavBar(selectedTab: Int, onTabSelected: (Int) -> Unit, modifier: Modifier = Modifier) {
    NavigationBar(modifier = modifier, containerColor = Anthracite) {
        NavigationBarItem(
                selected = selectedTab == 0,
                onClick = { onTabSelected(0) },
                icon = {
                    Icon(
                            Icons.Default.Home,
                            contentDescription = "Ana Sayfa",
                            modifier = Modifier.size(24.dp)
                    )
                },
                label = { Text("Ana Sayfa") },
                colors =
                        NavigationBarItemDefaults.colors(
                                selectedIconColor = Primary,
                                selectedTextColor = Primary,
                                unselectedIconColor = Color.White.copy(alpha = 0.7f),
                                unselectedTextColor = Color.White.copy(alpha = 0.7f),
                                indicatorColor = Primary.copy(alpha = 0.15f)
                        )
        )

        NavigationBarItem(
                selected = selectedTab == 1,
                onClick = { onTabSelected(1) },
                icon = {
                    Icon(
                            Icons.Default.FolderOpen,
                            contentDescription = "Dosyalarım",
                            modifier = Modifier.size(24.dp)
                    )
                },
                label = { Text("Dosyalarım") },
                colors =
                        NavigationBarItemDefaults.colors(
                                selectedIconColor = Primary,
                                selectedTextColor = Primary,
                                unselectedIconColor = Color.White.copy(alpha = 0.7f),
                                unselectedTextColor = Color.White.copy(alpha = 0.7f),
                                indicatorColor = Primary.copy(alpha = 0.15f)
                        )
        )

        NavigationBarItem(
                selected = selectedTab == 2,
                onClick = { onTabSelected(2) },
                icon = {
                    Icon(
                            Icons.AutoMirrored.Filled.HelpOutline,
                            contentDescription = "Yardım",
                            modifier = Modifier.size(24.dp)
                    )
                },
                label = { Text("Yardım") },
                colors =
                        NavigationBarItemDefaults.colors(
                                selectedIconColor = Primary,
                                selectedTextColor = Primary,
                                unselectedIconColor = Color.White.copy(alpha = 0.7f),
                                unselectedTextColor = Color.White.copy(alpha = 0.7f),
                                indicatorColor = Primary.copy(alpha = 0.15f)
                        )
        )
    }
}
