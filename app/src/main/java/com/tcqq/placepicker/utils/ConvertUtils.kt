package com.tcqq.placepicker.utils

import android.content.Context


/**
 * @author Alan Dreamer
 * @since 29/07/2018 Created
 */
object ConvertUtils {

    /**
     * Value of dp to value of px.
     */
    fun dp2px(context: Context, dpValue: Float): Int {
        val scale = context.resources.displayMetrics.density
        return (dpValue * scale + 0.5f).toInt()
    }

    /**
     * Value of px to value of dp.
     */
    fun px2dp(context: Context, pxValue: Float): Int {
        val scale = context.resources.displayMetrics.density
        return (pxValue / scale + 0.5f).toInt()
    }
}