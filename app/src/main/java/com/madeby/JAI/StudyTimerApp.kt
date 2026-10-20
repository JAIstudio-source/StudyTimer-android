package com.madeby.JAI

import android.app.Application
import android.content.Context
import android.content.res.Configuration

class StudyTimerApp : Application() {
    override fun attachBaseContext(base: Context) {
        val config = Configuration(base.resources.configuration)
        config.fontScale = config.fontScale.coerceIn(0.85f, 1.05f)
        super.attachBaseContext(base.createConfigurationContext(config))
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        newConfig.fontScale = newConfig.fontScale.coerceIn(0.85f, 1.05f)
        super.onConfigurationChanged(newConfig)
    }

    override fun onCreate() {
        super.onCreate()
        CrashReporter.init(this)
        AppAnalytics.init(this)
        NotificationHelper.createAllNotificationChannels(this)
    }
}
