package com.paradoxcat.waveformtest.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import java.nio.ByteBuffer
import kotlin.math.pow

/**
 * Draw all samples as small red circles and connect them with straight green lines.
 * All functionality assumes that provided data has only 1 channel, 44100 Hz sample rate, 16-bits per sample, and is
 * already without WAV header.
 */
class WaveformSlideBar(context: Context, attrs: AttributeSet) : View(context, attrs) {

    companion object {
        const val LEFT_RIGHT_PADDING = 50.0f
        const val TOP_BOTTOM_PADDING = 50.0f
        const val SAMPLE_DOT_RADIUS = 2.0f
        private val MAX_VALUE = 2.0f.pow(16.0f) - 1 // max 16-bit value
        val INV_MAX_VALUE = 1.0f / MAX_VALUE // multiply with this to get % of max value

        /** Transform raw audio into drawable array of integers */
        fun transformRawData(buffer: ByteBuffer): IntArray {
            val nSamples = buffer.limit() / 2 // assuming 16-bit PCM mono
            val waveForm = IntArray(nSamples)
            for (i in 1 until buffer.limit() step 2) {
                waveForm[i / 2] = (buffer[i].toInt() shl 8) or buffer[i - 1].toInt()
            }
            return waveForm
        }
    }

    private val linePaint = Paint()
    private val sampleDotPaint = Paint()
    private lateinit var waveForm: IntArray

    init {
        linePaint.color = Color.rgb(0, 255, 0)
        sampleDotPaint.color = Color.rgb(255, 0, 0)
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        if (::waveForm.isInitialized) {
            val sampleDistance =
                (width - LEFT_RIGHT_PADDING * 2) / (waveForm.size - 1) // distance between centers of 2 samples
            val maxAmplitude = height / 2.0f - TOP_BOTTOM_PADDING // max amount of px from middle to the edge minus pad
            val amplitudeScaleFactor = INV_MAX_VALUE * maxAmplitude // multiply by this to get number of px from middle

            var prevX = LEFT_RIGHT_PADDING
            var prevY = height / 2.0f - waveForm[0] * amplitudeScaleFactor
            canvas?.drawCircle(prevX, prevY, SAMPLE_DOT_RADIUS, sampleDotPaint)
            for (i in 1 until waveForm.size) {
                val x = LEFT_RIGHT_PADDING + i * sampleDistance
                val y = height / 2.0f - waveForm[i] * amplitudeScaleFactor // y axis starts on top and is pointed down
                canvas?.drawCircle(x, y, SAMPLE_DOT_RADIUS, sampleDotPaint)
                canvas?.drawLine(prevX, prevY, x, y, linePaint)
                prevX = x
                prevY = y
            }
        }
    }

    /**
     * Set raw audio data and draw it.
     * @param buffer -- raw audio buffer must be 16-bit samples packed together (mono, 16-bit PCM). Sample rate does
     *                  not matter, since we are not rendering any time-related information yet.
     */
    fun setData(buffer: ByteBuffer) {
        waveForm = transformRawData(buffer)
        invalidate()
    }
}