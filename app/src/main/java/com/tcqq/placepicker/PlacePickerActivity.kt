package com.tcqq.placepicker

import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomsheet.BottomSheetBehavior
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.common.SmoothScrollLinearLayoutManager
import eu.davidea.flexibleadapter.items.IFlexible
import kotlinx.android.synthetic.main.activity_place_picker.*


/**
 * @author Alan Dreamer
 * @since 17/09/2018 Created
 */
class PlacePickerActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AutoUtils.setSize(this, !hasTransparentStatusBar(), 1080, 1920)
        setContentView(R.layout.activity_place_picker)
        BarUtils.transparentStatusBar(this)
        AutoUtils.auto(this)

        val behavior = BottomSheetBehavior.from(bottom_sheet)
        if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            behavior.peekHeight = AutoUtils.getDisplayHeightValue(565)
        } else {
            behavior.peekHeight = AutoUtils.getDisplayHeightValue(620)
        }
        initRecyclerView()
    }

    private fun initRecyclerView() {
        val adapter: FlexibleAdapter<IFlexible<*>> = FlexibleAdapter(getNearbyPlacesItems(), this, true)
        recycler_view.layoutManager = SmoothScrollLinearLayoutManager(this)
        recycler_view.adapter = adapter
        recycler_view.setHasFixedSize(true)
    }

    private fun getNearbyPlacesItems(): List<IFlexible<*>> {
        val items = ArrayList<IFlexible<*>>()
        items.add(NearbyPlacesItem("1", "A"))
        items.add(NearbyPlacesItem("2", "B"))
        items.add(NearbyPlacesItem("3", "C"))
        items.add(NearbyPlacesItem("4", "D"))
        items.add(NearbyPlacesItem("5", "E"))
        items.add(NearbyPlacesItem("6", "F"))
        items.add(NearbyPlacesItem("7", "G"))
        items.add(NearbyPlacesItem("8", "H"))
        items.add(NearbyPlacesItem("9", "I"))
        items.add(NearbyPlacesItem("10", "J"))
        items.add(NearbyPlacesItem("11", "K"))
        items.add(NearbyPlacesItem("12", "L"))
        items.add(NearbyPlacesItem("13", "M"))
        items.add(NearbyPlacesItem("14", "N"))
        items.add(NearbyPlacesItem("15", "O"))
        return items
    }

    /**
     * Whether to support transparent status bar.
     */
    private fun hasTransparentStatusBar(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
    }
}