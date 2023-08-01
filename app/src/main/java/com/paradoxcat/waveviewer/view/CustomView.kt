package com.paradoxcat.waveviewer.view

import android.animation.ObjectAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.View

abstract class CustomView(context: Context, attrs: AttributeSet?) : View(context, attrs) {
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        render()
    }

    fun getAnimator(vararg values: Float): ObjectAnimator {
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
    abstract fun render()

    /**
     * Sets certain properties of the view to animate.
     *
     * Function to be called by ObjectAnimator on animation.start().
     */
    abstract fun setAnimation(animationValue : Float)
}