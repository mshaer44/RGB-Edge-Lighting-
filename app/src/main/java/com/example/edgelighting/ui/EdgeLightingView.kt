package com.example.edgelighting.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.os.Build
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator

/**
 * EdgeLightingView:
 * Custom Canvas-based renderer optimized for AMOLED / OLED displays.
 * Pure black pixels (transparent overlay) consume 0mW power on OLED panels while lit edge creates vibrant illumination.
 */
class EdgeLightingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var primaryColor: Int = 0xFF00f2ff.toInt()
    private var secondaryColor: Int = 0xFF00ff88.toInt()
    private var thickness: Float = 6f
    private var animSpeed: Float = 1.2f
    private var animStyle: String = "laser_comet"

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val borderPath = Path()
    private var pathLength: Float = 0f
    private var animPhase: Float = 0f
    private var animator: ValueAnimator? = null

    private val rainbowColors = intArrayOf(
        0xFFFF0055.toInt(),
        0xFFFF9900.toInt(),
        0xFFFFFF00.toInt(),
        0xFF33FF00.toInt(),
        0xFF00FFFF.toInt(),
        0xFF0066FF.toInt(),
        0xFFAA00FF.toInt(),
        0xFFFF0055.toInt()
    )

    init {
        // Hardware acceleration is enabled by default
        setLayerType(LAYER_TYPE_HARDWARE, null)
        startAnimation()
    }

    fun setConfig(
        primary: Int,
        secondary: Int,
        strokeWidth: Float,
        speed: Float,
        style: String
    ) {
        this.primaryColor = primary
        this.secondaryColor = secondary
        this.thickness = strokeWidth
        this.animSpeed = speed
        this.animStyle = style
        updatePaints()
        invalidate()
    }

    private fun updatePaints() {
        borderPaint.strokeWidth = thickness * resources.displayMetrics.density
        glowPaint.strokeWidth = (thickness * 2.4f) * resources.displayMetrics.density
    }

    fun startAnimation() {
        animator?.cancel()
        val duration = (2000L / animSpeed).toLong().coerceIn(400L, 6000L)
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            this.duration = duration
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener {
                animPhase = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    fun stopAnimation() {
        animator?.cancel()
        animator = null
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        rebuildPath(w, h)
    }

    private fun rebuildPath(w: Int, h: Int) {
        borderPath.reset()
        val inset = (thickness * resources.displayMetrics.density) / 2f + 2f
        val rect = RectF(inset, inset, w.toFloat() - inset, h.toFloat() - inset)
        val cornerRadius = 80f // Adaptive corner radius matching modern curved displays

        borderPath.addRoundRect(rect, cornerRadius, cornerRadius, Path.Direction.CW)

        val pathMeasure = PathMeasure(borderPath, false)
        pathLength = pathMeasure.length
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (pathLength <= 0f) return

        val width = width.toFloat()
        val height = height.toFloat()

        when (animStyle) {
            "rainbow_sweep" -> drawRainbowSweep(canvas, width, height)
            "chasing_snake" -> drawChasingSnake(canvas)
            "dual_orbit" -> drawDualOrbit(canvas)
            "pulse_wave" -> drawPulseWave(canvas)
            "fire_edge" -> drawFireEdge(canvas, width, height)
            else -> drawLaserComet(canvas)
        }
    }

    private fun drawLaserComet(canvas: Canvas) {
        val segmentLength = pathLength * 0.35f
        val offset = animPhase * pathLength

        borderPaint.color = primaryColor
        borderPaint.pathEffect = DashPathEffect(floatArrayOf(segmentLength, pathLength - segmentLength), offset)
        
        // Ambient soft outer glow
        glowPaint.color = Color.argb(120, Color.red(primaryColor), Color.green(primaryColor), Color.blue(primaryColor))
        glowPaint.pathEffect = borderPaint.pathEffect
        glowPaint.maskFilter = BlurMaskFilter(18f, BlurMaskFilter.Blur.NORMAL)

        canvas.drawPath(borderPath, glowPaint)
        glowPaint.maskFilter = null
        canvas.drawPath(borderPath, borderPaint)
    }

    private fun drawChasingSnake(canvas: Canvas) {
        val headLength = pathLength * 0.22f
        val offset = animPhase * pathLength

        borderPaint.color = primaryColor
        borderPaint.pathEffect = DashPathEffect(floatArrayOf(headLength, pathLength - headLength), offset)
        
        glowPaint.color = Color.argb(140, Color.red(primaryColor), Color.green(primaryColor), Color.blue(primaryColor))
        glowPaint.pathEffect = borderPaint.pathEffect
        glowPaint.maskFilter = BlurMaskFilter(22f, BlurMaskFilter.Blur.NORMAL)

        canvas.drawPath(borderPath, glowPaint)
        glowPaint.maskFilter = null
        canvas.drawPath(borderPath, borderPaint)
    }

    private fun drawDualOrbit(canvas: Canvas) {
        val seg = pathLength * 0.2f
        val offset1 = animPhase * pathLength
        val offset2 = -animPhase * pathLength

        // Orbit 1: Primary Color clockwise
        borderPaint.color = primaryColor
        borderPaint.pathEffect = DashPathEffect(floatArrayOf(seg, pathLength - seg), offset1)
        canvas.drawPath(borderPath, borderPaint)

        // Orbit 2: Secondary Color counter-clockwise
        borderPaint.color = secondaryColor
        borderPaint.pathEffect = DashPathEffect(floatArrayOf(seg, pathLength - seg), offset2)
        canvas.drawPath(borderPath, borderPaint)
    }

    private fun drawRainbowSweep(canvas: Canvas, w: Float, h: Float) {
        val sweep = SweepGradient(w / 2f, h / 2f, rainbowColors, null)
        val matrix = Matrix().apply {
            setRotate(animPhase * 360f, w / 2f, h / 2f)
        }
        sweep.setLocalMatrix(matrix)

        borderPaint.shader = sweep
        borderPaint.pathEffect = null
        canvas.drawPath(borderPath, borderPaint)
        borderPaint.shader = null
    }

    private fun drawPulseWave(canvas: Canvas) {
        val alpha = (0.3f + 0.7f * kotlin.math.sin(animPhase * Math.PI * 2).toFloat()).coerceIn(0.1f, 1.0f)
        borderPaint.color = Color.argb(
            (alpha * 255).toInt(),
            Color.red(primaryColor),
            Color.green(primaryColor),
            Color.blue(primaryColor)
        )
        borderPaint.pathEffect = null
        canvas.drawPath(borderPath, borderPaint)
    }

    private fun drawFireEdge(canvas: Canvas, w: Float, h: Float) {
        val fireColors = intArrayOf(primaryColor, secondaryColor, 0xFFFF0055.toInt(), primaryColor)
        val sweep = SweepGradient(w / 2f, h / 2f, fireColors, null)
        val matrix = Matrix().apply {
            setRotate(animPhase * 360f, w / 2f, h / 2f)
        }
        sweep.setLocalMatrix(matrix)

        borderPaint.shader = sweep
        borderPaint.strokeWidth = thickness * 1.5f * resources.displayMetrics.density
        canvas.drawPath(borderPath, borderPaint)
        borderPaint.shader = null
    }

    override fun onDetachedFromWindow() {
        stopAnimation()
        super.onDetachedFromWindow()
    }
}
