package com.paradoxcat.waveviewer.view

import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import com.paradoxcat.waveviewer.R
import kotlin.math.ceil

class LayeredButton(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    companion object {
        const val DEFAULT_HEIGHT_DIFFERENCE = 50f
        const val DEFAULT_CORNER_RADIUS = 10f
        const val DEFAULT_TOP_LAYER_COLOR = "#FF0000"
        const val DEFAULT_BOTTOM_LAYER_COLOR = "#650000"
    }

    private lateinit var topRect: RectF
    private lateinit var bottomRect: RectF

    private val topLayerPaint = Paint()
    private val bottomLayerPaint = Paint()

    private var heightDifference: Float = DEFAULT_HEIGHT_DIFFERENCE
    private var icon: Drawable? = null

    init {
        initTypedArrayOrDefault(attrs)
    }

    /**
     * Get styles from xml or use default values.
     */
    private fun initTypedArrayOrDefault(attrs: AttributeSet?) {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.LayeredButton)
        topLayerPaint.color = typedArray.getColor(
            R.styleable.LayeredButton_topLayerColor,
            Color.parseColor(DEFAULT_TOP_LAYER_COLOR)
        )
        bottomLayerPaint.color = typedArray.getColor(
            R.styleable.LayeredButton_bottomLayerColor,
            Color.parseColor(DEFAULT_BOTTOM_LAYER_COLOR)
        )
        heightDifference = typedArray.getDimension(
            R.styleable.LayeredButton_heightDifference,
            DEFAULT_HEIGHT_DIFFERENCE
        )
        icon = typedArray.getDrawable(R.styleable.LayeredButton_icon)
        typedArray.recycle()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        topRect = RectF(0f, 0f, w.toFloat(), h.toFloat() - heightDifference)
        bottomRect = RectF(
            0f,
            h.toFloat() - DEFAULT_HEIGHT_DIFFERENCE - DEFAULT_CORNER_RADIUS,
            w.toFloat(),
            h.toFloat()
        )
        icon?.bounds = createIconBounds(topRect, icon!!)
    }

    override fun onDraw(canvas: Canvas?) {
        if (!this::topRect.isInitialized) {
            return
        }
        super.onDraw(canvas)
        canvas?.drawRoundRect(
            bottomRect,
            DEFAULT_CORNER_RADIUS,
            DEFAULT_CORNER_RADIUS,
            bottomLayerPaint
        )
        canvas?.drawRoundRect(
            topRect,
            DEFAULT_CORNER_RADIUS,
            DEFAULT_CORNER_RADIUS,
            topLayerPaint
        )

        icon?.draw(canvas?: return)
    }

    private fun setHeightDifference(heightDifference: Float) {
        var newHeight = heightDifference
        if (heightDifference < 0) {
            throw IllegalArgumentException("Height difference cannot be negative")
        }
        if (heightDifference > DEFAULT_HEIGHT_DIFFERENCE) {
            newHeight = DEFAULT_HEIGHT_DIFFERENCE
        }
        if (newHeight == this.heightDifference) {
            return
        }
        topRect = RectF(
            0f,
            0f + DEFAULT_HEIGHT_DIFFERENCE - heightDifference,
            width.toFloat(),
            height.toFloat() - heightDifference
        )
        icon?.bounds = createIconBounds(topRect, icon!!)
        this.heightDifference = newHeight
        invalidate()
    }

    fun createIconBounds(topRect: RectF, icon: Drawable): Rect {
        val iconWidth = icon.intrinsicWidth
        val iconHeight = icon.intrinsicHeight

        val iconLeft = ceil((topRect.centerX() - iconWidth / 2)).toInt()
        val iconTop = ceil((topRect.centerY() - iconHeight / 2)).toInt()
        val iconRight = iconLeft + iconWidth
        val iconBottom = iconTop + iconHeight

        return Rect(iconLeft, iconTop, iconRight, iconBottom)
    }

    fun setData(heightDifference: Float) {
        ObjectAnimator.ofFloat(
            this,
            "heightDifference",
            this.heightDifference,
            heightDifference,
            DEFAULT_HEIGHT_DIFFERENCE
        ).apply {
            duration = 1000
            start()
        }
    }
}