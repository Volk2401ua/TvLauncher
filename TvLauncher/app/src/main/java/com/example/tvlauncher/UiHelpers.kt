package com.example.tvlauncher

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable

object UiHelpers {

    fun tile(context: Context, accent: Int): Drawable {
        val d = context.resources.displayMetrics.density
        fun make(fill: Int, strokeW: Int, strokeC: Int): Drawable =
            GradientDrawable().apply {
                cornerRadius = 18f * d
                setColor(fill)
                if (strokeW > 0) setStroke(strokeW, strokeC)
            }
        return StateListDrawable().apply {
            addState(intArrayOf(android.R.attr.state_focused), make(0xFF1F2A38.toInt(), (2 * d).toInt(), accent))
            addState(intArrayOf(android.R.attr.state_pressed), make(0xFF2A3542.toInt(), 0, 0))
            addState(intArrayOf(), make(0xFF1A2027.toInt(), (1 * d).toInt(), 0x22FFFFFF))
        }
    }

    fun ghostButton(context: Context, accent: Int): Drawable {
        val d = context.resources.displayMetrics.density
        fun make(fill: Int, strokeW: Int, strokeC: Int): Drawable =
            GradientDrawable().apply {
                cornerRadius = 20f * d
                setColor(fill)
                if (strokeW > 0) setStroke(strokeW, strokeC)
            }
        val accentTint = (accent and 0x00FFFFFF) or 0x33000000
        return StateListDrawable().apply {
            addState(intArrayOf(android.R.attr.state_focused), make(accentTint, (2 * d).toInt(), accent))
            addState(intArrayOf(android.R.attr.state_pressed), make(0x33FFFFFF, 0, 0))
            addState(intArrayOf(), make(0x00000000, (1 * d).toInt(), 0x33FFFFFF))
        }
    }

    class TiledDrawable(source: Drawable, tilePx: Int) : Drawable() {
        private val paint = Paint().apply {
            isAntiAlias = true
            shader = run {
                val bmp = Bitmap.createBitmap(tilePx, tilePx, Bitmap.Config.ARGB_8888)
                val c = Canvas(bmp)
                source.setBounds(0, 0, tilePx, tilePx)
                source.draw(c)
                BitmapShader(bmp, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
            }
        }

        override fun draw(canvas: Canvas) { canvas.drawRect(bounds, paint) }
        override fun setAlpha(alpha: Int) { paint.alpha = alpha; invalidateSelf() }
        override fun setColorFilter(cf: ColorFilter?) { paint.colorFilter = cf; invalidateSelf() }
        @Deprecated("Deprecated in Java")
        override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
    }
}