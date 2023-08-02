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
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import com.paradoxcat.waveviewer.R
import kotlin.math.ceil

/**
 * Modular layered-button view for 3d touch-like interaction.
 *
 * Additional customization is done through XML attributes.
 * See attributes resource file for more details.
 */
class LayeredButton(context: Context, attrs: AttributeSet?) : CustomView(context, attrs) {

    companion object {
        const val TAG = "LayeredButton"
        const val LEFT_COORDINATE = 0.0f
        const val TOP_COORDINATE = 0.0f
        const val ANIMATION_DURATION = 500L // animation duration for full press
        const val DEFAULT_MAX_HEIGHT_DIFFERENCE = 50f // default attribute values
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
        // pre-condition check::
        if (!this::topRect.isInitialized) {
            return
        }

        super.onDraw(canvas)
        canvas?.drawRoundRect(
            bottomRect,
            cornerRadius,
            cornerRadius,
            bottomLayerPaint
        )
        canvas?.drawRoundRect(
            topRect,
            cornerRadius,
            cornerRadius,
            topLayerPaint
        )
        // draw icon if it is available
        icon?.draw(canvas ?: return)
    }

    override fun render() {
        // pre-condition check:
        if (width==0 || height==0) {
            return
        }

        // calculate the shape of the top and bottom layer
        topRect = RectF(LEFT_COORDINATE, TOP_COORDINATE, width.toFloat(), height.toFloat() - currentHeight)
        bottomRect = RectF(
            LEFT_COORDINATE,
            height.toFloat() - maxHeight - 2 * cornerRadius,
            width.toFloat(),
            height.toFloat()
        )
        // adjust icon position to the center of the top layer
        icon?.bounds = createIconBounds(topRect, icon!!)
        invalidate()
    }

    override fun setAnimation(animationValue: Float) {
        var newHeight = animationValue
        // pre-condition check:
        // button could not be pushed lower than bottom layer
        if (animationValue < 0) {
            newHeight = 0f
        }
        // button can only be pushed up to the default value
        if (animationValue > maxHeight) {
            newHeight = currentMaxHeight
        }
        // return if height is the same
        if (newHeight==this.currentHeight) {
            return
        }

        // adjust top layer and icon to the new value
        topRect = RectF(
            LEFT_COORDINATE,
            TOP_COORDINATE + maxHeight - newHeight,
            width.toFloat(),
            height.toFloat() - newHeight
        )
        icon?.bounds = createIconBounds(topRect, icon!!)
        this.currentHeight = newHeight
        invalidate()
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    /**
     * Calculate icon draw coordinates.
     *
     * @param topRect -- top layer rectangle
     * @param icon -- icon to be drawn
     * @return icon bounds
     */
    private fun createIconBounds(topRect: RectF, icon: Drawable): Rect {
        // calculate icon sizes
        val iconWidth = icon.intrinsicWidth
        val iconHeight = icon.intrinsicHeight
        // calculate icon position to the center of the top layer
        val iconLeft = ceil((topRect.centerX() - iconWidth / 2)).toInt()
        val iconTop = ceil((topRect.centerY() - iconHeight / 2)).toInt()
        val iconRight = iconLeft + iconWidth
        val iconBottom = iconTop + iconHeight
        return Rect(iconLeft, iconTop, iconRight, iconBottom)
    }

    /**
     * Animate soft-press to the button.
     *
     * @param pressScaleFactor -- 0.0f - 1.0f, 0.0f is no press, 1.0f is full press
     */
    fun press(pressScaleFactor: Float) {
        // set maximum allowed height depending on the lock state
        // calculate end height after the press
        val scaleFactor = currentMaxHeight - (pressScaleFactor * currentMaxHeight)
        // animate to the new height, duration depends on the press factor
        val animator = getAnimator(currentHeight, scaleFactor, currentMaxHeight)
        animator.duration = ANIMATION_DURATION
        animator.interpolator = AccelerateDecelerateInterpolator()
        animator.start()
    }

    /**
     * Animate full-press to the button.
     *
     * Call pressRelease() after this to release the button.
     */
    fun pressHold() {
        isPressed = true
        val animator = getAnimator(currentHeight, 0f)
        animator.duration = ANIMATION_DURATION / 2
        animator.interpolator = DecelerateInterpolator()
        animator.start()
    }

    /**
     * Animate release-press to the button.
     *
     * Call this after pressHold() to animate button release.
     */
    fun pressRelease() {
        isPressed = false
        val animator = getAnimator(currentHeight, currentMaxHeight)
        animator.duration = ANIMATION_DURATION
        animator.interpolator = AccelerateDecelerateInterpolator()
        animator.start()
    }

    /**
     * Set the button to locked or unlocked state.
     *
     * @param isLocked -- true if the button is locked, false otherwise
     */
    fun lock(isLocked: Boolean) {
        currentMaxHeight = if (isLocked) {
            maxHeight * lockScaleFactor
        } else {
            maxHeight
        }
    }
}