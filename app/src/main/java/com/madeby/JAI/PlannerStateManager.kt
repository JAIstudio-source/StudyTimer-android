package com.madeby.JAI

import android.os.Handler
import android.os.Looper
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Reactive state manager for Planner goals.
 * Emits live updates whenever goals are added, deleted, edited, or checked,
 * so the UI immediately updates in real-time without requiring tab switches or app restarts.
 */
object PlannerStateManager {

    fun interface GoalChangeListener {
        fun onGoalsChanged()
    }

    private val listeners = CopyOnWriteArrayList<GoalChangeListener>()
    private val mainHandler = Handler(Looper.getMainLooper())

    fun addListener(listener: GoalChangeListener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener)
        }
    }

    fun removeListener(listener: GoalChangeListener) {
        listeners.remove(listener)
    }

    fun notifyGoalsChanged() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            for (listener in listeners) {
                try {
                    listener.onGoalsChanged()
                } catch (_: Exception) {}
            }
        } else {
            mainHandler.post {
                for (listener in listeners) {
                    try {
                        listener.onGoalsChanged()
                    } catch (_: Exception) {}
                }
            }
        }
    }
}
