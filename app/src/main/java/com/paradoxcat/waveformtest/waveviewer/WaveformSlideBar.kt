package com.paradoxcat.waveformtest.waveviewer

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.math.pow

/**
 * Draws a straight line from the middle to samples points.
 * All functionality assumes that provided data has only 1 channel, 44100 Hz sample rate, 16-bits per sample, and is
 * already without WAV header.
 */
class WaveformSlideBar(context: Context, attrs: AttributeSet) : View(context, attrs) {

    companion object {

        const val LEFT_RIGHT_PADDING = 50.0f
        const val TOP_BOTTOM_PADDING = 50.0f
        const val MIRROR_SAMPLES = true
        const val LINE_WIDTH = 1.0f
        const val DEFAULT_STEP_COUNT = 1000
        private val MAX_VALUE = 2.0f.pow(16.0f) - 1 // max 16-bit value
        val INV_MAX_VALUE = 2.0f / MAX_VALUE // multiply with this to get % of max value

        /**
         * Calculate points to draw for drawLines() method.
         * @param mirrored -- if true, line will be drawn from y to -y, otherwise from y to middle
         * @param waveform -- audio data in IntArray format
         * @param width -- width of the view
         * @param height -- height of the view
         * @param stepCount -- how many samples to skip to draw maxLines
         * @return -- floatArray of x0, y0, x1, y1, ...
         */
        fun calculateDrawLinesPoints(
            waveform: IntArray,
            mirrored: Boolean,
            width: Int,
            height: Int,
            stepCount: Int
        ): FloatArray {
            var result = floatArrayOf()
            val sampleDistance =
                (width - LEFT_RIGHT_PADDING * 2) / (waveform.size - 1) // distance between centers of 2 samples
            val canvasWidth = width - LEFT_RIGHT_PADDING * 2 // width of the canvas minus paddings
            val maxAmplitude =
                height / 2.0f - TOP_BOTTOM_PADDING // max amount of px from middle to the edge minus pad
            val amplitudeScaleFactor =
                INV_MAX_VALUE * maxAmplitude // multiply by this to get number of px from middle

            // calculate points for drawLines()
            for (i in 0 until waveform.size step stepCount) {
                // TODO: fine-tune y-values with skipped samples
                val y = (height / 2.0f - waveform[i] * amplitudeScaleFactor)
                val x = LEFT_RIGHT_PADDING + i * sampleDistance
                // draw a straight line from y to middle or -y depending on mirrored flag
                result = if (mirrored) {
                    result.plus(x).plus(height - y).plus(x).plus(y)
                } else {
                    result.plus(x).plus(height / 2.0f).plus(x).plus(y)
                }
            }

            return result
        }

    }

    private val linePaint = Paint()
    private lateinit var waveForm: FloatArray

    init {
        linePaint.color = Color.rgb(0, 0, 0)
        linePaint.strokeWidth = LINE_WIDTH
        linePaint.strokeCap = Paint.Cap.ROUND
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        if (::waveForm.isInitialized) {
            canvas?.drawLines(waveForm, linePaint)
            canvas?.save()
        }
    }

    /**
     * Set raw audio data and draw it.
     * @param buffer -- raw audio buffer must be 16-bit samples packed together (mono, 16-bit PCM). Sample rate does
     *                  not matter, since we are not rendering any time-related information yet.
     */
    fun setData(intArray: IntArray) {
        waveForm = calculateDrawLinesPoints(intArray, MIRROR_SAMPLES, width, height, DEFAULT_STEP_COUNT)
        invalidate()
    }
}
