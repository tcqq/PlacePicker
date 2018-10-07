package com.tcqq.placepicker

import android.content.res.Configuration
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.tcqq.placepicker.utils.AutoUtils
import com.tcqq.placepicker.utils.BarUtils
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.AbstractFlexibleItem
import eu.davidea.flexibleadapter.items.IFlexible
import eu.davidea.viewholders.FlexibleViewHolder

/**
 * @author Alan Dreamer
 * @since 26/09/2018 Created
 */
data class NearbyPlacesHeaderItem(val id: String) : AbstractFlexibleItem<NearbyPlacesHeaderItem.ViewHolder>() {

    override fun getLayoutRes(): Int {
        return R.layout.item_nearby_places_header
    }

    override fun createViewHolder(view: View, adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>): ViewHolder {
        AutoUtils.auto(view)
        return ViewHolder(view, adapter)
    }

    override fun bindViewHolder(adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>?, holder: ViewHolder?, position: Int, payloads: MutableList<Any>?) {
        if (holder != null) {
            val context = holder.itemView.context
            if (context.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                holder.root.layoutParams.height = BarUtils.hasTransparentStatusBar().let {
                    if (it) BarUtils.getStatusBarHeight() + AutoUtils.getDisplayHeightValue(147)
                    else AutoUtils.getDisplayHeightValue(147)
                }
            } else {
                holder.root.layoutParams.height = BarUtils.hasTransparentStatusBar().let {
                    if (it) BarUtils.getStatusBarHeight() + AutoUtils.getDisplayHeightValue(225)
                    else AutoUtils.getDisplayHeightValue(225)
                }
            }
        }
    }

    class ViewHolder(view: View, adapter: FlexibleAdapter<*>) : FlexibleViewHolder(view, adapter) {
        var root: ConstraintLayout = view.findViewById(R.id.root)
    }
}
