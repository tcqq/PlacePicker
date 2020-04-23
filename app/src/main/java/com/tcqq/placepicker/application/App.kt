package com.tcqq.placepicker.application


import android.app.Application
import android.content.Context
import androidx.multidex.MultiDex
import com.tcqq.placepicker.BuildConfig
import timber.log.Timber

/**
 * @author Perry Lance
 * @since 22/10/2016 Created
 */
class App : Application() {

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        MultiDex.install(this)
    }

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}
