package com.berat.mediagrab

import android.app.Application
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MediaGrabApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize Python for Chaquopy
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(this))
        }
    }
}
