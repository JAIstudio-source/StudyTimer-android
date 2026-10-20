package com.madeby.JAI

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import org.json.JSONArray
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

data class WallpaperPreset(
    val id: String,
    val title: String,
    val category: String,
    val assetPath: String,
    val thumbAssetPath: String,
    val recommendedDim: Float = 0.45f
)

object WallpaperManager {
    private const val PREFS_NAME = "StudyTimerPrefs"
    const val TYPE_NONE = "NONE"
    const val TYPE_PRESET = "PRESET"
    const val TYPE_STATIC = "STATIC"
    const val TYPE_LIVE_AURORA = "LIVE_AURORA"
    const val TYPE_LIVE_COSMIC = "LIVE_COSMIC"
    const val TYPE_LIVE_LOFI_RAIN = "LIVE_LOFI_RAIN"
    const val TYPE_LIVE_AMBER_GLOW = "LIVE_AMBER_GLOW"
    const val TYPE_LIVE_VIDEO = "LIVE_VIDEO"

    private const val KEY_WALLPAPER_TYPE = "wallpaper_type"
    private const val KEY_PRESET_ID = "wallpaper_preset_id"
    private const val KEY_DIM_ALPHA = "wallpaper_dim_alpha"
    private const val KEY_USER_SET_DIM = "wallpaper_user_set_dim"
    private const val WALLPAPER_FILE_NAME = "custom_study_wallpaper.jpg"
    private const val LIVE_VIDEO_FILE_NAME = "custom_live_video.mp4"

    private val thumbCache = mutableMapOf<String, Bitmap>()
    private var cachedPresetBitmap: Pair<String, Bitmap>? = null

    // Fallback preset list in case JSON parsing fails
    private val DEFAULT_PRESETS = listOf(
        WallpaperPreset("cozy_cat", "Cozy Cat", "Lofi & Chill", "wallpapers/cozy_cat.webp", "wallpapers/thumbs/cozy_cat.webp", 0.40f),
        WallpaperPreset("dark_night", "Dark Night Sky", "Astronomy", "wallpapers/dark_night.webp", "wallpapers/thumbs/dark_night.webp", 0.35f),
        WallpaperPreset("studying_desk", "Study Desk", "Lofi Study", "wallpapers/studying_desk.webp", "wallpapers/thumbs/studying_desk.webp", 0.40f),
        WallpaperPreset("tokyo_mountain", "Tokyo Mountain", "Landscape", "wallpapers/tokyo_mountain.webp", "wallpapers/thumbs/tokyo_mountain.webp", 0.40f),
        WallpaperPreset("dense_forest", "Dense Forest", "Nature", "wallpapers/dense_forest.webp", "wallpapers/thumbs/dense_forest.webp", 0.40f),
        WallpaperPreset("peaceful_field", "Peaceful Field", "Nature", "wallpapers/peaceful_field.webp", "wallpapers/thumbs/peaceful_field.webp", 0.45f),
        WallpaperPreset("bright_day", "Bright Day", "Nature", "wallpapers/bright_day.webp", "wallpapers/thumbs/bright_day.webp", 0.50f),
        WallpaperPreset("bright_sky", "Bright Sky", "Sky", "wallpapers/bright_sky.webp", "wallpapers/thumbs/bright_sky.webp", 0.50f),
        WallpaperPreset("tranquil_water", "Tranquil Water", "Minimal", "wallpapers/tranquil_water.webp", "wallpapers/thumbs/tranquil_water.webp", 0.45f),
        WallpaperPreset("wonderful_sky", "Wonderful Sky", "Sky", "wallpapers/wonderful_sky.webp", "wallpapers/thumbs/wonderful_sky.webp", 0.45f)
    )

