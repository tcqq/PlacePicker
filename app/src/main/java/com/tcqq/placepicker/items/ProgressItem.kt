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
 * @author Alan Dreamer
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
                holder.progressBar.visibility = View.VISIBLE
                holder.progressMessage.visibility = View.GONE
            }
            StatusEnum.ON_ERROR -> {
                holder.progressMessage.text = "加载失败，点击重试"
                holder.progressBar.visibility = View.GONE
                holder.progressMessage.visibility = View.VISIBLE
            }
        }
    }

    class ViewHolder(view: View, adapter: FlexibleAdapter<*>) : FlexibleViewHolder(view, adapter) {
        var progressBar: ProgressBar = view.findViewById(R.id.progress_bar)
        var progressMessage: AppCompatTextView = view.findViewById(R.id.progress_message)
    }

    enum class StatusEnum {
        MORE_TO_LOAD,
        ON_ERROR
    }
}