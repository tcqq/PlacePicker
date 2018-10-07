package com.tcqq.placepicker.utils

import android.content.Context
import android.content.res.Configuration


/**
 * @author Alan Dreamer
 * @since 27/09/2018 Created
 */
object ScreenUtils {

    /**
     * Return whether screen is portrait.
     */
    fun isPortrait(context: Context): Boolean {
        return context.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
    }
}