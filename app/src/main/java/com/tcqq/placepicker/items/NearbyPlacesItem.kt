package com.tcqq.placepicker.items

import android.view.View
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.RecyclerView
import com.tcqq.placepicker.R
import com.tcqq.placepicker.utils.AutoUtils
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.AbstractFlexibleItem
import eu.davidea.flexibleadapter.items.IFlexible
import eu.davidea.viewholders.FlexibleViewHolder

/**
 * @author Alan Dreamer
 * @since 2018/09/22 Created
 */
data class NearbyPlacesItem(val id: String,
                            val placeName: String,
                            val placeAddress: String,
                            val latitude: Double,
                            val longitude: Double) : AbstractFlexibleItem<NearbyPlacesItem.ViewHolder>() {

    override fun getLayoutRes(): Int {
        return R.layout.item_nearby_places
    }

    override fun createViewHolder(view: View, adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>): ViewHolder {
        AutoUtils.auto(view)
        return ViewHolder(view, adapter)
    }

    override fun bindViewHolder(adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>, holder: ViewHolder, position: Int, payloads: MutableList<Any>) {
        holder.placeName.text = placeName
        holder.placeAddress.text = placeAddress
        holder.placeAddress.visibility = placeAddress.isBlank().let {
            if (it) View.GONE
            else View.VISIBLE
        }
    }

    /**
     * Used for nearby places.
     */
    class ViewHolder(view: View, adapter: FlexibleAdapter<*>) : FlexibleViewHolder(view, adapter) {
        var placeName: AppCompatTextView = view.findViewById(R.id.place_name)
        var placeAddress: AppCompatTextView = view.findViewById(R.id.place_address)
    }
}
