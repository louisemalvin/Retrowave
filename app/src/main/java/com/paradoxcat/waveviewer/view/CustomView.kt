package com.paradoxcat.waveviewer.view

import android.animation.ObjectAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.View

/**
 * Base class for custom views.
 *
 * Provides a base for custom views to extend from.
 * Contains shared methods for all custom views.
 */
abstract class CustomView(context: Context, attrs: AttributeSet?) : View(context, attrs) {
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        render()
    }

    protected fun getAnimator(vararg values: Float): ObjectAnimator {
        // unpack varargs with * to array of values
        return ObjectAnimator.ofFloat(
            this,
            "animation",
            *values
        )
    }

    /**
     * Render the view.
     *
     * Re-calculates all the necessary values required for drawing.
     * Call this method when any of the view's properties has changed.
     */
    protected open fun render() {
        // pre-condition check
        if (width==0 || height==0) {
            return
        }
    }

    /**
     * Sets certain properties of the view to animate.
     *
     * Function to be called by ObjectAnimator on animation.start().
     */
    protected abstract fun setAnimation(animationValue : Float)
}