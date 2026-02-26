package com.example.cashia

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresExtension
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.cashia.Utility.BiometricAuthenticator
import com.example.cashia.Utility.PesaActions
import com.example.cashia.Utility.PesaTimeConfiguration
import com.example.cashia.models.SettingsViewModel
import com.example.cashia.models.TransactionsViewModel
import com.example.cashia.ui.navigation.AppDestinations
import com.example.cashia.ui.screens.AllTransactions.TransactionScreen
import com.example.cashia.ui.screens.Analytics.AnalyticsScreen
import com.example.cashia.ui.screens.CalendarScreen
import com.example.cashia.ui.screens.Dashboard.DashboardScreen
import com.example.cashia.ui.screens.ProjectionScreen
import com.example.cashia.ui.screens.SettingsScreen
import com.example.cashia.ui.theme.PesaPulseTheme
import com.example.cashia.ui.theme.primaryGradient

class MainActivity : FragmentActivity() {
	   
	   private val permissionRequestCode = 100
	   private val permissionsToRequest = arrayOf(Manifest.permission.READ_SMS)
	   private var isUnlocked = mutableStateOf(false)
	   
	   @RequiresExtension(extension = Build.VERSION_CODES.S, version = 13)
	   override fun onCreate(savedInstanceState: Bundle?) {
			installSplashScreen()
			super.onCreate(savedInstanceState)
			
			if (permissionsToRequest.any {
						ContextCompat.checkSelfPermission(
							   this,
							   it
						) != PackageManager.PERMISSION_GRANTED
				   }) {
				   ActivityCompat.requestPermissions(
						this,
						permissionsToRequest,
						permissionRequestCode
				   )
			}
			PesaTimeConfiguration.initialize(this)
			
			if (!PesaTimeConfiguration.isBiometricEnabled) {
				   isUnlocked.value = true
			} else {
				   authenticateUser()
			}
			enableEdgeToEdge()
			setContent {
				   PesaPulseTheme(darkTheme = PesaTimeConfiguration.isDarkTheme) {
						if (isUnlocked.value) {
							   PesaPulseApp()
						} else {
							   LockPlaceholder()
						}
				   }
			}
	   }
	   
	   private fun authenticateUser() {
			BiometricAuthenticator(this).promptAuthenticator(
				   activity = this,
				   onSuccess = { isUnlocked.value = true },
				   onError = { error ->
						// Handle error (e.g., show a "Retry" button)
				   })
	   }
	   
}

@Composable
fun LockPlaceholder() {
	   Box(
			Modifier
                      .fillMaxSize()
                      .background(MaterialTheme.colorScheme.background),
			contentAlignment = Alignment.Center
	   ) {
			Text(
				   "PesaPulse is Locked", style = MaterialTheme.typography.titleLarge.copy(
						fontWeight = FontWeight.Black,
						brush = primaryGradient(),
				   )
			)
	   }
}

private fun screenSubtitle(route: String?): String = when (route) {
	   AppDestinations.Dashboard.route -> "Your financial pulse, today"
	   AppDestinations.Transactions.route -> "Full transaction history"
	   AppDestinations.Calendar.route -> "Activity across the month"
	   AppDestinations.Analytics.route -> "Breakdown by period & category"
	   AppDestinations.Projections.route -> "30-day balance forecast"
	   else -> ""
}

private fun screenTitle(route: String?): String = when (route) {
	   AppDestinations.Dashboard.route -> "Home"
	   AppDestinations.Transactions.route -> "History"
	   AppDestinations.Calendar.route -> "Calendar"
	   AppDestinations.Analytics.route -> "Insights"
	   AppDestinations.Projections.route -> "Forecast"
	   else -> "PesaPulse"
}

