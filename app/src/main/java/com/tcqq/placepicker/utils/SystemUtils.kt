package com.tcqq.placepicker.utils

import android.content.Context
import android.view.View

/**
 * @author Perry Lance
 * @since 16/10/2018 Created
 */
object SystemUtils {

    fun isRtl(context: Context): Boolean {
        val config = context.resources.configuration
        return config.layoutDirection == View.LAYOUT_DIRECTION_RTL
    }
}