package com.example.cashia

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresExtension
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.cashia.models.CategoryViewModel
import com.example.cashia.models.DashboardViewModel
import com.example.cashia.models.SettingsViewModel
import com.example.cashia.models.TransactionsViewModel
import com.example.cashia.models.UpcomingViewModel
import com.example.cashia.ui.navigation.AppDestinations
import com.example.cashia.ui.reusedElements.Subtitles
import com.example.cashia.ui.screens.SettingsScreen
import com.example.cashia.ui.screens.allTransactions.TransactionScreen
import com.example.cashia.ui.screens.analytics.AnalyticsScreen
import com.example.cashia.ui.screens.calendar.CalendarScreen
import com.example.cashia.ui.screens.category.CategoryManagementScreen
import com.example.cashia.ui.screens.dashboard.DashboardScreen
import com.example.cashia.ui.screens.projections.ProjectionScreen
import com.example.cashia.ui.theme.primaryGradient
import com.example.cashia.utility.PesaActions
import com.example.cashia.utility.PesaTimeConfiguration
import java.time.LocalDate

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
@OptIn(ExperimentalMaterial3Api::class)
@RequiresExtension(extension = Build.VERSION_CODES.S, version = 13)
@Composable
fun PesaPulseApp() {
    val context = LocalContext.current
    val navController = rememberNavController()
    val navItems = remember {
        listOf(
            AppDestinations.Dashboard,
            AppDestinations.Transactions,
            AppDestinations.Calendar,
            AppDestinations.Analytics,
            AppDestinations.Projections,
        )
    }

    val haptic = LocalHapticFeedback.current
    val dashboardViewModel: DashboardViewModel = viewModel(
        factory = DashboardViewModel.provideFactory(context)
    )
    val sharedViewModel: TransactionsViewModel = viewModel(
        factory = TransactionsViewModel.provideFactory(context)
    )
    val categoryViewModel: CategoryViewModel = viewModel(
        factory = CategoryViewModel.provideFactory(context)
    )
    val upcomingViewModel: UpcomingViewModel = viewModel(
        factory = UpcomingViewModel.provideFactory(context)
    )
    val settingsViewModel: SettingsViewModel = viewModel()
    val isDark by settingsViewModel.darkTheme.collectAsState()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Dashboard FAB state — lives here since there's no Scaffold in DashboardScreen
    val today = remember { LocalDate.now() }
    val dashboardSelectedDate by dashboardViewModel.selectedDate.collectAsStateWithLifecycle()
    val showBackToTodayFab = currentRoute == AppDestinations.Dashboard.route
            && dashboardSelectedDate != today

    LaunchedEffect(sharedViewModel, upcomingViewModel) {
        sharedViewModel.newTransactionsSynced.collect { newTxs ->
            newTxs.forEach { upcomingViewModel.tryAutoMatch(it) }
        }
    }

    val bgColor = MaterialTheme.colorScheme.background

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        // ── Full-bleed content ────────────────────────────────────────────────
        NavHost(
            navController = navController,
            startDestination = AppDestinations.Dashboard.route,
            enterTransition = { fadeIn(tween(0)) },
            exitTransition = { fadeOut(tween(0)) },
            popEnterTransition = { fadeIn(tween(0)) },
            popExitTransition = { fadeOut(tween(0)) },
            modifier = Modifier.fillMaxSize()
        ) {
            composable(AppDestinations.Dashboard.route) {
                DashboardScreen(
                    sharedViewModel,
                    categoryViewModel,
                    upcomingViewModel,
                    dashboardViewModel
                )
            }
            composable(AppDestinations.Transactions.route) {
                TransactionScreen(sharedViewModel, categoryViewModel, upcomingViewModel)
            }
            composable(AppDestinations.Calendar.route) {
                CalendarScreen(sharedViewModel, categoryViewModel, upcomingViewModel)
            }
            composable(AppDestinations.Analytics.route) {
                AnalyticsScreen(categoryViewModel, sharedViewModel)
            }
            composable(AppDestinations.Projections.route) {
                ProjectionScreen(sharedViewModel, upcomingViewModel, categoryViewModel)
            }
            composable(AppDestinations.Settings.route) {
                SettingsScreen(
                    isDarkTheme = isDark,
                    onToggleTheme = settingsViewModel::setDarkTheme,
                    onFirstDayOfWeekChange = settingsViewModel::setFirstDayOfWeek,
                    navController,
                )
            }
            composable(AppDestinations.Categories.route) {
                CategoryManagementScreen(categoryViewModel)
            }
        }

        // ── Top scrim + bar ───────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .background(
                        Brush.verticalGradient(
                            0f to bgColor,
                            0.7f to bgColor.copy(alpha = 0.85f),
                            1f to bgColor.copy(alpha = 0f)
                        )
                    )
            )
            TopAppBar(
                modifier = Modifier.statusBarsPadding(),
                title = {
                    Column {
                        AnimatedContent(
                            targetState = PesaTimeConfiguration.screenTitle(currentRoute),
                            transitionSpec = {
                                (fadeIn(tween(180)) + scaleIn(tween(180), 0.97f))
                                    .togetherWith(fadeOut(tween(120)) + scaleOut(tween(120), 0.97f))
                            },
                            label = "title"
                        ) { title ->
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    brush = primaryGradient(),
                                )
                            )
                        }
                        AnimatedContent(
                            targetState = PesaTimeConfiguration.screenSubtitle(currentRoute),
                            transitionSpec = {
                                fadeIn(tween(150)).togetherWith(fadeOut(tween(100)))
                            },
                            label = "subtitle"
                        ) { subtitle ->
                            if (subtitle.isNotEmpty()) Subtitles(text = subtitle)
                        }
                    }
                },
                windowInsets = WindowInsets(0),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent
                ),
                actions = {
                    IconButton(onClick = {
                        PesaActions.playClick(haptic)
                        navController.navigate(AppDestinations.Settings.route) {
                            launchSingleTop = true
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            restoreState = true
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            )
        }

        // ── Bottom scrim + island nav + FAB ───────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .background(
                        Brush.verticalGradient(
                            0f to bgColor.copy(alpha = 0f),
                            0.4f to bgColor.copy(alpha = 0.75f),
                            1f to bgColor
                        )
                    )
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // FAB sits just above the island, fades in/out cleanly
                AnimatedVisibility(
                    visible = showBackToTodayFab,
                    enter = fadeIn(tween(200)),
                    exit = fadeOut(tween(150))
                ) {
                    ExtendedFloatingActionButton(
                        onClick = { dashboardViewModel.setDate(today) },
                        icon = { Icon(Icons.Default.Today, contentDescription = "Today") },
                        text = { Text("Back to Today", fontWeight = FontWeight.SemiBold) },
                        containerColor = Color.Transparent,
                        elevation = FloatingActionButtonDefaults.elevation(0.dp),
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier
                            .padding(bottom = 8.dp)
                            .clip(CircleShape)
                            .background(brush = primaryGradient())
                    )
                }

                IslandNavigationBar(haptic, navController, navItems)
            }
        }
    }
}

