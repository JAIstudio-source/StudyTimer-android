package com.madeby.JAI

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class FestivalReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_CHECK_FESTIVAL = "com.madeby.JAI.ACTION_CHECK_FESTIVAL"
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                FestivalScheduler.schedule(context)
                FestivalManager.checkAndNotify(context)
            }
            ACTION_CHECK_FESTIVAL -> {
                FestivalManager.checkAndNotify(context)
                FestivalScheduler.schedule(context)
            }
        }
    }
}

