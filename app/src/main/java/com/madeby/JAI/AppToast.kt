package com.madeby.JAI

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast

/**
 * Universal Toast manager that prevents toast queuing and debounces rapid taps.
 *
 * If a user taps an action repeatedly (e.g., clicking 10 times to switch timer mode),
 * standard Android Toast puts 10 toasts into the system queue, displaying them one after
 * another for 20-30 seconds.
 *
 * AppToast guarantees:
 * 1. Immediate cancellation of any currently showing toast, so toasts never queue.
 * 2. Debouncing identical toast messages within 1.5s so rapid repetitive clicks don't flood.
 * 3. Safe invocation from any thread (dispatched to UI thread via Looper.getMainLooper()).
 */
object AppToast {
    @Volatile
    private var activeToast: Toast? = null
    @Volatile
    private var lastMessage: String? = null
    @Volatile
    private var lastShownTime: Long = 0L
    private val mainHandler = Handler(Looper.getMainLooper())

    fun show(context: Context?, message: CharSequence?, duration: Int = Toast.LENGTH_SHORT) {
        if (context == null || message.isNullOrBlank()) return

        val msgStr = message.toString()
        val now = System.currentTimeMillis()

        // Debounce identical toast within 1.5 seconds so rapid spam clicks are ignored
        if (msgStr == lastMessage && (now - lastShownTime) < 1500L) {
            return
        }

        val appContext = context.applicationContext

        val action = Runnable {
            try {
                // Cancel previous toast immediately so messages never queue
                activeToast?.cancel()

                val toast = Toast.makeText(appContext, msgStr, duration)
                activeToast = toast
                lastMessage = msgStr
                lastShownTime = now
                toast.show()
            } catch (_: Exception) {
                // Ignore any window/token exceptions on background context
            }
        }

        if (Looper.myLooper() == Looper.getMainLooper()) {
            action.run()
        } else {
            mainHandler.post(action)
        }
    }

    fun show(context: Context?, resId: Int, duration: Int = Toast.LENGTH_SHORT) {
        if (context == null) return
        try {
            show(context, context.getString(resId), duration)
        } catch (_: Exception) {}
    }

    fun cancel() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            try {
                activeToast?.cancel()
            } catch (_: Exception) {}
            activeToast = null
        } else {
            mainHandler.post {
                try {
                    activeToast?.cancel()
                } catch (_: Exception) {}
                activeToast = null
            }
        }
    }
}

/**
 * Convenient Context extension functions to show debounced, non-queuing toasts.
 */
fun Context?.showAppToast(message: CharSequence?, duration: Int = Toast.LENGTH_SHORT) {
    AppToast.show(this, message, duration)
}

fun Context?.showAppToast(resId: Int, duration: Int = Toast.LENGTH_SHORT) {
    AppToast.show(this, resId, duration)
}
