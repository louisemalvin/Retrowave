package com.paradoxcat.waveformtest.waveviewer
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import com.paradoxcat.waveformtest.model.Point
import kotlin.math.floor
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
        const val LINE_WIDTH = 2.0f
        const val DEFAULT_STEP_COUNT = 2000
        private val MAX_VALUE = 2.0f.pow(16.0f) - 1 // max 16-bit value
        val INV_MAX_VALUE = 2.0f / MAX_VALUE // multiply with this to get % of max value

    }

    private lateinit var waveform: Path

    private val linePaint = Paint()
    private val animator: ValueAnimator
    private var indexOfDrawnPoints: Int = 0

    init {
        linePaint.color = Color.rgb(0, 0, 0)
        linePaint.strokeWidth = LINE_WIDTH
        linePaint.style = Paint.Style.FILL_AND_STROKE
        linePaint.strokeCap = Paint.Cap.ROUND
        animator = ValueAnimator.ofFloat(0.0f, 1.0f)
        animator.interpolator = AccelerateDecelerateInterpolator()
        animator.duration = 2000
    }
    /**
     * Calculate points to draw for drawLines() method.
     * @param waveform -- audio data in IntArray format
     * @param width -- width of the view
     * @param height -- height of the view
     * @param stepCount -- how many samples to skip to draw maxLines
     */
    private fun calculatePathPoints(
        waveform: IntArray,
        width: Int,
        height: Int,
        stepCount: Int
    ): Array<Point> {
        var result = arrayOf<Point>()
        val sampleDistance =
            (width - LEFT_RIGHT_PADDING * 2) / (waveform.size - 1) // distance between centers of 2 samples
        val canvasWidth = width - LEFT_RIGHT_PADDING * 2 // width of the canvas minus paddings
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
            // draw a straight line from y to middle or -y depending on mirrored flag
            result = result.plus(point)
        }
        return result
    }

    private fun preparePath(points: Array<Point>): Path {
        val result = Path()
        var prevX = points.first().x
        var prevY = points.first().y
        result.moveTo(prevX, prevY)
        for (point in points) {
            val x = point.x
            val y = point.y
            val controlX1 = (prevX + x) / 2
            val controlY1 = (prevY + y) / 2
            result.quadTo(controlX1, controlY1, x, y)
            prevX = x
            prevY = y
        }
        return result
    }

    private fun render(points: Array<Point>, animationValue: Float) {
        val nextDrawIndex : Int = floor(points.size * animationValue).toInt() - 1
        if (nextDrawIndex <= indexOfDrawnPoints) return
        val pointsToDraw = points.copyOfRange(indexOfDrawnPoints, nextDrawIndex)
        indexOfDrawnPoints = nextDrawIndex + 1
        waveform.addPath(preparePath(pointsToDraw))
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas?.save()
        if (::waveform.isInitialized) {
            canvas?.drawPath(waveform, linePaint)
        }
        canvas?.restore()
    }

    /**
     * Set raw audio data and draw it.
     * @param buffer -- raw audio buffer must be 16-bit samples packed together (mono, 16-bit PCM). Sample rate does
     *                  not matter, since we are not rendering any time-related information yet.
     */
    fun setData(intArray: IntArray) {
        val points = calculatePathPoints(intArray, width, height, DEFAULT_STEP_COUNT)
        waveform = Path()
        indexOfDrawnPoints = 0
        animator.apply {
            addUpdateListener { animation ->
                val phase = animation.animatedValue as Float
                render(points, phase)
                invalidate()
            }
        }
        animator.start()
    }
}
