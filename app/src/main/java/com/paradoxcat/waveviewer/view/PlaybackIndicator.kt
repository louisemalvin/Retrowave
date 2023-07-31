package com.paradoxcat.waveviewer.view

import android.content.Context
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View

class PlaybackIndicator(context: Context, attrs: AttributeSet) : View(context, attrs) {

    companion object {
        const val TAG = "PlaybackIndicator"
        const val LEFT_RIGHT_PADDING = 10.0f
        const val BORDER_WIDTH = 10.0f
    }

    private var isPlaying = false
    private val innerColorOn: Int = Color.parseColor("#FF0000")
    private val innerColorOff: Int = Color.parseColor("#650000")
    private val innerPaintOff = Paint()
    private val innerPaintOn = Paint()
    private val borderPaint = Paint()
    private val bloomPaint = Paint()

    init {
        initInnerPaint()
        initBloomPaint()
        initBorderPaint()
    }

    private fun initInnerPaint() {
        innerPaintOff.color = innerColorOff
    }

    private fun initBloomPaint() {
        bloomPaint.isAntiAlias = true
        bloomPaint.color = Color.RED
        val blurMask = BlurMaskFilter(25f, BlurMaskFilter.Blur.NORMAL)
        bloomPaint.maskFilter = blurMask
    }

    private fun initBorderPaint() {
        borderPaint.style = Paint.Style.STROKE
        borderPaint.strokeWidth = 10f
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
        val center = width / 2f
        val radius = center - 50f
        if (isPlaying) {
            canvas.drawCircle(center, center, radius, bloomPaint)
            canvas.drawCircle(center, center, radius-10f, borderPaint)
            canvas.drawCircle(center, center, radius-10f, innerPaintOn)
        } else {
            canvas.drawCircle(center, center, radius-10f, borderPaint)
            canvas.drawCircle(center, center, radius-10f, innerPaintOff)
        }


    }

    /**
     * Turn the light on if audio is playing.
     * @param isPlaying true if audio is playing, false otherwise
     */
    fun setData(isPlaying: Boolean) {
        this.isPlaying = isPlaying
        // Initialize gradient color for on state
        val center = width / 2f
        val radius = center - 50f
        val gradient = RadialGradient(
            center, center, radius,
            innerColorOn, innerColorOff,
            Shader.TileMode.CLAMP
        )
        innerPaintOn.shader = gradient
        invalidate()
    }
}