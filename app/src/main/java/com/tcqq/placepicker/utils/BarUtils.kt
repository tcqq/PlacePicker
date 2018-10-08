package com.tcqq.placepicker.utils

import android.app.Activity
import android.content.res.Resources
import android.graphics.Color
import android.os.Build
import android.view.View
import android.view.WindowManager

/**
 * @author Alan Dreamer
 * @since 02/06/2018 Created
 */
object BarUtils {

    fun transparentStatusBar(activity: Activity) {
        if (hasTransparentStatusBar()) {
            val window = activity.window
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor = Color.TRANSPARENT
        }
    }

    fun darkIconsStatusBar(activity: Activity, darkIcons: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val decor = activity.window.decorView
            if (darkIcons) {
                decor.systemUiVisibility = decor.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            } else {
                // We want to change tint color to white again.
                // You can also record the flags in advance so that you can turn UI back completely if
                // you have set other flags before, such as translucent or full screen.
                decor.systemUiVisibility = 0
            }
        }
    }

    /**
     * Return the status bar's height.
     */
    fun getStatusBarHeight(): Int {
        val resources = Resources.getSystem()
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        return resources.getDimensionPixelSize(resourceId)
    }

    /**
     * Whether to support transparent status bar.
     */
    fun hasTransparentStatusBar(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
    }

    ///////////////////////////////////////////////////////////////////////////
    // navigation bar
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Return the navigation bar's height.
     *
     * @return the navigation bar's height
     */
    fun getNavBarHeight(): Int {
        val res = Resources.getSystem()
        val resourceId = res.getIdentifier("navigation_bar_height", "dimen", "android")
        return if (resourceId != 0) {
            res.getDimensionPixelSize(resourceId)
        } else {
            0
        }
    }
}