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
import com.example.cashia.ui.screens.LockPlaceholder
import com.example.cashia.ui.theme.PesaPulseTheme
import com.example.cashia.utility.BiometricAuthenticator
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
