package com.tcqq.placepicker.utils

import android.os.Build
import java.util.*

/**
 * @author Alan Dreamer
 * @since 2018/10/08 Created
 */
object RomUtils {

    val isHuaweiRom: Boolean
        get() {
            val manufacturer = Build.MANUFACTURER
            return !manufacturer.isNullOrEmpty() && manufacturer.contains("HUAWEI")
        }

    val isMiuiRom: Boolean
        get() = !getSystemProperty("ro.miui.ui.version.name").isNullOrEmpty()

    val isOppoRom: Boolean
        get() {
            val a = getSystemProperty("ro.product.brand")
            return !a.isNullOrEmpty()
                    && a.toLowerCase(Locale.getDefault()).contains("oppo")
        }

    val isVivoRom: Boolean
        get() {
            val a = getSystemProperty("ro.vivo.os.name")
            return !a.isNullOrEmpty()
                    && a.toLowerCase(Locale.getDefault()).contains("funtouch")
        }

    private fun getSystemProperty(propName: String): String? {
        return SystemProperties[propName, null]
    }
}