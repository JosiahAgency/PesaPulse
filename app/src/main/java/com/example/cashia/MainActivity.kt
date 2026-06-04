package com.example.cashia

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresExtension
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.example.cashia.ui.screens.LockPlaceholder
import com.example.cashia.ui.theme.PesaPulseTheme
import com.example.cashia.utility.BiometricAuthenticator
import com.example.cashia.utility.NotificationHelper
import com.example.cashia.utility.NotificationScheduler
import com.example.cashia.utility.PesaTimeConfiguration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
        NotificationHelper.createChannels(this)
        NotificationScheduler.scheduleAll(this)
        fireWelcomeNotificationIfFirstRun()

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

        // 1. ADD all required permissions FIRST
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        // 2. FILTER out the ones that are already granted
        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        // 3. REQUEST the remaining permissions
        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.toTypedArray(),
                permissionRequestCode
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == permissionRequestCode) {
            val notifGranted = permissions.zip(grantResults.toTypedArray()).any { (perm, result) ->
                perm == Manifest.permission.POST_NOTIFICATIONS &&
                        result == PackageManager.PERMISSION_GRANTED
            }
            if (notifGranted) {
                // Re-schedule now that we have permission
                NotificationScheduler.scheduleAll(this)
            }
        }
    }

    private fun authenticateUser() {
        BiometricAuthenticator(this).promptAuthenticator(
            activity = this,
            onSuccess = { isUnlocked.value = true },
            onError = { /* Show retry UI if needed */ }
        )
    }

    // Add this private function to MainActivity
    private fun fireWelcomeNotificationIfFirstRun() {
        val prefs = getSharedPreferences("cashia_prefs", MODE_PRIVATE)
        val isFirstRun = prefs.getBoolean("first_run_notif", true)
        if (!isFirstRun) return

        prefs.edit().putBoolean("first_run_notif", false).apply()

        // Fire immediately on a background thread — confirms the whole pipeline works
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(this@MainActivity)
            val balance = db.transactionDao().getLatestBalance() ?: 0.0
            val count = db.transactionDao().getAllTransactionsOnce().size

            NotificationHelper.post(
                context = this@MainActivity,
                id = 9999,
                channelId = NotificationHelper.CH_DAILY,
                title = "👋 Cashia is ready",
                body = buildString {
                    append("Notifications are active.\n")
                    append("🏦 Current balance: ${NotificationHelper.ksh(balance)}\n")
                    append("📋 $count transactions synced")
                }
            )
        }
    }
}
