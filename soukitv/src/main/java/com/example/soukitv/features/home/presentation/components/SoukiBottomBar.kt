package com.example.soukitv.features.home.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.soukitv.R
import com.example.soukitv.features.home.presentation.NavigationTab
import com.example.soukitv.ui.theme.SoukiDarkBackground
import com.example.soukitv.ui.theme.SoukiDarkBorder
import com.example.soukitv.ui.theme.SoukiRed

private val InactiveGrey = Color(0xFF71717A)

@Composable
fun SoukiBottomBar(
    selectedTab: NavigationTab,
    onTabSelected: (NavigationTab) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().background(SoukiDarkBackground)) {
        HorizontalDivider(thickness = 0.5.dp, color = SoukiDarkBorder)
        NavigationBar(
            containerColor = SoukiDarkBackground,
            tonalElevation = 0.dp
        ) {
            BottomTab(
                selected = selectedTab == NavigationTab.MATCHES,
                icon = Icons.Filled.SportsSoccer,
                label = stringResource(R.string.tab_matches),
                onClick = { onTabSelected(NavigationTab.MATCHES) }
            )
            BottomTab(
                selected = selectedTab == NavigationTab.CHANNELS,
                icon = Icons.Filled.Tv,
                label = stringResource(R.string.tab_channels),
                onClick = { onTabSelected(NavigationTab.CHANNELS) }
            )
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.BottomTab(
    selected: Boolean,
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        icon = { Icon(imageVector = icon, contentDescription = label) },
        label = {
            Text(
                text = label,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            )
        },
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = Color.White,
            selectedTextColor = Color.White,
            indicatorColor = SoukiRed,
            unselectedIconColor = InactiveGrey,
            unselectedTextColor = InactiveGrey
        )
    )
}

