package com.tcqq.placepicker.utils

import android.content.Context
import android.util.TypedValue
import androidx.annotation.Nullable

/**
 * @author Alan Dreamer
 * @since 22/07/2018 Created
 */
object ThemeUtils {

    fun getThemeValue(resId: Int, context: Context): Int {
        val value = TypedValue()
        context.theme.resolveAttribute(resId, value, true)
        return value.data
    }
}