@OptIn(ExperimentalMaterial3Api::class)
@RequiresExtension(extension = Build.VERSION_CODES.S, version = 13)
@Composable
fun PesaPulseApp() {
	   val context = LocalContext.current
	   val navController = rememberNavController()
	   
	   val navItems = listOf(
			AppDestinations.Dashboard,
			AppDestinations.Transactions,
			AppDestinations.Calendar,
			AppDestinations.Analytics,
			AppDestinations.Projections,
			AppDestinations.Category
	   )
	   
	   val haptic = LocalHapticFeedback.current
	   val sharedViewModel: TransactionsViewModel = viewModel(
			factory = TransactionsViewModel.provideFactory(context)
	   )
	   val settingsViewModel: SettingsViewModel = viewModel()
	   val isDark by settingsViewModel.darkTheme.collectAsState()
	   
	   val navBackStackEntry by navController.currentBackStackEntryAsState()
	   val currentRoute = navBackStackEntry?.destination?.route
	   
	   Box(
			modifier = Modifier
                      .fillMaxSize()
                      .background(MaterialTheme.colorScheme.background)
	   ) {
			BackgroundGlow(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
			
			Scaffold(containerColor = Color.Transparent, topBar = {
				   // Contextual top bar: title changes per screen with subtitle explaining what the screen does
				   Box(
						modifier = Modifier
                                        .fillMaxWidth()
                                        .statusBarsPadding()
				   ) {
						TopAppBar(
							   title = {
									Column {
										   AnimatedContent(
												targetState = screenTitle(
													   currentRoute
												),
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
												targetState = screenSubtitle(
													   currentRoute
												),
												transitionSpec = { fadeIn() togetherWith fadeOut() },
												label = "subtitle"
										   ) { subtitle ->
												if (subtitle.isNotEmpty()) {
													   Text(
															text = subtitle,
															style = MaterialTheme.typography.labelSmall,
															color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
																   alpha = 0.6f
															)
													   )
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
										   navController.navigate(
												AppDestinations.Settings.route
										   ) {
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
							   })
				   }
			}, bottomBar = {
				   // Only show bottom nav on main screens, not settings
				   if (currentRoute != AppDestinations.Settings.route) {
						GlassNavigationBar(haptic, navController, navItems)
				   }
			}) { innerPadding ->
				   NavHost(
						navController = navController,
						startDestination = AppDestinations.Dashboard.route,
						modifier = Modifier.padding(innerPadding)
				   ) {
						composable(AppDestinations.Dashboard.route) {
							   DashboardScreen(viewModel = sharedViewModel)
						}
						composable(AppDestinations.Transactions.route) {
							   TransactionScreen(
									viewModel = sharedViewModel,
									onNavigateBack = { navController.popBackStack() })
						}
						composable(AppDestinations.Calendar.route) {
							   CalendarScreen(viewModel = sharedViewModel)
						}
						composable(AppDestinations.Analytics.route) {
							   AnalyticsScreen()
						}
						composable(AppDestinations.Projections.route) {
							   ProjectionScreen()
						}
						composable(AppDestinations.Settings.route) {
							   SettingsScreen(
									isDarkTheme = isDark,
									onToggleTheme = settingsViewModel::setDarkTheme,
									onFirstDayOfWeekChange = settingsViewModel::setFirstDayOfWeek
							   )
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
				   ), alpha = 0.4f
			)
			drawCircle(
				   brush = Brush.radialGradient(
						colors = listOf(color, Color.Transparent),
						center = Offset(size.width * 0.8f, size.height * 0.9f),
						radius = size.width * 0.6f
				   ), alpha = 0.3f
			)
	   }
}

@Composable
fun GlassNavigationBar(
	   haptic: HapticFeedback, navController: NavController, paths: List<AppDestinations>
) {
	   val navBackStackEntry by navController.currentBackStackEntryAsState()
	   val currentRoute = navBackStackEntry?.destination?.route
	   val colorScheme = MaterialTheme.colorScheme
	   
	   Surface(
			modifier = Modifier
                      .padding(horizontal = 16.dp, vertical = 20.dp)
                      .navigationBarsPadding()
                      .fillMaxWidth()
                      .height(72.dp),
			shape = RoundedCornerShape(28.dp),
			color = colorScheme.surface.copy(
				   alpha = if (colorScheme.background.luminance() > 0.5f) 0.92f else 0.78f
			),
			border = BorderStroke(0.6.dp, colorScheme.onSurface.copy(alpha = 0.12f)),
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
                                                     if (currentRoute != screen.route) {
                                                         haptic.performHapticFeedback(
                                                             HapticFeedbackType.TextHandleMove
                                                         )
                                                         navController.navigate(
                                                             screen.route
                                                         ) {
                                                             popUpTo(navController.graph.startDestinationId) {
                                                                 saveState = true
                                                             }
                                                             launchSingleTop = true
                                                             restoreState = true
                                                         }
                                                     }
                                                 }, contentAlignment = Alignment.Center
						) {
							   Column(
									horizontalAlignment = Alignment.CenterHorizontally,
									verticalArrangement = Arrangement.Center
							   ) {
									// Selected indicator pill
									if (selected) {
										   Box(
												modifier = Modifier
                                                                            .width(24.dp)
                                                                            .height(3.dp)
                                                                            .clip(
                                                                                RoundedCornerShape(
                                                                                    2.dp
                                                                                )
                                                                            )
                                                                            .background(brush = primaryGradient())
										   )
										   Spacer(Modifier.height(4.dp))
									} else {
										   Spacer(Modifier.height(7.dp))
									}
									
									Icon(
										   imageVector = screen.icon,
										   contentDescription = screen.label,
										   modifier = Modifier.size(22.dp),
										   tint = if (selected) colorScheme.primary
										   else colorScheme.onSurfaceVariant.copy(
												alpha = 0.55f
										   ),
									)
									Spacer(modifier = Modifier.height(3.dp))
									Text(
										   text = screen.label,
										   fontSize = 10.sp,
										   maxLines = 1,
										   fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
										   color = if (selected) Color.Unspecified
										   else colorScheme.onSurfaceVariant.copy(
												alpha = 0.55f
										   ),
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