    fun getWallpaperType(context: Context): String {
        val type = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_WALLPAPER_TYPE, TYPE_LIVE_AURORA) ?: TYPE_LIVE_AURORA
        // Graceful migration: remove deprecated unappealing Amber theme
        if (type == TYPE_LIVE_AMBER_GLOW) {
            setWallpaperType(context, TYPE_LIVE_AURORA)
            return TYPE_LIVE_AURORA
        }
        return type
    }

    fun isLiveWallpaper(type: String): Boolean {
        return type.startsWith("LIVE_")
    }

    fun isLiveWallpaper(context: Context): Boolean {
        return isLiveWallpaper(getWallpaperType(context))
    }

    fun setWallpaperType(context: Context, type: String) {
        val targetType = if (type == TYPE_LIVE_AMBER_GLOW) TYPE_LIVE_AURORA else type
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_WALLPAPER_TYPE, targetType)
            .apply()
    }

    fun getActivePresetId(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_PRESET_ID, "cozy_cat") ?: "cozy_cat"
    }

    fun setActivePreset(context: Context, presetId: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_WALLPAPER_TYPE, TYPE_PRESET)
            .putString(KEY_PRESET_ID, presetId)
            .apply()
    }

    fun getWallpaperDimAlpha(context: Context): Float {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getFloat(KEY_DIM_ALPHA, 0.42f)
    }

    fun setWallpaperDimAlpha(context: Context, alpha: Float, isUserExplicit: Boolean = true) {
        val clamped = alpha.coerceIn(0.15f, 0.85f)
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putFloat(KEY_DIM_ALPHA, clamped)
            .putBoolean(KEY_USER_SET_DIM, isUserExplicit)
            .apply()
    }

    fun hasUserCustomDimAlpha(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_USER_SET_DIM, false)
    }

    fun getPresetCatalog(context: Context): List<WallpaperPreset> {
        return try {
            val jsonString = context.assets.open("wallpapers/presets.json").bufferedReader().use { it.readText() }
            val jsonArray = JSONArray(jsonString)
            val list = mutableListOf<WallpaperPreset>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    WallpaperPreset(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        category = obj.optString("category", "General"),
                        assetPath = obj.getString("assetPath"),
                        thumbAssetPath = obj.getString("thumbAssetPath"),
                        recommendedDim = obj.optDouble("recommendedDim", 0.45).toFloat()
                    )
                )
            }
            if (list.isNotEmpty()) list else DEFAULT_PRESETS
        } catch (_: Exception) {
            DEFAULT_PRESETS
        }
    }

    fun getPresetById(context: Context, presetId: String): WallpaperPreset? {
        return getPresetCatalog(context).find { it.id == presetId }
    }

    fun loadPresetThumbnail(context: Context, preset: WallpaperPreset): Bitmap? {
        thumbCache[preset.id]?.let { return it }
        // 1. Attempt loading dedicated thumbnail with BufferedInputStream for reliable WebP decoding
        try {
            val stream = java.io.BufferedInputStream(context.assets.open(preset.thumbAssetPath))
            val bm = BitmapFactory.decodeStream(stream)
            stream.close()
            if (bm != null) {
                thumbCache[preset.id] = bm
                return bm
            }
        } catch (_: Exception) {}

        // 2. Reliable fallback: decode downsampled full asset image
        return try {
            val stream = java.io.BufferedInputStream(context.assets.open(preset.assetPath))
            val opts = BitmapFactory.Options().apply { inSampleSize = 4 }
            val bm = BitmapFactory.decodeStream(stream, null, opts)
            stream.close()
            if (bm != null) {
                thumbCache[preset.id] = bm
            }
            bm
        } catch (_: Exception) {
            null
        }
    }

    fun loadPresetBitmap(context: Context, presetId: String): Bitmap? {
        val cached = cachedPresetBitmap
        if (cached != null && cached.first == presetId) {
            return cached.second
        }

        val preset = getPresetById(context, presetId) ?: return null
        return try {
            val stream = java.io.BufferedInputStream(context.assets.open(preset.assetPath))
            val bm = BitmapFactory.decodeStream(stream)
            stream.close()
            if (bm != null) {
                cachedPresetBitmap = Pair(presetId, bm)
            }
            bm
        } catch (_: Exception) {
            null
        }
    }

    fun getCustomWallpaperFile(context: Context): File {
        return File(context.filesDir, WALLPAPER_FILE_NAME)
    }

    fun getCustomLiveVideoFile(context: Context): File {
        return File(context.filesDir, LIVE_VIDEO_FILE_NAME)
    }

    fun saveCustomWallpaperFromUri(context: Context, uri: Uri): Boolean {
        return try {
            val input = context.contentResolver.openInputStream(uri) ?: return false
            val file = getCustomWallpaperFile(context)
            FileOutputStream(file).use { out ->
                input.copyTo(out)
            }
            input.close()
            setWallpaperType(context, TYPE_STATIC)
            true
        } catch (_: Exception) {
            false
        }
    }

    enum class VideoSaveResult {
        SUCCESS,
        EXCEEDS_DURATION,
        ERROR
    }

    fun saveCustomLiveVideoFromUri(context: Context, uri: Uri): VideoSaveResult {
        return try {
            val tempFile = File(context.cacheDir, "temp_live_video_${System.currentTimeMillis()}.mp4")
            val input = context.contentResolver.openInputStream(uri) ?: return VideoSaveResult.ERROR
            FileOutputStream(tempFile).use { out ->
                input.copyTo(out)
            }
            input.close()

            // Verify duration (max 20 seconds to prevent device lag/OOM)
            val retriever = android.media.MediaMetadataRetriever()
            var durationMs = 0L
            try {
                try {
                    retriever.setDataSource(context, uri)
                } catch (_: Exception) {
                    retriever.setDataSource(tempFile.absolutePath)
                }
                val durationStr = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
                durationMs = durationStr?.toLongOrNull() ?: 0L
            } catch (e: Exception) {
                android.util.Log.w("WallpaperManager", "Could not extract video duration, fallback to size check", e)
            } finally {
                try { retriever.release() } catch (_: Exception) {}
            }

            if (durationMs > 20_500L) {
                tempFile.delete()
                return VideoSaveResult.EXCEEDS_DURATION
            }

            // Size guard: reject videos larger than 35MB to prevent memory issues
            if (tempFile.length() > 35 * 1024 * 1024) {
                tempFile.delete()
                return VideoSaveResult.EXCEEDS_DURATION
            }

            val destFile = getCustomLiveVideoFile(context)
            if (destFile.exists()) destFile.delete()
            if (!tempFile.renameTo(destFile)) {
                tempFile.inputStream().use { inStream ->
                    FileOutputStream(destFile).use { outStream ->
                        inStream.copyTo(outStream)
                    }
                }
                tempFile.delete()
            }
            setWallpaperType(context, TYPE_LIVE_VIDEO)
            VideoSaveResult.SUCCESS
        } catch (_: Exception) {
            VideoSaveResult.ERROR
        }
    }

    fun clearWallpaper(context: Context) {
        setWallpaperType(context, TYPE_NONE)
        try {
            val f = getCustomWallpaperFile(context)
            if (f.exists()) f.delete()
        } catch (_: Exception) {}
        try {
            val vf = getCustomLiveVideoFile(context)
            if (vf.exists()) vf.delete()
        } catch (_: Exception) {}
    }

    /**
     * Builds a full-screen FrameLayout displaying the active theme background, custom/preset wallpaper,
     * and matching dim scrim layer.
     * Used by full-screen dialogs and modals to maintain consistent edge-to-edge wallpaper presentation.
     */
    fun buildWallpaperContainer(context: Context, themeCoordinator: ThemeCoordinator): FrameLayout {
        val root = FrameLayout(context).apply {
            background = themeCoordinator.createBackgroundDrawable()
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        }
        val type = getWallpaperType(context)
        val hasWallpaper = type != TYPE_NONE

        if (type == TYPE_PRESET) {
            val presetId = getActivePresetId(context)
            val bm = loadPresetBitmap(context, presetId)
            if (bm != null) {
                val iv = ImageView(context).apply {
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
                    setImageBitmap(bm)
                }
                root.addView(iv)
            }
        } else if (type == TYPE_STATIC) {
            val file = getCustomWallpaperFile(context)
            if (file.exists()) {
                val bm = BitmapFactory.decodeFile(file.absolutePath)
                if (bm != null) {
                    val iv = ImageView(context).apply {
                        scaleType = ImageView.ScaleType.CENTER_CROP
                        layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
                        setImageBitmap(bm)
                    }
                    root.addView(iv)
                }
            }
        } else if (type.startsWith("LIVE_")) {
            val liveView = LiveWallpaperView(context).apply {
                layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
                setWallpaperMode(type)
                resume()
            }
            root.addView(liveView)
        }

        // Matching Dim Scrim Layer
        if (hasWallpaper) {
            val dimAlpha = getWallpaperDimAlpha(context)
            val scrim = View(context).apply {
                layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
                setBackgroundColor(Color.argb((dimAlpha * 255).toInt(), 0, 0, 0))
            }
            root.addView(scrim)
        }

        return root
    }

    /**
     * Samples the central screen area behind the timer to calculate perceived relative luminance.
     * Takes into account the scrim dim factor.
     */
    fun isBackgroundUnderTimerBright(context: Context): Boolean {
        val type = getWallpaperType(context)
        if (type == TYPE_NONE) {
            val theme = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString("activeBgMode", "OLED")
            return theme == "LIGHT"
        }

        val dimAlpha = getWallpaperDimAlpha(context)
        // If dim scrim is >= 40%, the background is guaranteed dark enough for white text
        if (dimAlpha >= 0.40f) {
            return false
        }

        val sampleBitmap: Bitmap? = when (type) {
            TYPE_PRESET -> {
                val preset = getPresetById(context, getActivePresetId(context))
                if (preset != null) {
                    loadPresetThumbnail(context, preset)
                } else null
            }
            TYPE_STATIC -> {
                val file = getCustomWallpaperFile(context)
                if (file.exists()) {
                    val options = BitmapFactory.Options().apply { inSampleSize = 8 }
                    BitmapFactory.decodeFile(file.absolutePath, options)
                } else null
            }
            else -> null
        }

        if (sampleBitmap != null) {
            try {
                val w = sampleBitmap.width
                val h = sampleBitmap.height
                val startX = (w * 0.25f).toInt()
                val endX = (w * 0.75f).toInt()
                val startY = (h * 0.30f).toInt()
                val endY = (h * 0.60f).toInt()

                var totalLum = 0.0
                var count = 0
                val step = 4
                for (x in startX until endX step step) {
                    for (y in startY until endY step step) {
                        val pixel = sampleBitmap.getPixel(x, y)
                        val r = (Color.red(pixel) / 255.0) * (1.0 - dimAlpha)
                        val g = (Color.green(pixel) / 255.0) * (1.0 - dimAlpha)
                        val b = (Color.blue(pixel) / 255.0) * (1.0 - dimAlpha)
                        val lum = 0.2126 * r + 0.7152 * g + 0.0722 * b
                        totalLum += lum
                        count++
                    }
                }
                if (count > 0) {
                    val avgLum = totalLum / count
                    return avgLum > 0.48
                }
            } catch (_: Exception) {}
        }

        return false
    }
}
