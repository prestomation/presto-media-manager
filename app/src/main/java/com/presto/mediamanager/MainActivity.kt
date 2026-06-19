package com.presto.mediamanager

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.presto.mediamanager.ui.nav.AppNav
import com.presto.mediamanager.ui.theme.PrestoTheme
import com.presto.mediamanager.work.WorkScheduler

class MainActivity : ComponentActivity() {

    private val requestNotifications =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* badge best-effort */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        maybeRequestNotificationPermission()
        setContent {
            PrestoTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNav()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        WorkScheduler.scanNow(this)
    }

    /** Only prompt when the permission isn't already granted, so an already-granted
     *  install (and instrumentation tests) never see a foreground dialog. */
    private fun maybeRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
        if (!granted) requestNotifications.launch(android.Manifest.permission.POST_NOTIFICATIONS)
    }
}
