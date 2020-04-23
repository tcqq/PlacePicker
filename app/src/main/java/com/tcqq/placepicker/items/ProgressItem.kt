package com.tcqq.placepicker.items

import android.view.View
import android.widget.ProgressBar
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.RecyclerView
import com.tcqq.placepicker.R
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.AbstractFlexibleItem
import eu.davidea.flexibleadapter.items.IFlexible
import eu.davidea.viewholders.FlexibleViewHolder

/**
 * @author Perry Lance
 * @since 2018-10-23 Created
 */
data class ProgressItem(var status: StatusEnum = StatusEnum.MORE_TO_LOAD) : AbstractFlexibleItem<ProgressItem.ViewHolder>() {

    override fun getLayoutRes(): Int {
        return R.layout.item_progress
    }

    override fun createViewHolder(view: View, adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>): ViewHolder {
        return ViewHolder(view, adapter)
    }

    override fun bindViewHolder(adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>, holder: ViewHolder, position: Int, payloads: MutableList<Any>) {
        when (status) {
            StatusEnum.MORE_TO_LOAD -> {
                holder.errorMessageText.visibility = View.GONE
                holder.loadProgress.visibility = View.VISIBLE
            }
            StatusEnum.ON_ERROR -> {
                holder.errorMessageText.text = "加载失败，点击重试"
                holder.errorMessageText.visibility = View.VISIBLE
                holder.loadProgress.visibility = View.GONE
            }
        }
    }

    class ViewHolder(view: View, adapter: FlexibleAdapter<*>) : FlexibleViewHolder(view, adapter) {
        var loadProgress: ProgressBar = view.findViewById(R.id.load_progress)
        var errorMessageText: AppCompatTextView = view.findViewById(R.id.error_message_text)
    }

    enum class StatusEnum {
        MORE_TO_LOAD,
        ON_ERROR,
    }
}