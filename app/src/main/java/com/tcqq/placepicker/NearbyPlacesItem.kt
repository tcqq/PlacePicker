package com.tcqq.placepicker

import android.view.View
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.RecyclerView
import com.tcqq.placepicker.utils.AutoUtils
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.AbstractFlexibleItem
import eu.davidea.flexibleadapter.items.IFlexible
import eu.davidea.viewholders.FlexibleViewHolder

/**
 * @author Alan Dreamer
 * @since 22/09/2018 Created
 */
data class NearbyPlacesItem(val id: String,
                            val placeName: String) : AbstractFlexibleItem<NearbyPlacesItem.ViewHolder>() {

    override fun getLayoutRes(): Int {
        return R.layout.item_nearby_places
    }

    override fun createViewHolder(view: View, adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>): ViewHolder {
        AutoUtils.auto(view)
        return ViewHolder(view, adapter)
    }

    override fun bindViewHolder(adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>?, holder: ViewHolder?, position: Int, payloads: MutableList<Any>?) {
        if (holder != null) {
            holder.text.text = placeName
        }
    }

    /**
     * Used for nearby places.
     */
    class ViewHolder(view: View, adapter: FlexibleAdapter<*>) : FlexibleViewHolder(view, adapter) {
        var text: AppCompatTextView = view.findViewById(R.id.text)
    }
}
