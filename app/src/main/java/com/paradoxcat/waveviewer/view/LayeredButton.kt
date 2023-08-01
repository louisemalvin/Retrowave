package com.paradoxcat.waveviewer.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuffColorFilter
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.Log
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AccelerateInterpolator
import com.paradoxcat.waveviewer.R
import kotlin.math.ceil

/**
 * Modular layered-button view for 3d touch-like interaction.
 *
 * Additional customization is done through XML attributes.
 * See the XML resource file for more details.
 */
class LayeredButton(context: Context, attrs: AttributeSet?) : CustomView(context, attrs) {

    companion object {
        const val TAG = "LayeredButton"
        const val DEFAULT_MAX_HEIGHT_DIFFERENCE = 50f
        const val DEFAULT_CORNER_RADIUS = 10f
        const val DEFAULT_LOCKED_HEIGHT_SCALE_FACTOR = 0.5f
        const val DEFAULT_TOP_LAYER_COLOR = "#FF0000"
        const val DEFAULT_BOTTOM_LAYER_COLOR = "#650000"
        const val DEFAULT_ICON_COLOR = "#FFFFFF"
    }

    private lateinit var topRect: RectF
    private lateinit var bottomRect: RectF

    private val topLayerPaint = Paint()
    private val bottomLayerPaint = Paint()

    private var lockScaleFactor = DEFAULT_LOCKED_HEIGHT_SCALE_FACTOR
    private var cornerRadius = DEFAULT_CORNER_RADIUS
    private var maxHeight = DEFAULT_MAX_HEIGHT_DIFFERENCE
    private var currentMaxHeight = DEFAULT_MAX_HEIGHT_DIFFERENCE
    private var currentHeight = DEFAULT_MAX_HEIGHT_DIFFERENCE
    private var icon: Drawable? = null
    private var isLocked = false

    init {
        initTypedArrayOrDefault(attrs)
    }

    /**
     * Get styles from xml or use default values.
     */
    private fun initTypedArrayOrDefault(attrs: AttributeSet?) {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.LayeredButton)
        // get color value
        topLayerPaint.color = typedArray.getColor(
            R.styleable.LayeredButton_topLayerColor,
            Color.parseColor(DEFAULT_TOP_LAYER_COLOR)
        )
        bottomLayerPaint.color = typedArray.getColor(
            R.styleable.LayeredButton_bottomLayerColor,
            Color.parseColor(DEFAULT_BOTTOM_LAYER_COLOR)
        )
        // get max height difference
        maxHeight = typedArray.getDimension(
            R.styleable.LayeredButton_heightDifference,
            DEFAULT_MAX_HEIGHT_DIFFERENCE
        )
        currentMaxHeight = maxHeight
        currentHeight = maxHeight
        // get corner radius value
        cornerRadius = typedArray.getDimension(
            R.styleable.LayeredButton_cornerRadius,
            DEFAULT_CORNER_RADIUS
        )
        // get icon reference and color
        icon = typedArray.getDrawable(R.styleable.LayeredButton_icon)
        icon?.colorFilter = PorterDuffColorFilter(
            typedArray.getColor(
                R.styleable.LayeredButton_iconColor,
                Color.parseColor(DEFAULT_ICON_COLOR)
            ),
            android.graphics.PorterDuff.Mode.SRC_IN
        )
        // xml read finished, recycle the typed array
        typedArray.recycle()
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
        icon?.draw(canvas ?: return)
    }

    override fun render() {
        // pre-condition check
        if (width==0 || height==0) {
            return
        }
        // initialize how the buttons should look like
        topRect = RectF(0f, 0f, width.toFloat(), height.toFloat() - currentHeight)
        bottomRect = RectF(
            0f,
            height.toFloat() - maxHeight - cornerRadius,
            width.toFloat(),
            height.toFloat()
        )
        // adjust icon bounds to the center of the top layer
        icon?.bounds = createIconBounds(topRect, icon!!)
        invalidate()
    }

    @Suppress("unused")
    override fun setAnimation(animationValue: Float) {
        var newHeight = animationValue
        // pre-condition checks
        // button could not be pushed lower than bottom layer
        if (animationValue < 0) {
            Log.e(TAG, "Set height < 0.")
            throw IllegalArgumentException("Height < 0")
        }
        // button can only be pushed up to the default value
        if (animationValue > maxHeight) {
            Log.w(TAG, "Set height > maximum allowed.")
            newHeight = currentMaxHeight
        }
        // return if height is the same
        if (newHeight==this.currentHeight) {
            Log.i(TAG, "Set height is the same as current. Skipping.")
            return
        }

        // adjust top layer and icon to the new value
        topRect = RectF(
            0f,
            0f + maxHeight - newHeight,
            width.toFloat(),
            height.toFloat() - newHeight
        )
        icon?.bounds = createIconBounds(topRect, icon!!)
        this.currentHeight = newHeight
        invalidate()
    }

    private fun createIconBounds(topRect: RectF, icon: Drawable): Rect {
        val iconWidth = icon.intrinsicWidth
        val iconHeight = icon.intrinsicHeight

        val iconLeft = ceil((topRect.centerX() - iconWidth / 2)).toInt()
        val iconTop = ceil((topRect.centerY() - iconHeight / 2)).toInt()
        val iconRight = iconLeft + iconWidth
        val iconBottom = iconTop + iconHeight

        return Rect(iconLeft, iconTop, iconRight, iconBottom)
    }

    fun setData(heightDifference: Float) {
        val animator = getAnimator(currentHeight, heightDifference, currentMaxHeight)
        animator.duration = 500
        animator.interpolator = AccelerateDecelerateInterpolator()
        animator.start()
    }

    fun setData(lock: Boolean) {
        if (isLocked) {
            return
        }
        // keeps the button 'pressed' if it is locked
        if (lock) {
            isLocked = true
            currentMaxHeight = maxHeight * lockScaleFactor
        }
        val animator = getAnimator(currentHeight, 0f, currentMaxHeight)
        animator.duration = 400
        animator.interpolator = AccelerateDecelerateInterpolator()
        animator.start()
    }

    fun setData() {
        // unlock the button
        if (isLocked) {
            isLocked = false
            currentMaxHeight = maxHeight
            val animator = getAnimator(
                currentHeight,
                currentMaxHeight,
                currentMaxHeight - 10f,
                currentMaxHeight
            )
            animator.duration = 400
            animator.interpolator = AccelerateInterpolator()
            animator.start()
        }
    }
}