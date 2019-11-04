package com.tcqq.placepicker.utils

import android.content.Context
import android.location.Location
import java.text.NumberFormat


/**
 * @author Alan Perry
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

    /**
     * Values of latitude and longitude to value of Degree-Minute-Second(DMS): 41°24'12.2"N 2°10'26.5"E
     */
    fun latLng2DMS(location: Location): String {
        val sb = StringBuilder()
        var lat = location.latitude
        val latDirection: String
        if (lat < 0) {
            lat *= -1.0
            latDirection = "S"
        } else {
            latDirection = "N"
        }

        val degreeAndMinuteFormatter = NumberFormat.getNumberInstance()
        degreeAndMinuteFormatter.minimumFractionDigits = 0
        degreeAndMinuteFormatter.maximumFractionDigits = 0

        val secondFormatter = NumberFormat.getNumberInstance()
        secondFormatter.minimumFractionDigits = 1
        secondFormatter.maximumFractionDigits = 1

        val latDegree = degreeAndMinuteFormatter.format(Math.floor(lat)).toInt()
        val latMinute = degreeAndMinuteFormatter.format(Math.floor((lat - latDegree) * 60)).toInt()
        val latSecond = secondFormatter.format(((lat - latDegree) * 60 - latMinute) * 60)
        var lon = location.longitude
        val lonDirection: String
        if (lon < 0) {
            lon *= -1.0
            lonDirection = "W"
        } else {
            lonDirection = "E"
        }
        val lonDegree = degreeAndMinuteFormatter.format(Math.floor(lon)).toInt()
        val lonMinute = degreeAndMinuteFormatter.format(Math.floor((lon - lonDegree) * 60)).toInt()
        val lonSecond = secondFormatter.format(((lon - lonDegree) * 60 - lonMinute) * 60)
        sb.append(latDegree)
        sb.append('\u00B0')
        insertText(sb, latMinute, latSecond)
        sb.append(latDirection)
        sb.append(" ")
        sb.append(degreeAndMinuteFormatter.format(lonDegree))
        sb.append('\u00B0')
        insertText(sb, lonMinute, lonSecond)
        sb.append(lonDirection)
        return sb.toString()
    }

    private fun insertText(sb: StringBuilder, minute: Int,
                           second: String) {
        sb.append(minute)
        sb.append("'")
        sb.append(second)
        sb.append("\"")
    }
}