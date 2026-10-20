package com.madeby.JAI

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.net.Uri
import android.util.AttributeSet
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.widget.FrameLayout
import java.io.File
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * High-performance, battery-conscious Live Wallpaper view.
 * Supports fluid mathematical ambient shaders (Aurora, Cosmic Stars, Lofi Rain, Amber Hearth)
 * and hardware-accelerated looping Video Live Wallpapers.
 */
class LiveWallpaperView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private var currentMode: String = "NONE"
    private var isPlaying = false

    private val canvasView = AmbientCanvasView(context)
    private var videoTextureView: TextureView? = null
    private var mediaPlayer: MediaPlayer? = null
    private var videoFis: java.io.FileInputStream? = null
    private var videoSurface: Surface? = null
    private var isSurfaceAvailable = false

    init {
        addView(canvasView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
    }

    fun setLiveMode(mode: String) = setWallpaperMode(mode)

    fun setWallpaperMode(mode: String) {
        currentMode = mode
        if (mode.startsWith("LIVE_VIDEO")) {
            canvasView.visibility = View.GONE
            canvasView.pause()
            setupVideoPlayer()
        } else if (mode.startsWith("LIVE_")) {
            stopVideoPlayer()
            canvasView.visibility = View.VISIBLE
            canvasView.setPreset(mode)
            if (isPlaying) canvasView.resume()
        } else {
            stopVideoPlayer()
            canvasView.visibility = View.GONE
            canvasView.pause()
        }
    }

    fun resume() {
        isPlaying = true
        if (currentMode.startsWith("LIVE_VIDEO")) {
            try {
                if (mediaPlayer != null) {
                    mediaPlayer?.start()
                } else if (isSurfaceAvailable) {
                    startVideoPlayback()
                }
            } catch (_: Exception) {}
        } else if (currentMode.startsWith("LIVE_")) {
            canvasView.resume()
        }
    }

    fun pause() {
        isPlaying = false
        if (currentMode.startsWith("LIVE_VIDEO")) {
            try {
                if (mediaPlayer?.isPlaying == true) {
                    mediaPlayer?.pause()
                }
            } catch (_: Exception) {}
        }
        canvasView.pause()
    }

    fun release() {
        stopVideoPlayer()
        canvasView.pause()
    }

    private fun setupVideoPlayer() {
        if (videoTextureView == null) {
            val tv = TextureView(context)
            tv.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                    videoSurface = Surface(surface)
                    isSurfaceAvailable = true
                    startVideoPlayback()
                }

                override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
                    currentVideoSize?.let { adjustAspectRatio(it.first, it.second) }
                }

                override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                    isSurfaceAvailable = false
                    videoSurface?.release()
                    videoSurface = null
                    return true
                }

                override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
            }
            tv.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
                currentVideoSize?.let { adjustAspectRatio(it.first, it.second) }
            }
            videoTextureView = tv
            addView(tv, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        }

        videoTextureView?.visibility = View.VISIBLE
        if (isSurfaceAvailable) {
            startVideoPlayback()
        }
    }

    private var currentVideoSize: Pair<Int, Int>? = null

    private fun adjustAspectRatio(videoWidth: Int, videoHeight: Int) {
        val tv = videoTextureView ?: return
        val viewWidth = tv.width.toFloat()
        val viewHeight = tv.height.toFloat()
        if (viewWidth <= 0f || viewHeight <= 0f || videoWidth <= 0 || videoHeight <= 0) return

        val viewRatio = viewWidth / viewHeight
        val videoRatio = videoWidth.toFloat() / videoHeight.toFloat()

        var scaleX = 1f
        var scaleY = 1f
        if (viewRatio > videoRatio) {
            scaleY = viewRatio / videoRatio
        } else {
            scaleX = videoRatio / viewRatio
        }

        val matrix = android.graphics.Matrix()
        matrix.setScale(scaleX, scaleY, viewWidth / 2f, viewHeight / 2f)
        tv.setTransform(matrix)
    }

    private fun startVideoPlayback() {
        val surface = videoSurface ?: return
        val videoFile = WallpaperManager.getCustomLiveVideoFile(context)
        if (!videoFile.exists() || videoFile.length() <= 0) return

        try {
            stopVideoPlaybackOnly()
            val fis = java.io.FileInputStream(videoFile)
            videoFis = fis
            mediaPlayer = MediaPlayer().apply {
                setSurface(surface)
                setDataSource(fis.fd, 0, videoFile.length())
                isLooping = true
                setVolume(0f, 0f) // Silent ambient wallpaper
                setOnVideoSizeChangedListener { _, width, height ->
                    if (width > 0 && height > 0) {
                        currentVideoSize = Pair(width, height)
                        post { adjustAspectRatio(width, height) }
                    }
                }
                setOnErrorListener { mp, what, extra ->
                    android.util.Log.e("LiveWallpaperView", "MediaPlayer error: what=$what extra=$extra")
                    try { mp.reset() } catch (_: Exception) {}
                    true
                }
                setOnPreparedListener { mp ->
                    try {
                        mp.start()
                    } catch (e: Exception) {
                        android.util.Log.e("LiveWallpaperView", "MediaPlayer start failed", e)
                    }
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            android.util.Log.e("LiveWallpaperView", "startVideoPlayback exception", e)
        }
    }

    private fun stopVideoPlaybackOnly() {
        try {
            mediaPlayer?.stop()
        } catch (_: Exception) {}
        try {
            mediaPlayer?.release()
        } catch (_: Exception) {}
        mediaPlayer = null
        try {
            videoFis?.close()
        } catch (_: Exception) {}
        videoFis = null
    }

    private fun stopVideoPlayer() {
        stopVideoPlaybackOnly()
        videoTextureView?.let { removeView(it) }
        videoTextureView = null
        videoSurface?.release()
        videoSurface = null
        isSurfaceAvailable = false
        currentVideoSize = null
    }

    /**
     * Mathematical hardware-accelerated ambient shader view.
     */
    private class AmbientCanvasView(context: Context) : View(context) {
        private var preset = "LIVE_AURORA"
        private var isRunning = false
        private var startTime = System.currentTimeMillis()

        private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val particlePaint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Cosmic stars
        private data class Star(var x: Float, var y: Float, var size: Float, var alphaSpeed: Float, var baseAlpha: Float)
        private val stars = mutableListOf<Star>()

        // Rain droplets
        private data class Drop(var x: Float, var y: Float, var length: Float, var speed: Float, var alpha: Int)
        private val drops = mutableListOf<Drop>()

        // Embers
        private data class Ember(var x: Float, var y: Float, var radius: Float, var speedY: Float, var speedX: Float, var alpha: Float)
        private val embers = mutableListOf<Ember>()

        fun setPreset(name: String) {
            preset = name
            initParticles()
            invalidate()
        }

        fun resume() {
            if (!isRunning) {
                isRunning = true
                startTime = System.currentTimeMillis()
                postInvalidateOnAnimation()
            }
        }

        fun pause() {
            isRunning = false
        }

        override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
            super.onSizeChanged(w, h, oldw, oldh)
            initParticles()
        }

        private fun initParticles() {
            val w = width.toFloat().coerceAtLeast(100f)
            val h = height.toFloat().coerceAtLeast(100f)
            val rng = Random(42)

            when (preset) {
                "LIVE_COSMIC" -> {
                    stars.clear()
                    for (i in 0 until 90) {
                        stars.add(
                            Star(
                                x = rng.nextFloat() * w,
                                y = rng.nextFloat() * h,
                                size = 1.0f + rng.nextFloat() * 2.8f,
                                alphaSpeed = 1.5f + rng.nextFloat() * 2.5f,
                                baseAlpha = 0.3f + rng.nextFloat() * 0.7f
                            )
                        )
                    }
                }
                "LIVE_LOFI_RAIN" -> {
                    drops.clear()
                    for (i in 0 until 75) {
                        drops.add(
                            Drop(
                                x = rng.nextFloat() * (w + 100f) - 50f,
                                y = rng.nextFloat() * h,
                                length = 18f + rng.nextFloat() * 26f,
                                speed = 14f + rng.nextFloat() * 16f,
                                alpha = 60 + rng.nextInt(120)
                            )
                        )
                    }
                }
                "LIVE_AMBER_GLOW" -> {
                    embers.clear()
                    for (i in 0 until 40) {
                        embers.add(
                            Ember(
                                x = rng.nextFloat() * w,
                                y = rng.nextFloat() * h,
                                radius = 2.5f + rng.nextFloat() * 4.5f,
                                speedY = 1.0f + rng.nextFloat() * 2.2f,
                                speedX = (rng.nextFloat() - 0.5f) * 0.8f,
                                alpha = 0.3f + rng.nextFloat() * 0.7f
                            )
                        )
                    }
                }
            }
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val w = width.toFloat()
            val h = height.toFloat()
            if (w <= 0 || h <= 0) return

            val t = (System.currentTimeMillis() - startTime) / 1000f

            when (preset) {
                "LIVE_AURORA" -> drawAurora(canvas, w, h, t)
                "LIVE_COSMIC" -> drawCosmic(canvas, w, h, t)
                "LIVE_LOFI_RAIN" -> drawRain(canvas, w, h)
                else -> drawAurora(canvas, w, h, t)
            }

            if (isRunning) {
                postInvalidateOnAnimation()
            }
        }

        private fun drawAurora(canvas: Canvas, w: Float, h: Float, t: Float) {
            // Dark cosmic backdrop
            canvas.drawColor(0xFF070B14.toInt())

            // Oscillating aurora energy center 1 (Emerald Teal)
            val cx1 = w * (0.3f + 0.25f * sin(t * 0.35f))
            val cy1 = h * (0.35f + 0.2f * cos(t * 0.45f))
            val r1 = (w.coerceAtLeast(h) * 0.75f)

            val tealGrad = RadialGradient(
                cx1, cy1, r1,
                intArrayOf(0x5500F5D4.toInt(), 0x22059669.toInt(), Color.TRANSPARENT),
                floatArrayOf(0f, 0.45f, 1f),
                Shader.TileMode.CLAMP
            )
            bgPaint.shader = tealGrad
            canvas.drawRect(0f, 0f, w, h, bgPaint)

            // Oscillating aurora energy center 2 (Electric Violet)
            val cx2 = w * (0.7f + 0.22f * cos(t * 0.4f))
            val cy2 = h * (0.6f + 0.25f * sin(t * 0.3f))
            val r2 = (w.coerceAtLeast(h) * 0.85f)

            val violetGrad = RadialGradient(
                cx2, cy2, r2,
                intArrayOf(0x507928CA.toInt(), 0x203B82F6.toInt(), Color.TRANSPARENT),
                floatArrayOf(0f, 0.5f, 1f),
                Shader.TileMode.CLAMP
            )
            bgPaint.shader = violetGrad
            canvas.drawRect(0f, 0f, w, h, bgPaint)

            // Deep breathing bottom glow
            val bottomPulse = 0.15f + 0.08f * sin(t * 0.6f)
            val alphaByte = (bottomPulse * 255).toInt().coerceIn(0, 255)
            val bottomGrad = LinearGradient(
                0f, h * 0.7f, 0f, h,
                Color.TRANSPARENT,
                Color.argb(alphaByte, 99, 102, 241),
                Shader.TileMode.CLAMP
            )
            bgPaint.shader = bottomGrad
            canvas.drawRect(0f, h * 0.7f, w, h, bgPaint)
        }

        private fun drawCosmic(canvas: Canvas, w: Float, h: Float, t: Float) {
            // Deep space backdrop
            canvas.drawColor(0xFF030712.toInt())

            // Subtle nebula center
            val nebGrad = RadialGradient(
                w * 0.5f, h * 0.45f, w * 0.7f,
                intArrayOf(0x356366F1.toInt(), 0x153B82F6.toInt(), Color.TRANSPARENT),
                floatArrayOf(0f, 0.5f, 1f),
                Shader.TileMode.CLAMP
            )
            bgPaint.shader = nebGrad
            canvas.drawRect(0f, 0f, w, h, bgPaint)
            bgPaint.shader = null

            // Drifting, twinkling stars
            particlePaint.style = Paint.Style.FILL
            for (s in stars) {
                val twinkle = (sin(t * s.alphaSpeed + s.x) + 1f) * 0.5f
                val effectiveAlpha = (s.baseAlpha * (0.4f + 0.6f * twinkle) * 255).toInt().coerceIn(0, 255)
                particlePaint.color = Color.argb(effectiveAlpha, 255, 255, 255)
                canvas.drawCircle(s.x, s.y, s.size, particlePaint)

                // Very slow drift
                s.y += 0.15f
                if (s.y > h) {
                    s.y = 0f
                    s.x = Random.nextFloat() * w
                }
            }
        }

        private fun drawRain(canvas: Canvas, w: Float, h: Float) {
            // Night storm moody backdrop
            canvas.drawColor(0xFF0B132B.toInt())

            particlePaint.style = Paint.Style.STROKE
            particlePaint.strokeWidth = 1.8f

            val dx = 3.5f // Slight diagonal angle
            for (d in drops) {
                particlePaint.color = Color.argb(d.alpha, 148, 163, 184)
                canvas.drawLine(d.x, d.y, d.x + dx, d.y + d.length, particlePaint)

                d.y += d.speed
                d.x += dx * 0.6f
                if (d.y > h + 30f) {
                    d.y = -d.length
                    d.x = Random.nextFloat() * (w + 100f) - 50f
                }
            }
        }

        private fun drawAmberGlow(canvas: Canvas, w: Float, h: Float, t: Float) {
            // Deep charcoal amber backdrop
            canvas.drawColor(0xFF0F0A06.toInt())

            // Warm campfire hearth bottom glow
            val hearthPulse = 0.45f + 0.1f * sin(t * 1.2f) + 0.05f * cos(t * 2.1f)
            val alphaByte = (hearthPulse * 255).toInt().coerceIn(0, 255)
            val hearthGrad = RadialGradient(
                w * 0.5f, h * 0.95f, w * 0.9f,
                intArrayOf(Color.argb(alphaByte, 245, 158, 11), 0x30D97706.toInt(), Color.TRANSPARENT),
                floatArrayOf(0f, 0.45f, 1f),
                Shader.TileMode.CLAMP
            )
            bgPaint.shader = hearthGrad
            canvas.drawRect(0f, 0f, w, h, bgPaint)
            bgPaint.shader = null

            // Rising embers
            particlePaint.style = Paint.Style.FILL
            for (e in embers) {
                val emberPulse = (sin(t * 2f + e.x) + 1f) * 0.5f
                val effectiveAlpha = (e.alpha * (0.5f + 0.5f * emberPulse) * 255).toInt().coerceIn(0, 255)
                particlePaint.color = Color.argb(effectiveAlpha, 251, 191, 36)
                canvas.drawCircle(e.x, e.y, e.radius, particlePaint)

                e.y -= e.speedY
                e.x += e.speedX + sin(t + e.y * 0.01f) * 0.4f
                if (e.y < -10f) {
                    e.y = h + 10f
                    e.x = Random.nextFloat() * w
                }
            }
        }
    }
}
