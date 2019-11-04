package com.tcqq.placepicker.activity

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.annotation.MenuRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.tcqq.placepicker.R
import com.tcqq.placepicker.utils.MenuColorize
import com.tcqq.placepicker.viewmodel.FrameViewModel
import com.trello.rxlifecycle3.components.support.RxAppCompatActivity


/**
 * Base Activity class, used to initialize global configuration.
 *
 * @author Alan Perry
 * @since 12/03/2016 Created
 */
abstract class BaseActivity : RxAppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val model = ViewModelProviders.of(this).get(FrameViewModel::class.java)
        model.menuResId.observe(this, Observer<Int> { menuResId ->
            val toolbar = findViewById<Toolbar>(R.id.toolbar)
            toolbar.menu.clear()
            menuInflater.inflate(menuResId, menu)
            MenuColorize.colorMenu(this, menu, ContextCompat.getColor(this, android.R.color.white))
        })
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    fun setMenuFromResource(@MenuRes menuResId: Int) {
        ViewModelProviders.of(this).get(FrameViewModel::class.java).menuResId.value = menuResId
    }

    fun setActionBar(toolbar: Toolbar) {
        setSupportActionBar(toolbar)
        setBackArrow(shouldShowBackArrow())
    }

    fun setActionBarTitle(@StringRes resId: Int) {
        supportActionBar?.setTitle(resId)
    }

    fun shouldShowBackArrow(): Boolean {
        return true
    }

    @SuppressLint("PrivateResource")
    private fun setBackArrow(showBackArrow: Boolean) {
        if (showBackArrow) {
            val actionBar = supportActionBar
            actionBar?.let {
                actionBar.setDisplayHomeAsUpEnabled(true)
                actionBar.setHomeButtonEnabled(true)
                val backArrow = ContextCompat.getDrawable(this, R.drawable.abc_ic_ab_back_material)
                backArrow?.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP)
                actionBar.setHomeAsUpIndicator(backArrow)
            }
        }
    }
}
