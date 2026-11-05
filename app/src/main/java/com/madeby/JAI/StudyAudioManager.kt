package com.madeby.JAI

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log

data class ChimeToneItem(
    val id: String,
    val displayName: String,
    val description: String,
    val rawResId: Int
)

data class AmbientSoundItem(
    val id: String,
    val displayName: String,
    val iconEmoji: String,
    val description: String,
    val rawResId: Int
)

/**
 * Clean, centralized audio controller for:
 * 1. Timer Completion Chimes (gentle bells when focus/break ends)
 * 2. Ambient Focus Sounds (calming background loops: rain, fire, ocean, etc.)
 *
 * Designed with simple, clear wording and zero jargon.
 */
object StudyAudioManager {

    private const val TAG = "StudyAudioManager"
    private const val PREFS_NAME = "StudyTimerPrefs"

    // Preference Keys
    private const val KEY_CHIME_ENABLED = "sound_chime_enabled"
    private const val KEY_VIBRATE_ENABLED = "sound_vibrate_enabled"
    private const val KEY_CHIME_CHOICE = "sound_chime_choice"
    private const val KEY_AMBIENT_ENABLED = "sound_ambient_enabled"
    private const val KEY_AMBIENT_CHOICE = "sound_ambient_choice"
    private const val KEY_AMBIENT_VOLUME = "sound_ambient_volume"

    // Available Completion Chimes
    val CHIME_OPTIONS = listOf(
        ChimeToneItem("zen", "Gentle Zen", "Calm meditation bell", R.raw.timer_bell_zen),
        ChimeToneItem("soft", "Soft Bell", "Light acoustic pulse", R.raw.timer_bell_soft),
        ChimeToneItem("classic", "Classic Chime", "Clear study chime", R.raw.timer_bell_classic),
        ChimeToneItem("harmony", "Marimba Harmony", "Uplifting notes", R.raw.timer_bell_harmony),
        ChimeToneItem("pulse", "Digital Pulse", "Modern double chime", R.raw.timer_bell_pulse)
    )

    // Available Ambient Focus Sounds
    val AMBIENT_OPTIONS = listOf(
        AmbientSoundItem("rain", "Gentle Rain", "🌧️", "Raindrops on window", R.raw.ambient_gentle_rain),
        AmbientSoundItem("fire", "Cozy Fireplace", "🔥", "Warm crackling embers", R.raw.ambient_cozy_fire),
        AmbientSoundItem("ocean", "Ocean Waves", "🌊", "Rhythmic sea waves", R.raw.ambient_ocean_waves),
        AmbientSoundItem("forest", "Forest Breeze", "🌲", "Woodland air & birds", R.raw.ambient_forest_breeze),
        AmbientSoundItem("stream", "Mountain Stream", "💧", "Flowing clear water", R.raw.ambient_mountain_stream),
        AmbientSoundItem("clock", "Clock Ticking", "⏱️", "Steady focus tempo", R.raw.ambient_clock_tick),
        AmbientSoundItem("pencil", "Pencil on Paper", "✏️", "Quiet study room writing", R.raw.ambient_pencil_scratch)
    )

    // Media players
    @Volatile
    private var ambientPlayer: MediaPlayer? = null
    @Volatile
    private var previewPlayer: MediaPlayer? = null
    @Volatile
    private var chimePlayer: MediaPlayer? = null

    // =========================================================================
    // Preference Helpers
    // =========================================================================

