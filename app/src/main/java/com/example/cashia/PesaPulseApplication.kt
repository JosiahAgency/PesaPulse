package com.example.cashia

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresExtension
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.cashia.models.AnalyticsViewModel
import com.example.cashia.models.CalendarViewModel
import com.example.cashia.models.CategoryViewModel
import com.example.cashia.models.DashboardViewModel
import com.example.cashia.models.ExportViewModel
import com.example.cashia.models.SettingsViewModel
import com.example.cashia.models.TransactionsViewModel
import com.example.cashia.models.UpcomingViewModel
import com.example.cashia.ui.navigation.AppDestinations
import com.example.cashia.ui.navigation.IslandNavigationBar
import com.example.cashia.ui.reusedElements.HarmonizedActionFab
import com.example.cashia.ui.reusedElements.Subtitles
import com.example.cashia.ui.screens.NotificationsSettingsScreen
import com.example.cashia.ui.screens.SettingsScreen
import com.example.cashia.ui.screens.allTransactions.TransactionScreen
import com.example.cashia.ui.screens.allTransactions.UnmatchedReviewScreen
import com.example.cashia.ui.screens.analytics.AnalyticsScreen
import com.example.cashia.ui.screens.calendar.CalendarScreen
import com.example.cashia.ui.screens.category.CategoryManagementScreen
import com.example.cashia.ui.screens.dashboard.DashboardScreen
import com.example.cashia.ui.screens.export.ExportScreen
import com.example.cashia.ui.screens.upcomingTransactions.UpcomingPaymentsScreen
import com.example.cashia.ui.theme.LocalNavigationDockHeight
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
        )
    }

    val haptic = LocalHapticFeedback.current
    val sharedViewModel: TransactionsViewModel = viewModel(
        factory = TransactionsViewModel.provideFactory(context)
    )
    val dashboardViewModel: DashboardViewModel = viewModel(
        factory = DashboardViewModel.provideFactory(context)
    )
    val categoryViewModel: CategoryViewModel = viewModel(
        factory = CategoryViewModel.provideFactory(context)
    )
    val upcomingViewModel: UpcomingViewModel = viewModel(
        factory = UpcomingViewModel.provideFactory(context)
    )
    val settingsViewModel: SettingsViewModel = viewModel()
    val isDark by settingsViewModel.darkTheme.collectAsStateWithLifecycle()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val isTabDestination = navItems.any { it.route == currentRoute }

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

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(sharedViewModel) {
        sharedViewModel.syncError.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    val bgColor = MaterialTheme.colorScheme.background
    val density = LocalDensity.current
    var dockHeight by remember { mutableStateOf(102.dp) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        // ── Full-bleed content ────────────────────────────────────────────────
        CompositionLocalProvider(
            LocalNavigationDockHeight provides if (isTabDestination) dockHeight else 102.dp
        ) {
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
                        dashboardViewModel,
                        onNavigateToUpcoming = {
                            navController.navigate(AppDestinations.UpcomingPayments.route) {
                                launchSingleTop = true
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                restoreState = true
                            }
                        }
                    )
                }
                composable(AppDestinations.Transactions.route) {
                    TransactionScreen(
                        sharedViewModel,
                        categoryViewModel,
                        upcomingViewModel,
                        onNavigateToUpcoming = {
                            navController.navigate(AppDestinations.UpcomingPayments.route) {
                                launchSingleTop = true
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                restoreState = true
                            }
                        },
                        onNavigateToUnmatched = {
                            navController.navigate(AppDestinations.UnmatchedReview.route) {
                                launchSingleTop = true
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                restoreState = true
                            }
                        }
                    )
                }
                composable(AppDestinations.Calendar.route) {
                    val calendarViewModel: CalendarViewModel = viewModel(
                        factory = CalendarViewModel.provideFactory(
                            AppDatabase.getDatabase(context).transactionDao(),
                            upcomingViewModel.upcoming
                        )
                    )
                    CalendarScreen(
                        sharedViewModel,
                        categoryViewModel,
                        upcomingViewModel,
                        calendarViewModel
                    )
                }
                composable(AppDestinations.Analytics.route) {
                    val analyticsViewModel: AnalyticsViewModel = viewModel(
                        factory = AnalyticsViewModel.provideFactory(
                            AppDatabase.getDatabase(context).transactionDao(),
                        )
                    )
                    AnalyticsScreen(categoryViewModel, analyticsViewModel)
                }
                composable(AppDestinations.Settings.route) {
                    SettingsScreen(
                        isDarkTheme = isDark,
                        onToggleTheme = settingsViewModel::setDarkTheme,
                        onFirstDayOfWeekChange = settingsViewModel::setFirstDayOfWeek,
                        navController,
                    )
                }
                composable(AppDestinations.NotificationsSettings.route) {
                    NotificationsSettingsScreen(settingsViewModel)
                }
                composable(AppDestinations.Categories.route) {
                    CategoryManagementScreen(categoryViewModel)
                }
                composable(AppDestinations.Export.route) {
                    val exportViewModel: ExportViewModel = viewModel(
                        factory = ExportViewModel.provideFactory(context)
                    )
                    ExportScreen(exportViewModel, categoryViewModel)
                }
                composable(AppDestinations.UpcomingPayments.route) {
                    UpcomingPaymentsScreen(upcomingViewModel, categoryViewModel)
                }
                composable(AppDestinations.UnmatchedReview.route) {
                    UnmatchedReviewScreen(sharedViewModel, categoryViewModel)
                }
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
                    .matchParentSize()
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
                navigationIcon = {
                    if (!isTabDestination) {
                        IconButton(onClick = {
                            PesaActions.playClick(haptic)
                            navController.popBackStack()
                        }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                },
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent
                ),
                actions = {
                    val goToSettings = {
                        navController.navigate(AppDestinations.Settings.route) {
                            launchSingleTop = true
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            restoreState = true
                        }
                    }
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                    ) {
                        IconButton(onClick = {
                            PesaActions.playClick(haptic)
                            navController.navigate(AppDestinations.NotificationsSettings.route) {
                                launchSingleTop = true
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                restoreState = true
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Outlined.Notifications,
                                contentDescription = "Notifications",
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                    ) {
                        IconButton(onClick = {
                            PesaActions.playClick(haptic)
                            goToSettings()
                        }) {
                            Icon(
                                imageVector = Icons.Outlined.Settings,
                                contentDescription = "Settings",
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
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
            if (isTabDestination) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.verticalGradient(
                                0f to bgColor.copy(alpha = 0f),
                                0.4f to bgColor.copy(alpha = 0.75f),
                                1f to bgColor
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .navigationBarsPadding(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AnimatedVisibility(
                        visible = showBackToTodayFab,
                        enter = fadeIn(tween(200)),
                        exit = fadeOut(tween(150))
                    ) {
                        HarmonizedActionFab(
                            onClick = { dashboardViewModel.setDate(today) },
                            icon = Icons.Default.Today,
                            text = "Today",
                            contentDescription = "Today",
                            modifier = Modifier
                                .padding(horizontal = 24.dp)
                                .padding(bottom = 8.dp),
                            fullWidth = true
                        )
                    }

                    IslandNavigationBar(
                        haptic, navController, navItems,
                        modifier = Modifier.onSizeChanged {
                            dockHeight = with(density) { it.height.toDp() }
                        }
                    )
                }
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = if (isTabDestination) dockHeight else 16.dp)
            )
        }
    }
}
