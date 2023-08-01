package com.paradoxcat.waveviewer.view

import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.animation.AccelerateDecelerateInterpolator
import com.paradoxcat.waveviewer.model.Point
import kotlin.math.floor
import kotlin.math.pow

/**
 * Custom view to draw the waveform pattern of an audio file.
 *
 * Array value determines the height (y) of the waveform, while
 * the array length determines the width (x) of the waveform.
 */
class WaveformSlideBar(context: Context, attrs: AttributeSet) : CustomView(context, attrs) {

    companion object {
        const val TAG = "WaveformSlideBar"
        private const val LEFT_RIGHT_PADDING = 45.0f
        private const val TOP_BOTTOM_PADDING = 50.0f
        const val LINE_WIDTH = 1.0f // thickness of the waveform line
        const val DEFAULT_STEP_COUNT = 2000 // how many samples to skip when drawing the waveform
        const val ANIMATION_DURATION = 1000L // duration of the drawing in milliseconds
        const val ANIMATION_START_PERCENTAGE = 0.0f
        const val ANIMATION_END_PERCENTAGE = 1.0f
        val MAX_VALUE = 2.0f.pow(16.0f) - 1 // max 16-bit value
        private val INV_MAX_VALUE = 2.0f / MAX_VALUE // multiply with this to get % of max value

        /**
         * Calculate points to draw for drawLines() method.
         * @param waveform -- audio data in IntArray format
         * @param width -- width of the view
         * @param height -- height of the view
         * @param stepCount -- how many samples to skip to draw maxLines
         */
        fun calculatePoints(
            waveform: IntArray,
            width: Int,
            height: Int,
            stepCount: Int
        ): Array<Point> {
            var result = arrayOf<Point>()
            // calculate distance between samples and and maximum amplitude length
            val sampleDistance = (width - LEFT_RIGHT_PADDING * 2) / (waveform.size - 1)
            val maxAmplitude = height / 2.0f - TOP_BOTTOM_PADDING
            val amplitudeScaleFactor = INV_MAX_VALUE * maxAmplitude

            // calculate points for drawLines()
            for (i in waveform.indices step stepCount) {
                // TODO: fine-tune y-values with skipped samples
                val x = LEFT_RIGHT_PADDING + i * sampleDistance
                val y = (height / 2.0f - waveform[i] * amplitudeScaleFactor)
                val point = Point(x, y)
                result = result.plus(point)
            }
            return result
        }

        /**
         * Convert part of the waveform samples to a Path.
         * @param points -- samples of the waveform in Point
         * @param startIndex -- index of the first sample to draw
         * @param endIndex -- index of the last sample to draw
         * @return Path object of the waveform samples[startIndex, endIndex]
         */
        fun getPathChunk(points: Array<Point>, startIndex: Int, endIndex: Int): Path {
            // pre-condition check
            if (endIndex - startIndex <= 1 || points.isEmpty() || startIndex < 0
                || endIndex >= points.size
            ) {
                return Path()
            }
            val result = Path()
            // set starting point for the path
            var prevX = points[startIndex].x
            var prevY = points[startIndex].y
            result.moveTo(prevX, prevY)
            // calculate points to draw from startIndex to endIndex
            for (i in startIndex + 1 until endIndex) {
                val x = points[i].x
                val y = points[i].y
                // calculate control points for the curve
                val controlX1 = (prevX + x) / 2
                val controlY1 = (prevY + y) / 2
                result.quadTo(controlX1, controlY1, x, y)
                prevX = x
                prevY = y
            }
            return result
        }


    }

    private lateinit var waveform: Path
    private lateinit var rawData: IntArray
    private lateinit var points: Array<Point>
    private lateinit var animator: ObjectAnimator

    private val linePaint = Paint()

    private var indexOfDrawnPoints: Int = 0

    init {
        initLinePaint()
        initAnimator()
    }

    private fun initLinePaint() {
        linePaint.color = Color.BLACK
        linePaint.strokeWidth = LINE_WIDTH
        linePaint.style = Paint.Style.STROKE
        linePaint.strokeCap = Paint.Cap.ROUND
    }

    private fun initAnimator() {
        animator = getAnimator(ANIMATION_START_PERCENTAGE, ANIMATION_END_PERCENTAGE)
        animator.interpolator = AccelerateDecelerateInterpolator()
        animator.duration = ANIMATION_DURATION
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        // pre-condition check
        if (!::waveform.isInitialized) {
            return
        }

        canvas?.drawPath(waveform, linePaint)
    }

    override fun render() {
        super.render()
        waveform = Path()
        waveform.moveTo(0F, height / 2.0f)
        waveform.lineTo(width.toFloat(), height / 2.0f)
        points = calculatePoints(rawData, width, height, DEFAULT_STEP_COUNT)
        indexOfDrawnPoints = 0
        animator.start()
    }

    override fun setAnimation(animationValue: Float) {
        // get next index to draw depending on current animation value
        val nextDrawIndex: Int = floor(points.size * animationValue).toInt() - 1
        // get next path chunk to draw [last drawn index] to [next draw index]
        val nextChunk = getPathChunk(points, indexOfDrawnPoints, nextDrawIndex)
        // add to waveform buffer
        waveform.addPath(nextChunk)
        // update index of last drawn point
        indexOfDrawnPoints = nextDrawIndex
        invalidate()
    }

    /**
     * Set the transformed audio data to be drawn.
     *
     * @param intArray -- converted audio buffer
     */
    fun setData(intArray: IntArray) {
        rawData = intArray
        render()
    }
}
