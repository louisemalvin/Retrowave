package com.paradoxcat.waveviewer.view

import android.content.Context
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.util.AttributeSet

class PlaybackIndicator(context: Context, attrs: AttributeSet) : CustomView(context, attrs) {

    companion object {
        const val TAG = "PlaybackIndicator"
        const val PADDING = 10.0f
        const val BORDER_WIDTH_SCALE = 0.4f
        const val BLOOM_WIDTH_SCALE = 0.4f
        const val DEFAULT_INNER_COLOR_ON_FIRST = "#FF0000"
        const val DEFAULT_INNER_COLOR_ON_SECOND = "#9e0000"
        const val DEFAULT_INNER_COLOR_OFF_FIRST = "#6e0101"
        const val DEFAULT_INNER_COLOR_OFF_SECOND = "#4a0000"
    }

    private val innerColorOnFirst: Int = Color.parseColor(DEFAULT_INNER_COLOR_ON_FIRST)
    private val innerColorOnSecond: Int = Color.parseColor(DEFAULT_INNER_COLOR_ON_SECOND)
    private val innerColorOffFirst: Int = Color.parseColor(DEFAULT_INNER_COLOR_OFF_FIRST)
    private val innerColorOffSecond: Int = Color.parseColor(DEFAULT_INNER_COLOR_OFF_SECOND)
    private val innerPaintOff = Paint()
    private val innerPaintOn = Paint()
    private val borderPaint = Paint()
    private val bloomPaint = Paint()

    private var isPlaying = false
    private var center = 0f
    private var circleRadius = 0f
    private var bloomRadius = 0f

    init {
        initInnerPaint()
        initBloomPaint()
        initBorderPaint()
    }

    private fun initInnerPaint() {
        innerPaintOn.color = innerColorOnFirst
        innerPaintOff.color = innerColorOffFirst
    }

    private fun initBloomPaint() {
        bloomPaint.isAntiAlias = true
        bloomPaint.color = Color.RED
    }

    private fun initBorderPaint() {
        borderPaint.style = Paint.Style.STROKE
        borderPaint.isAntiAlias = true
        borderPaint.color = Color.WHITE
    }


    // override onMeasure to make the view square
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        setMeasuredDimension(widthMeasureSpec, widthMeasureSpec)

    }

    // override onDraw to light indicator depending on audio playback state
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (isPlaying) {
            canvas.drawCircle(center, center, bloomRadius, bloomPaint)
            canvas.drawCircle(center, center, circleRadius, borderPaint)
            canvas.drawCircle(center, center, circleRadius, innerPaintOn)
        } else {
            canvas.drawCircle(center, center, circleRadius, borderPaint)
            canvas.drawCircle(center, center, circleRadius, innerPaintOff)
        }
    }

    override fun render() {
        // pre-condition check
        if (width==0 || height==0) {
            return
        }
        center = width / 2f
        bloomRadius = center - PADDING
        circleRadius = bloomRadius - (BLOOM_WIDTH_SCALE * bloomRadius)
        borderPaint.strokeWidth = BORDER_WIDTH_SCALE * circleRadius
        val blurMask = BlurMaskFilter(BLOOM_WIDTH_SCALE * bloomRadius, BlurMaskFilter.Blur.NORMAL)
        val gradientOff = RadialGradient(
            center, center, circleRadius,
            innerColorOffFirst, innerColorOffSecond,
            Shader.TileMode.CLAMP
        )
        val gradientOn = RadialGradient(
            center, center, circleRadius,
            innerColorOnFirst, innerColorOnSecond,
            Shader.TileMode.CLAMP
        )
        bloomPaint.maskFilter = blurMask
        innerPaintOn.shader = gradientOn
        innerPaintOff.shader = gradientOff
        invalidate()
    }

    override fun setAnimation(animationValue: Float) {
        // nothing to animate currently
    }

    /**
     * Turn the light on if audio is playing.
     * @param isPlaying true if audio is playing, false otherwise
     */
    fun setData(isPlaying: Boolean) {
        this.isPlaying = isPlaying
        render()
    }
}