package com.example.cashia

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresExtension
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.cashia.models.CategoryViewModel
import com.example.cashia.models.DailyDigestWorker
import com.example.cashia.models.NotificationHelper
import com.example.cashia.models.SettingsViewModel
import com.example.cashia.models.TransactionsViewModel
import com.example.cashia.models.UpcomingViewModel
import com.example.cashia.ui.calendar.CalendarScreen
import com.example.cashia.ui.navigation.AppDestinations
import com.example.cashia.ui.reusedElements.BackgroundGlow
import com.example.cashia.ui.reusedElements.GlassNavigationBar
import com.example.cashia.ui.reusedElements.Subtitles
import com.example.cashia.ui.screens.LockPlaceholder
import com.example.cashia.ui.screens.SettingsScreen
import com.example.cashia.ui.screens.allTransactions.TransactionScreen
import com.example.cashia.ui.screens.analytics.AnalyticsScreen
import com.example.cashia.ui.screens.category.CategoryManagementScreen
import com.example.cashia.ui.screens.dashboard.DashboardScreen
import com.example.cashia.ui.screens.projections.ProjectionScreen
import com.example.cashia.ui.theme.PesaPulseTheme
import com.example.cashia.ui.theme.primaryGradient
import com.example.cashia.utility.BiometricAuthenticator
import com.example.cashia.utility.PesaActions
import com.example.cashia.utility.PesaTimeConfiguration

class MainActivity : FragmentActivity() {
    private val permissionRequestCode = 100
    private var isUnlocked = mutableStateOf(false)

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @RequiresExtension(extension = Build.VERSION_CODES.S, version = 13)
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        handlePermissions()
        PesaTimeConfiguration.initialize(this)
        NotificationHelper.createNotificationChannel(this)
        DailyDigestWorker.schedule(this)

        if (!PesaTimeConfiguration.isBiometricEnabled) {
            isUnlocked.value = true
        } else {
            authenticateUser()
        }

        enableEdgeToEdge()
        setContent {
            val unlocked by remember { isUnlocked }
            PesaPulseTheme(darkTheme = PesaTimeConfiguration.isDarkTheme) {
                if (unlocked) {
                    PesaPulseApp()
                } else {
                    LockPlaceholder(onUnlockClick = { authenticateUser() })
                }
            }
        }
    }

    private fun handlePermissions() {
        val permissions = mutableListOf(Manifest.permission.READ_SMS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.toTypedArray(),
                permissionRequestCode
            )
        }
    }

    private fun authenticateUser() {
        BiometricAuthenticator(this).promptAuthenticator(
            activity = this,
            onSuccess = { isUnlocked.value = true },
            onError = { /* Show retry UI if needed */ }
        )
    }
}

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

    LaunchedEffect(Unit) {
        sharedViewModel.onNewTransactionsSynced = { newTxs ->
            newTxs.forEach { upcomingViewModel.tryAutoMatch(it) }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        BackgroundGlow(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
//        AnimatedMeshBg()
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                ) {
                    TopAppBar(
                        title = {
                            Column {
                                AnimatedContent(
                                    targetState = PesaTimeConfiguration.screenTitle(currentRoute),
                                    transitionSpec = { fadeIn() togetherWith fadeOut() },
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
                                    targetState = PesaTimeConfiguration.screenSubtitle
                                        (currentRoute),
                                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                                    label = "subtitle"
                                ) { subtitle ->
                                    if (subtitle.isNotEmpty()) {
                                        Subtitles(text = subtitle)
                                    }
                                }
                            }
                        },
                        windowInsets = WindowInsets.statusBars,
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent
                        ),
                        actions = {
                            IconButton(onClick = {
                                PesaActions.playClick(haptic)
                                navController.navigate(AppDestinations.Settings.route) {
                                    launchSingleTop = true
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = true
                                    }
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
            },
            bottomBar = {
                if (currentRoute != AppDestinations.Settings.route &&
                    currentRoute != AppDestinations.Categories.route
                ) {
                    GlassNavigationBar(haptic, navController, navItems)
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = AppDestinations.Dashboard.route,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(AppDestinations.Dashboard.route) {
                    DashboardScreen(viewModel = sharedViewModel, categoryViewModel)
                }
                composable(AppDestinations.Transactions.route) {
                    TransactionScreen(viewModel = sharedViewModel, categoryViewModel)
                }
                composable(AppDestinations.Calendar.route) {
                    CalendarScreen(
                        viewModel = sharedViewModel,
                        categoryViewModel,
                        upcomingViewModel
                    )
                }
                composable(AppDestinations.Analytics.route) {
                    AnalyticsScreen(categoryViewModel, sharedViewModel)
                }
                composable(AppDestinations.Projections.route) {
                    ProjectionScreen(
                        sharedViewModel,
                        upcomingViewModel,
                        categoryViewModel
                    )
                }
                composable(AppDestinations.Settings.route) {
                    SettingsScreen(
                        isDarkTheme = isDark,
                        onToggleTheme = settingsViewModel::setDarkTheme,
                        onFirstDayOfWeekChange = settingsViewModel::setFirstDayOfWeek,
                        navController
                    )
                }
                composable(AppDestinations.Categories.route) {
                    CategoryManagementScreen(categoryViewModel)
                }
            }
        }
    }
}