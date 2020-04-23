package com.tcqq.placepicker.items

import android.view.View
import android.widget.ProgressBar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.tcqq.placepicker.R
import com.tcqq.placepicker.utils.AutoUtils
import com.tcqq.placepicker.utils.BarUtils
import com.tcqq.placepicker.utils.ConvertUtils
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.AbstractFlexibleItem
import eu.davidea.flexibleadapter.items.IFlexible
import eu.davidea.viewholders.FlexibleViewHolder

/**
 * @author Perry Lance
 * @since 2018/09/26 Created
 */
data class NearbyPlacesHeaderItem(val id: String) : AbstractFlexibleItem<NearbyPlacesHeaderItem.ViewHolder>() {

    override fun getLayoutRes(): Int {
        return R.layout.item_nearby_places_header
    }

    override fun createViewHolder(view: View, adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>): ViewHolder {
        AutoUtils.auto(view)
        return ViewHolder(view, adapter)
    }

    override fun bindViewHolder(adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>, holder: ViewHolder, position: Int, payloads: MutableList<Any>) {
        val context = holder.itemView.context
        holder.root.layoutParams.height = BarUtils.hasTransparentStatusBar().let {
            if (it) BarUtils.getStatusBarHeight(context) + ConvertUtils.dp2px(context, 56F)
            else ConvertUtils.dp2px(context, 56F)
        }
    }

    class ViewHolder(view: View, adapter: FlexibleAdapter<*>) : FlexibleViewHolder(view, adapter) {
        var root: ConstraintLayout = view.findViewById(R.id.root)
        var progressbar: ProgressBar = view.findViewById(R.id.progressbar)
    }
}
