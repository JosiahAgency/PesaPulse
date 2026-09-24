package com.example.cashia.ui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState

@Composable
fun IslandNavigationBar(
    haptic: HapticFeedback,
    navController: NavController,
    paths: List<AppDestinations>,
    modifier: Modifier = Modifier
) {
    val entry by navController.currentBackStackEntryAsState()
    val currentRoute = paths.firstOrNull { screen ->
        entry?.destination?.hierarchy?.any { it.route == screen.route } == true
    }?.route

    NavigationDock(
        modifier = modifier,
        paths = paths,
        currentRoute = currentRoute,
        onSelect = { screen ->
            if (currentRoute != screen.route) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                navController.navigate(screen.route) {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        }
    )
}

@Composable
internal fun NavigationDock(
    paths: List<AppDestinations>,
    currentRoute: String?,
    onSelect: (AppDestinations) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth(),
            shape = RoundedCornerShape(26.dp),
            color = colors.surface,
            contentColor = colors.onSurface,
            border = BorderStroke(1.dp, colors.outlineVariant.copy(alpha = 0.45f)),
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .selectableGroup()
                    .padding(6.dp)
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                paths.forEach { screen ->
                    DockItem(
                        screen = screen,
                        selected = currentRoute == screen.route,
                        onClick = { onSelect(screen) },
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                }
            }
        }
    }
}

@Composable
private fun DockItem(
    screen: AppDestinations,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val background by animateColorAsState(
        targetValue = if (selected) colors.primary.copy(alpha = 0.12f) else Color.Transparent,
        animationSpec = tween(180),
        label = "tabBackground"
    )
    val tint by animateColorAsState(
        targetValue = if (selected) colors.primary else colors.onSurfaceVariant,
        animationSpec = tween(180),
        label = "tabTint"
    )
    val indicatorWidth by animateDpAsState(
        targetValue = if (selected) 18.dp else 0.dp,
        animationSpec = tween(180),
        label = "tabIndicator"
    )
    Column(
        modifier = modifier
            .heightIn(min = 66.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(background)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically)
    ) {
        Icon(
            imageVector = if (selected) screen.selectedIcon() else screen.icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = screen.label,
            color = tint,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            textAlign = TextAlign.Center
        )
        Box(
            Modifier.width(indicatorWidth).height(2.dp)
                .background(colors.primary, RoundedCornerShape(1.dp))
        )
    }
}

private fun AppDestinations.selectedIcon(): ImageVector = when (this) {
    AppDestinations.Dashboard -> Icons.Rounded.Home
    AppDestinations.Transactions -> Icons.Rounded.Receipt
    AppDestinations.Calendar -> Icons.Rounded.CalendarMonth
    AppDestinations.Analytics -> Icons.Rounded.BarChart
    else -> icon
}
