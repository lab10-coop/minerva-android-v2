package minerva.android.widget

import android.app.Activity
import android.content.Context
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.media.MediaPlayer.OnCompletionListener
import android.media.MediaPlayer.OnPreparedListener
import android.net.Uri
import android.os.Handler
import android.util.AttributeSet
import android.view.Surface
import android.view.TextureView
import android.view.TextureView.SurfaceTextureListener
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.MediaController
import androidx.annotation.DrawableRes
import androidx.annotation.Nullable
import androidx.annotation.RawRes
import com.bumptech.glide.Glide
import minerva.android.R
import minerva.android.extension.gone
import minerva.android.kotlinUtils.InvalidId
import minerva.android.kotlinUtils.function.orElse
import minerva.android.splash.PassiveVideoToActivityInteractor

/*PassiveVideoView is taken from: https://medium.com/@cwurthner/animated-content-in-android-for-dummies-decc19342c14*/

class PassiveVideoView : FrameLayout, MediaController.MediaPlayerControl, SurfaceTextureListener {
    private lateinit var listener: PassiveVideoToActivityInteractor
    private var mediaPlayer: MediaPlayer? = null
    private lateinit var videoUri: Uri

    private var isPrepared: Boolean = false

    private var fallbackView: ImageView? = null
    private var loadingView: ImageView? = null
    private var textureView: TextureView? = null

    private val onErrorListener: MediaPlayer.OnErrorListener =
        MediaPlayer.OnErrorListener { _, _, _ ->
            textureView?.gone()
            fallbackView?.animate()?.alpha(ALPHA)?.start()
            Handler().postDelayed({ listener.onAnimationEnd() }, DELAY)
            true
        }

    private val onPreparedListener: OnPreparedListener = OnPreparedListener {
        isPrepared = true
        start()
        listener.initWalletConfig()
    }

    private val onCompletionListener: OnCompletionListener = OnCompletionListener { mediaPlayer ->
        isPrepared = false
        mediaPlayer.release()
        listener.onAnimationEnd()
    }

    constructor(context: Context) : super(context) {
        init(null, 0)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(attrs, 0)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init(attrs, defStyleAttr)
    }

    fun setupListener(listener: PassiveVideoToActivityInteractor) {
        this.listener = listener
    }

    private fun init(@Nullable attrs: AttributeSet?, defStyleAttr: Int) {
        loadingView = ImageView(context)
        textureView = TextureView(context)
        fallbackView = ImageView(context)

        textureView?.apply {
            isOpaque = false
            alpha = TRANSPARENT
        }

        fallbackView?.apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            alpha = TRANSPARENT
        }

        addView(loadingView)
        addView(fallbackView)
        addView(textureView)

        context.theme.obtainStyledAttributes(attrs, R.styleable.PassiveVideoView, defStyleAttr, 0)
            .apply {
                val video = getResourceId(R.styleable.PassiveVideoView_video, 0)
                val fallback = getResourceId(R.styleable.PassiveVideoView_fallbackImage, 0)
                val loading = getResourceId(R.styleable.PassiveVideoView_loadingImage, 0)

                if (video != 0 && fallback != 0 && loading != 0) {
                    setVideoResource(video, fallback, loading)
                }
            }
    }

    fun onCreate(activity: Activity) {
        activity.window.addFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED)
        textureView?.apply {
            surfaceTextureListener = this@PassiveVideoView
            if (isAvailable) surfaceTexture?.let { onSurfaceTextureAvailable(it, 0, 0) }
        }
    }

    fun onDestroy() {
        mediaPlayer?.release()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var aspectRatio: Float = -1f
        try {
            mediaPlayer?.apply { aspectRatio = videoHeight / videoWidth.toFloat() }
        } catch (exception: Exception) {
            fallbackView?.drawable?.let {
                aspectRatio = it.intrinsicHeight / it.intrinsicWidth.toFloat()
            }.orElse {
                aspectRatio = 9 / 16f
            }
        }

        val width = MeasureSpec.getSize(widthMeasureSpec)
        super.onMeasure(
            MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec((width * aspectRatio).toInt(), MeasureSpec.EXACTLY)
        )
    }

    private fun setVideoUri(uri: Uri, @DrawableRes fallback: Int, @DrawableRes loading: Int) {
        videoUri = uri
        fallbackView?.let {
            Glide.with(context).asBitmap().override(BITMAP_WIDTH, BITMAP_HEIGHT).fitCenter()
                .load(fallback).into(it)
        }
        loadingView?.let {
            Glide.with(context).asBitmap().override(BITMAP_WIDTH, BITMAP_HEIGHT).fitCenter()
                .load(loading).into(it)
        }
    }

    private fun setVideoResource(
        @RawRes resource: Int,
        @DrawableRes fallback: Int,
        @DrawableRes loading: Int
    ) {
        setVideoUri(
            Uri.parse("android.resource://" + context.packageName + "/" + resource),
            fallback,
            loading
        )
    }

    override fun onSurfaceTextureAvailable(surfaceTexture: SurfaceTexture, i: Int, i1: Int) {
        try {
            mediaPlayer = MediaPlayer()
            mediaPlayer?.apply {
                setDataSource(context, videoUri)
                setSurface(Surface(textureView?.surfaceTexture))
                setOnErrorListener(onErrorListener)
                setOnPreparedListener(onPreparedListener)
                setOnCompletionListener(onCompletionListener)
                prepareAsync()
            }
        } catch (e: Exception) {
            onErrorListener.onError(mediaPlayer, MediaPlayer.MEDIA_ERROR_UNKNOWN, 0)
        }
    }

    override fun onSurfaceTextureSizeChanged(surfaceTexture: SurfaceTexture, i: Int, i1: Int) {
        // Nothing to do
    }

    override fun onSurfaceTextureDestroyed(surfaceTexture: SurfaceTexture): Boolean = false

    override fun onSurfaceTextureUpdated(surfaceTexture: SurfaceTexture) {
        // Nothing to do
    }

    override fun start() {
        if (isPrepared) {
            mediaPlayer?.start()
            textureView?.animate()?.alpha(ALPHA)?.setStartDelay(STARTUP_DELAY)?.start()
            requestLayout()
        }
    }

    override fun pause() {
        mediaPlayer?.let {
            if (isPrepared && it.isPlaying) it.pause()
        }
    }

    override fun getDuration(): Int = mediaPlayer?.duration ?: Int.InvalidId
    override fun getCurrentPosition(): Int = mediaPlayer?.currentPosition ?: Int.InvalidId
    override fun seekTo(i: Int) {
        mediaPlayer?.seekTo(i)
    }

    override fun isPlaying(): Boolean = mediaPlayer?.isPlaying ?: false
    override fun getBufferPercentage(): Int = 0
    override fun canPause(): Boolean = false
    override fun canSeekBackward(): Boolean = false
    override fun canSeekForward(): Boolean = false
    override fun getAudioSessionId(): Int = mediaPlayer?.audioSessionId ?: Int.InvalidId

    companion object {
        const val BITMAP_HEIGHT = 1080
        const val BITMAP_WIDTH = 680
        const val ALPHA = 1f
        const val DELAY = 800L
        const val STARTUP_DELAY = 100L
        const val TRANSPARENT = 0f
    }
}