package com.example.edgelighting.util

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.palette.graphics.Palette

object ColorExtractor {

    /**
     * Extracts a Pair of (Primary, Secondary) vibrant colors from an application icon bitmap.
     * Falls back to high-contrast neon cyan / emerald defaults if extraction fails.
     */
    fun extractColors(context: Context, packageName: String): Pair<Int, Int> {
        val defaultPrimary = 0xFF00FF88.toInt() // Neon Emerald
        val defaultSecondary = 0xFF00E5FF.toInt() // Cyan

        return try {
            val pm: PackageManager = context.packageManager
            val iconDrawable: Drawable = pm.getApplicationIcon(packageName)
            val bitmap = drawableToBitmap(iconDrawable)

            val palette = Palette.from(bitmap).generate()

            val primary = palette.getVibrantColor(
                palette.getLightVibrantColor(
                    palette.getDominantColor(defaultPrimary)
                )
            )

            val secondary = palette.getDarkVibrantColor(
                palette.getMutedColor(
                    palette.getLightMutedColor(defaultSecondary)
                )
            )

            Pair(primary, secondary)
        } catch (e: Exception) {
            Pair(defaultPrimary, defaultSecondary)
        }
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            return drawable.bitmap
        }

        val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 96
        val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 96

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }
}
