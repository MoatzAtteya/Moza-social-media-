package com.example.moza.utils

import android.view.animation.Interpolator

class BubbleInterpolate(val a  : Double , val f : Double) : Interpolator {
    override fun getInterpolation(input: Float): Float {
        return (-1 * Math.pow(Math.E, -input/a) * Math.cos(f * input) +1).toFloat()
    }
}