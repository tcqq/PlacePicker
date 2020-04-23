package com.tcqq.placepicker.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

/**
 * @author Perry Lance
 * @since 2018-11-25 Created
 */
@Parcelize
data class SelectedLocation(val placeName: String,
                            val latitude: Double,
                            val longitude: Double): Parcelable