@Composable
fun IslandNavigationBar(
    haptic: HapticFeedback,
    navController: NavController,
    paths: List<AppDestinations>
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val colorScheme = MaterialTheme.colorScheme

    Surface(
        modifier = Modifier
            .padding(24.dp, 16.dp)
            .navigationBarsPadding()
            .fillMaxWidth()
            .height(64.dp),
        shape = RoundedCornerShape(32.dp),
        color = colorScheme.surface,
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            paths.forEach { screen ->
                val selected = currentRoute == screen.route
                IslandNavItem(
                    screen = screen,
                    selected = selected,
                    onClick = {
                        if (currentRoute != screen.route) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun RowScope.IslandNavItem(
    screen: AppDestinations,
    selected: Boolean,
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    // Press feedback
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.82f else 1f,
        animationSpec = spring(
            stiffness = Spring.StiffnessHigh,       // snappy down
            dampingRatio = Spring.DampingRatioNoBouncy
        ),
        label = "scale"
    )

    // Icon + label tint — pure alpha, zero layout impact
    val contentAlpha by animateFloatAsState(
        targetValue = if (selected) 1f else 0.4f,
        animationSpec = tween(120),
        label = "alpha"
    )

    // Indicator width — spring feels physical
    val indicatorWidth by animateDpAsState(
        targetValue = if (selected) 18.dp else 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "indicator"
    )

    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .scale(scale)
            .pointerInput(onClick) {
                detectTapGestures(
                    onPress = {
                        pressed = true
                        tryAwaitRelease()
                        pressed = false
                    },
                    onTap = { onClick() }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(vertical = 6.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(indicatorWidth)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(brush = primaryGradient())
            )

            Spacer(Modifier.height(4.dp))

            Icon(
                imageVector = screen.icon,
                contentDescription = screen.label,
                modifier = Modifier.size(22.dp),
                // Animate tint alpha directly — no recompose, no layout change
                tint = if (selected) colorScheme.primary
                else colorScheme.onSurfaceVariant.copy(alpha = contentAlpha),
            )

            Spacer(Modifier.height(2.dp))

            Text(
                text = screen.label,
                fontSize = 10.sp,
                maxLines = 1,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.labelSmall.copy(
                    brush = primaryGradient()
                ),
                modifier = Modifier.graphicsLayer { alpha = contentAlpha }
            )
        }
    }
}