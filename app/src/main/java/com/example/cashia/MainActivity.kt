package com.example.cashia

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresExtension
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.cashia.models.TransactionsViewModel
import com.example.cashia.ui.navigation.AppDestinations
import com.example.cashia.ui.screens.AllTransactions.TransactionScreen
import com.example.cashia.ui.screens.Analytics.AnalyticsScreen
import com.example.cashia.ui.screens.CalendarScreen
import com.example.cashia.ui.screens.DashboardScreen
import com.example.cashia.ui.theme.PesaPulseTheme
import com.example.cashia.ui.theme.primaryGradient

class MainActivity : ComponentActivity() {

    private val PERMISSIONS_REQUEST_CODE = 100
    private val permissionsToRequest = arrayOf(
        Manifest.permission.READ_SMS,
    )

    @RequiresExtension(extension = Build.VERSION_CODES.S, version = 13)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (permissionsToRequest.any {
                ContextCompat.checkSelfPermission(
                    this,
                    it
                ) != PackageManager.PERMISSION_GRANTED
            }) {
            ActivityCompat.requestPermissions(
                this, permissionsToRequest, PERMISSIONS_REQUEST_CODE
            )
        }
        enableEdgeToEdge()
        setContent {
            var isDarkTheme by remember { mutableStateOf(true) }

            PesaPulseTheme(darkTheme = isDarkTheme) {
                PesaPulseApp(
                    isDarkTheme = isDarkTheme,
                    onToggleTheme = { isDarkTheme = !isDarkTheme }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@RequiresExtension(extension = Build.VERSION_CODES.S, version = 13)
@Composable
fun PesaPulseApp(
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val paths = listOf(
        AppDestinations.Dashboard,
        AppDestinations.Calendar,
        AppDestinations.Analytics,
        AppDestinations.Transactions,
        )
    val sharedViewModel: TransactionsViewModel = viewModel(
        factory = TransactionsViewModel.provideFactory(context)
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        BackgroundGlow(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
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
                            Text(
                                "PESAPULSE",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 4.sp,
                                    brush = primaryGradient(),
                                )
                            )
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent,
                        ),
                        actions = {
                            IconButton(onClick = onToggleTheme) {
                                Icon(
                                    imageVector = if (isDarkTheme) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                                    contentDescription = "Theme",
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    )
                }
            },
            bottomBar = {
                GlassNavigationBar(navController, paths)
            }) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = AppDestinations.Dashboard.route,
                modifier = Modifier.padding(innerPadding)

            ) {
                composable(AppDestinations.Dashboard.route) {
                    DashboardScreen(viewModel = sharedViewModel)
                }
                composable(AppDestinations.Calendar.route) {
                    CalendarScreen(viewModel = sharedViewModel)
                }
                composable(AppDestinations.Analytics.route) {
                    AnalyticsScreen()
                }
                composable(AppDestinations.Transactions.route) {
                    TransactionScreen(
                        viewModel = sharedViewModel,
                        onNavigateBack = { navController.popBackStack() })
                }
            }
        }
    }
}

@Composable
fun BackgroundGlow(color: Color) {
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer(renderEffect = null)
    ) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(color, Color.Transparent),
                center = Offset(size.width * 0.2f, size.height * 0.15f),
                radius = size.width * 0.8f
            ),
            alpha = 0.4f
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(color, Color.Transparent),
                center = Offset(size.width * 0.8f, size.height * 0.9f),
                radius = size.width * 0.6f
            ),
            alpha = 0.3f
        )
    }
}

@Composable
fun GlassNavigationBar(
    navController: NavController,
    paths: List<AppDestinations>
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val colorScheme = MaterialTheme.colorScheme

    Surface(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 20.dp)
            .fillMaxWidth()
            .height(72.dp),
        shape = RoundedCornerShape(28.dp),
        color = colorScheme.surface.copy(
            alpha = if (colorScheme.background.luminance() > 0.5f) 0.92f else 0.78f
        ),
        border = BorderStroke(
            0.6.dp,
            colorScheme.onSurface.copy(alpha = 0.12f)
        ),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            paths.forEach { screen ->
                val selected = currentRoute == screen.route
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = screen.icon,
                            contentDescription = screen.label,
                            modifier = Modifier.size(24.dp),
                            tint = if (selected)
                                colorScheme.primary
                            else
                                colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = screen.label,
                            fontSize = 11.sp,
                            maxLines = 1,
                            fontWeight = FontWeight.SemiBold,
                            color = if (selected)
                                Color.Unspecified
                            else
                                colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.labelSmall.copy(
                                brush = if (selected) primaryGradient() else null,
                            )
                        )
                    }
                }
            }
        }
    }
}

