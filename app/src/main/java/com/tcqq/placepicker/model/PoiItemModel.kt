package com.tcqq.placepicker.model

import android.os.Parcelable
import com.amap.api.services.core.PoiItem
import kotlinx.android.parcel.Parcelize

/**
 * @author Perry Lance
 * @since 2018-11-18 Created
 */
@Parcelize
data class PoiItemModel(val poiItem: PoiItem) : Parcelable