    fun isChimeEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_CHIME_ENABLED, true)
    }

    fun setChimeEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_CHIME_ENABLED, enabled).apply()
    }

    fun isVibrateEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_VIBRATE_ENABLED, true)
    }

    fun setVibrateEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_VIBRATE_ENABLED, enabled).apply()
    }

    fun getSelectedChime(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_CHIME_CHOICE, "zen") ?: "zen"
    }

    fun setSelectedChime(context: Context, chimeId: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_CHIME_CHOICE, chimeId).apply()
    }

    fun isAmbientEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_AMBIENT_ENABLED, false)
    }

    fun setAmbientEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_AMBIENT_ENABLED, enabled).apply()
    }

    fun getSelectedAmbient(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_AMBIENT_CHOICE, "rain") ?: "rain"
    }

    fun setSelectedAmbient(context: Context, soundId: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_AMBIENT_CHOICE, soundId).apply()
    }

    fun getAmbientVolume(context: Context): Float {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getFloat(KEY_AMBIENT_VOLUME, 0.65f).coerceIn(0.05f, 1.0f)
    }

    fun setAmbientVolume(context: Context, volume: Float) {
        val clamped = volume.coerceIn(0.05f, 1.0f)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putFloat(KEY_AMBIENT_VOLUME, clamped).apply()
        try {
            ambientPlayer?.setVolume(clamped, clamped)
        } catch (_: Exception) {}
    }

    // =========================================================================
    // Completion Chime Playback
    // =========================================================================

    /**
     * Plays the completion chime if enabled by the user.
     */
    fun playCompletionChime(context: Context) {
        if (!isChimeEnabled(context)) return

        val chimeId = getSelectedChime(context)
        val item = CHIME_OPTIONS.find { it.id == chimeId } ?: CHIME_OPTIONS[0]

        try {
            chimePlayer?.release()
            chimePlayer = null

            val mp = MediaPlayer.create(context.applicationContext, item.rawResId) ?: return
            mp.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            mp.setOnCompletionListener {
                try {
                    it.release()
                } catch (_: Exception) {}
                if (chimePlayer == it) chimePlayer = null
            }
            chimePlayer = mp
            mp.start()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play completion chime", e)
        }
    }

    /**
     * Plays a brief audio preview when user selects a chime in settings.
     */
    fun playChimePreview(context: Context, chimeId: String) {
        val item = CHIME_OPTIONS.find { it.id == chimeId } ?: return
        try {
            previewPlayer?.release()
            previewPlayer = null

            val mp = MediaPlayer.create(context.applicationContext, item.rawResId) ?: return
            mp.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            mp.setOnCompletionListener {
                try {
                    it.release()
                } catch (_: Exception) {}
                if (previewPlayer == it) previewPlayer = null
            }
            previewPlayer = mp
            mp.start()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play chime preview", e)
        }
    }

    /**
     * Plays celebratory applause on milestone or goal achievement.
     */
    fun playCelebrationApplause(context: Context) {
        if (!isChimeEnabled(context)) return
        try {
            val mp = MediaPlayer.create(context.applicationContext, R.raw.celebration_applause) ?: return
            mp.setOnCompletionListener {
                try {
                    it.release()
                } catch (_: Exception) {}
            }
            mp.start()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play celebration applause", e)
        }
    }

    // =========================================================================
    // Ambient Focus Soundscapes
    // =========================================================================

    /**
     * Starts looping the selected background focus sound if enabled.
     */
    @Synchronized
    fun startAmbient(context: Context) {
        if (!isAmbientEnabled(context)) {
            stopAmbient()
            return
        }

        val soundId = getSelectedAmbient(context)
        val soundItem = AMBIENT_OPTIONS.find { it.id == soundId } ?: AMBIENT_OPTIONS[0]
        val volume = getAmbientVolume(context)

        try {
            // If already playing the same sound, just ensure volume and playing
            if (ambientPlayer != null) {
                try {
                    if (ambientPlayer?.isPlaying == true) {
                        ambientPlayer?.setVolume(volume, volume)
                        return
                    } else {
                        ambientPlayer?.start()
                        ambientPlayer?.setVolume(volume, volume)
                        return
                    }
                } catch (_: Exception) {
                    stopAmbient()
                }
            }

            val mp = MediaPlayer.create(context.applicationContext, soundItem.rawResId) ?: return
            mp.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            mp.isLooping = true
            mp.setVolume(volume, volume)
            ambientPlayer = mp
            mp.start()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start ambient focus sound", e)
            stopAmbient()
        }
    }

    /**
     * Pauses ambient sound (e.g. when study timer is paused).
     */
    @Synchronized
    fun pauseAmbient() {
        try {
            if (ambientPlayer?.isPlaying == true) {
                ambientPlayer?.pause()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to pause ambient sound", e)
        }
    }

    /**
     * Resumes ambient sound (e.g. when study timer resumes).
     */
    @Synchronized
    fun resumeAmbient(context: Context) {
        if (!isAmbientEnabled(context)) return
        try {
            if (ambientPlayer != null && ambientPlayer?.isPlaying == false) {
                val vol = getAmbientVolume(context)
                ambientPlayer?.setVolume(vol, vol)
                ambientPlayer?.start()
            } else if (ambientPlayer == null) {
                startAmbient(context)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to resume ambient sound", e)
            startAmbient(context)
        }
    }

    /**
     * Stops and releases ambient sound playback completely.
     */
    @Synchronized
    fun stopAmbient() {
        try {
            ambientPlayer?.apply {
                if (isPlaying) {
                    stop()
                }
                release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop ambient sound", e)
        } finally {
            ambientPlayer = null
        }
    }

    /**
     * Changes the background sound and immediately plays the new loop if currently active.
     */
    @Synchronized
    fun changeAmbientSound(context: Context, soundId: String) {
        setSelectedAmbient(context, soundId)
        if (isAmbientEnabled(context)) {
            stopAmbient()
            startAmbient(context)
        }
    }

    fun isPlayingAmbient(): Boolean {
        return try {
            ambientPlayer?.isPlaying == true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Plays a brief celebratory cheer/applause sound when a study goal or milestone is achieved.
     * Respects the user's master sound toggle.
     */
    fun playCelebrationSound(context: Context) {
        if (!isChimeEnabled(context)) return
        try {
            val player = MediaPlayer.create(context.applicationContext, R.raw.celebration_applause)
            player.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            player.setOnCompletionListener {
                try {
                    it.release()
                } catch (_: Exception) {}
            }
            player.start()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play celebration sound", e)
        }
    }
}

