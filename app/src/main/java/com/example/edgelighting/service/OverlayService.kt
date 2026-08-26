package com.example.edgelighting.service

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.example.edgelighting.EdgeLightingApp
import com.example.edgelighting.R
import com.example.edgelighting.ui.EdgeLightingView

/**
 * OverlayService:
 * Creates a transparent system-wide overlay window using TYPE_APPLICATION_OVERLAY.
 * Manages view attachment, hardware acceleration, and auto-dismiss timeout.
 */
class OverlayService : Service() {

    companion object {
        const val EXTRA_PRIMARY_COLOR = "extra_primary_color"
        const val EXTRA_SECONDARY_COLOR = "extra_secondary_color"
        const val EXTRA_DURATION_MS = "extra_duration_ms"
        const val EXTRA_THICKNESS = "extra_thickness"
        const val EXTRA_SPEED = "extra_speed"
        const val EXTRA_ANIM_STYLE = "extra_anim_style"
        const val EXTRA_PACKAGE_NAME = "extra_pkg_name"
        private const val NOTIFICATION_ID = 9921
    }

    private var windowManager: WindowManager? = null
    private var edgeLightingView: EdgeLightingView? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var dismissRunnable: Runnable? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notification: Notification = NotificationCompat.Builder(this, EdgeLightingApp.CHANNEL_ID)
                .setContentTitle("Edge Lighting Active")
                .setContentText("Displaying notification border illumination")
                .setSmallIcon(R.drawable.ic_notification)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .build()
            startForeground(NOTIFICATION_ID, notification)
        }

        val primaryColor = intent?.getIntExtra(EXTRA_PRIMARY_COLOR, 0xFF00FF88.toInt()) ?: 0xFF00FF88.toInt()
        val secondaryColor = intent?.getIntExtra(EXTRA_SECONDARY_COLOR, 0xFF00F0FF.toInt()) ?: 0xFF00F0FF.toInt()
        val durationMs = intent?.getLongExtra(EXTRA_DURATION_MS, 3500L) ?: 3500L
        val thickness = intent?.getFloatExtra(EXTRA_THICKNESS, 6f) ?: 6f
        val speed = intent?.getFloatExtra(EXTRA_SPEED, 1.2f) ?: 1.2f
        val animStyle = intent?.getStringExtra(EXTRA_ANIM_STYLE) ?: "laser_comet"

        showLightingOverlay(primaryColor, secondaryColor, durationMs, thickness, speed, animStyle)
        return START_NOT_STICKY
    }

    private fun showLightingOverlay(
        primaryColor: Int,
        secondaryColor: Int,
        durationMs: Long,
        thickness: Float,
        speed: Float,
        animStyle: String
    ) {
        // Cancel existing dismiss timer if active
        dismissRunnable?.let { mainHandler.removeCallbacks(it) }

        if (edgeLightingView == null) {
            edgeLightingView = EdgeLightingView(this).apply {
                setConfig(primaryColor, secondaryColor, thickness, speed, animStyle)
            }

            val layoutFlags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED

            val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                type,
                layoutFlags,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                }
            }

            try {
                windowManager?.addView(edgeLightingView, params)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            // Update existing view parameters and reset animation
            edgeLightingView?.setConfig(primaryColor, secondaryColor, thickness, speed, animStyle)
        }

        // Schedule auto dismiss
        dismissRunnable = Runnable {
            removeOverlayAndStop()
        }
        mainHandler.postDelayed(dismissRunnable!!, durationMs)
    }

    private fun removeOverlayAndStop() {
        try {
            edgeLightingView?.let {
                it.stopAnimation()
                windowManager?.removeView(it)
                edgeLightingView = null
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    override fun onDestroy() {
        dismissRunnable?.let { mainHandler.removeCallbacks(it) }
        removeOverlayAndStop()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
