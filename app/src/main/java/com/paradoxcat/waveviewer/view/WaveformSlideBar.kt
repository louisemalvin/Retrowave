package com.paradoxcat.waveviewer.view

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import com.paradoxcat.waveviewer.model.Point
import kotlin.math.floor
import kotlin.math.pow

/**
 * Draws a straight line from the middle to samples points.
 * All functionality assumes that provided data has only 1 channel, 44100 Hz sample rate, 16-bits per sample, and is
 * already without WAV header.
 */
class WaveformSlideBar(context: Context, attrs: AttributeSet) : View(context, attrs) {

    companion object {

        const val LEFT_RIGHT_PADDING = 45.0f
        const val TOP_BOTTOM_PADDING = 50.0f
        const val LINE_WIDTH = 1.0f
        const val DEFAULT_STEP_COUNT = 2000
        const val ANIMATION_DURATION = 2000L
        private val MAX_VALUE = 2.0f.pow(16.0f) - 1 // max 16-bit value
        val INV_MAX_VALUE = 2.0f / MAX_VALUE // multiply with this to get % of max value

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
            val sampleDistance =
                (width - LEFT_RIGHT_PADDING * 2) / (waveform.size - 1) // distance between centers of 2 samples
            val maxAmplitude =
                height / 2.0f - TOP_BOTTOM_PADDING // max amount of px from middle to the edge minus pad
            val amplitudeScaleFactor =
                INV_MAX_VALUE * maxAmplitude // multiply by this to get number of px from middle

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
            if (endIndex - startIndex <= 1 || points.isEmpty() || startIndex < 0 || endIndex >= points.size) {
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
//                result.addCircle(x, y, 4.0f, Path.Direction.CW)
                prevX = x
                prevY = y
            }
            return result
        }


    }

    private lateinit var waveform: Path

    private val linePaint = Paint()
    private val animator: ValueAnimator
    private var indexOfDrawnPoints: Int = 0

    init {
        linePaint.color = Color.rgb(0, 0, 0)
        linePaint.strokeWidth = LINE_WIDTH
        linePaint.style = Paint.Style.STROKE
        linePaint.strokeCap = Paint.Cap.ROUND
        // initialize animator
        animator = ValueAnimator.ofFloat(0.0f, 1.0f)
        animator.interpolator = AccelerateDecelerateInterpolator()
        animator.duration = ANIMATION_DURATION
    }

    /**
     * Render part of the waveform samples to the view.
     * @param points -- array of samples to be drawn
     * @param phase -- current animation phase, from 0.0 to 1.0
     */
    private fun render(points: Array<Point>, phase: Float) {
        val nextDrawIndex: Int = floor(points.size * phase).toInt() - 1
        waveform.addPath(getPathChunk(points, indexOfDrawnPoints, nextDrawIndex))
        indexOfDrawnPoints = nextDrawIndex
        invalidate()
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        if (::waveform.isInitialized) {
            canvas?.drawPath(waveform, linePaint)
        }
    }

    /**
     * Set raw audio data and draw it.
     * @param buffer -- raw audio buffer must be 16-bit samples packed together (mono, 16-bit PCM). Sample rate does
     *                  not matter, since we are not rendering any time-related information yet.
     */
    fun setData(intArray: IntArray) {
        val points = calculatePoints(intArray, width, height, DEFAULT_STEP_COUNT)
        val initPath = Path()
        initPath.moveTo(0F, height / 2.0f)
        initPath.lineTo(width.toFloat(), height / 2.0f)
        waveform = Path()
        waveform.addPath(initPath)
        indexOfDrawnPoints = 0
        animator.apply {
            addUpdateListener { animation ->
                val phase = animation.animatedValue as Float
                render(points, phase)
            }
        }
        animator.start()
    }
}
