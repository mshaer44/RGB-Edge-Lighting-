package com.example.edgelighting.service

import android.content.Intent
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.edgelighting.data.SettingsStore
import com.example.edgelighting.util.ColorExtractor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * EdgeNotificationListener:
 * Intercepts incoming notifications with zero battery polling overhead.
 * Uses AndroidX Palette to dynamically extract app identity color or applies user customized rules.
 */
class EdgeNotificationListener : NotificationListenerService() {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val TAG = "EdgeNotificationListener"

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val pkg = sbn.packageName
        // Prevent recursive loop on self notifications
        if (pkg == this.packageName) return

        // Skip ongoing persistent notifications (e.g. download progress, media playback)
        if (sbn.isOngoing) return

        scope.launch {
            try {
                val settings = SettingsStore(applicationContext)
                val isEnabled = settings.isMainEnabledFlow.first()
                if (!isEnabled) return@launch

                // 1. Check if user configured a custom color rule for this specific package
                val customRule = settings.getAppRule(pkg)
                val primaryColor: Int
                val secondaryColor: Int
                val animStyle: String

                if (customRule != null && customRule.enabled) {
                    primaryColor = customRule.primaryColor
                    secondaryColor = customRule.secondaryColor
                    animStyle = customRule.style
                } else {
                    // 2. Dynamically extract vibrant primary & secondary color from App Icon using Palette
                    val paletteColors = ColorExtractor.extractColors(applicationContext, pkg)
                    primaryColor = paletteColors.first
                    secondaryColor = paletteColors.second
                    animStyle = settings.defaultStyleFlow.first()
                }

                val durationMs = settings.durationFlow.first()
                val thickness = settings.thicknessFlow.first()
                val speed = settings.speedFlow.first()

                Log.d(TAG, "Triggering Edge Lighting for package: $pkg with color #${Integer.toHexString(primaryColor)}")

                // 3. Launch OverlayService to render hardware-accelerated Canvas edge lighting
                val intent = Intent(applicationContext, OverlayService::class.java).apply {
                    putExtra(OverlayService.EXTRA_PRIMARY_COLOR, primaryColor)
                    putExtra(OverlayService.EXTRA_SECONDARY_COLOR, secondaryColor)
                    putExtra(OverlayService.EXTRA_DURATION_MS, durationMs)
                    putExtra(OverlayService.EXTRA_THICKNESS, thickness)
                    putExtra(OverlayService.EXTRA_SPEED, speed)
                    putExtra(OverlayService.EXTRA_ANIM_STYLE, animStyle)
                    putExtra(OverlayService.EXTRA_PACKAGE_NAME, pkg)
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(intent)
                } else {
                    startService(intent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling notification", e)
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.i(TAG, "Edge Lighting Notification Listener connected and active.")
    }
}
