package com.tcqq.placepicker.application


import android.app.Application
import com.tcqq.placepicker.BuildConfig
import timber.log.Timber

/**
 * @author Alan Dreamer
 * @since 22/10/2016 Created
 */
class App : Application() {

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}
