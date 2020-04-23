package com.tcqq.placepicker.items

import android.view.View
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.RecyclerView
import com.tcqq.placepicker.R
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.AbstractFlexibleItem
import eu.davidea.flexibleadapter.items.IFlexible
import eu.davidea.viewholders.FlexibleViewHolder

/**
 * @author Perry Lance
 * @since 2018/10/23 Created
 */
data class AutocompleteItem(val id: String,
                            val placeName: String,
                            val placeAddress: String,
                            val latitude: Double,
                            val longitude: Double) : AbstractFlexibleItem<AutocompleteItem.ViewHolder>() {

    override fun getLayoutRes(): Int {
        return R.layout.item_autocomplete
    }

    override fun createViewHolder(view: View, adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>): ViewHolder {
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
     * Used for autocomplete.
     */
    class ViewHolder(view: View, adapter: FlexibleAdapter<*>) : FlexibleViewHolder(view, adapter) {
        var placeName: AppCompatTextView = view.findViewById(R.id.place_name)
        var placeAddress: AppCompatTextView = view.findViewById(R.id.place_address)
    }
}
