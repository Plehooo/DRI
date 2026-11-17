package id.plehooo.dri

import android.app.Activity
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView

class MainActivity : Activity() {
    private var exoPlayer: ExoPlayer? = null
    private lateinit var playerView: PlayerView
    private lateinit var urlInput: EditText
    private lateinit var statusText: TextView
    private lateinit var playButton: Button
    private val prefs by lazy { getSharedPreferences("dri_player", Context.MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(11, 17, 27)
        window.navigationBarColor = Color.rgb(11, 17, 27)
        window.decorView.systemUiVisibility = 0

        setContentView(createScreen())
        exoPlayer = ExoPlayer.Builder(this).build().also { player ->
            playerView.player = player
            player.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    when (playbackState) {
                        Player.STATE_BUFFERING -> setStatus("Menghubungkan ke manifest…")
                        Player.STATE_READY -> setStatus("Siap diputar • ${currentKind()}")
                        Player.STATE_ENDED -> setStatus("Siaran selesai.")
                        Player.STATE_IDLE -> Unit
                    }
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    if (isPlaying) {
                        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                        playButton.text = "PAUSE"
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                        playButton.text = "PLAY"
                    }
                }

                override fun onPlayerError(error: PlaybackException) {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    setStatus("Gagal memutar: ${error.errorCodeName}. Periksa URL, koneksi, atau izin akses stream.")
                }
            })
        }

        val last = prefs.getString(KEY_LAST_URL, "").orEmpty()
        if (last.isNotBlank()) urlInput.setText(last)
    }

    private fun createScreen(): View {
        val scroll = ScrollView(this).apply {
            setBackgroundColor(Color.rgb(11, 17, 27))
            isFillViewport = true
        }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(22), dp(18), dp(26))
        }
        scroll.addView(content)

        val brand = TextView(this).apply {
            text = "DRI"
            textSize = 31f
            setTextColor(Color.rgb(72, 216, 178))
            typeface = Typeface.DEFAULT_BOLD
        }
        content.addView(brand)

        val heading = TextView(this).apply {
            text = "DIRECT STREAM PLAYER"
            textSize = 12f
            letterSpacing = 0.12f
            setTextColor(Color.rgb(173, 190, 210))
        }
        content.addView(heading, marginParams(bottom = 18))

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(15), dp(14), dp(14))
            background = rounded(Color.rgb(22, 32, 47), 18)
        }
        content.addView(card, marginParams(bottom = 14))

        val label = TextView(this).apply {
            text = "URL MANIFEST HLS / DASH"
            textSize = 11f
            letterSpacing = 0.08f
            setTextColor(Color.rgb(181, 199, 216))
            typeface = Typeface.DEFAULT_BOLD
        }
        card.addView(label, marginParams(bottom = 9))

        urlInput = EditText(this).apply {
            hint = "https://server.example/live/index.m3u8"
            setHintTextColor(Color.rgb(111, 130, 151))
            setTextColor(Color.WHITE)
            textSize = 13f
            setSingleLine(true)
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_URI
            setSelectAllOnFocus(false)
            setPadding(dp(12), 0, dp(12), 0)
            background = rounded(Color.rgb(12, 20, 31), 12, Color.rgb(49, 68, 87))
        }
        card.addView(urlInput, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(50)))

        val actions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        card.addView(actions, marginParams(top = 11))

        val paste = makeButton("TEMPEL", false)
        actions.addView(paste, weightedParams())
        paste.setOnClickListener { pasteFromClipboard() }

        val play = makeButton("PUTAR", true)
        actions.addView(play, weightedParams(start = 8))
        play.setOnClickListener { playManifest() }

        val last = makeButton("TERAKHIR", false)
        actions.addView(last, weightedParams(start = 8))
        last.setOnClickListener {
            val value = prefs.getString(KEY_LAST_URL, "").orEmpty()
            if (value.isNotBlank()) urlInput.setText(value)
            else setStatus("Belum ada URL yang tersimpan.")
        }

        statusText = TextView(this).apply {
            text = "Tempel URL manifest yang boleh kamu akses, lalu tekan PUTAR."
            textSize = 12f
            setTextColor(Color.rgb(177, 195, 213))
            setPadding(dp(2), dp(10), dp(2), dp(2))
        }
        card.addView(statusText)

        val playerLabel = TextView(this).apply {
            text = "PEMUTAR INTERNAL"
            textSize = 11f
            letterSpacing = 0.1f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.rgb(72, 216, 178))
        }
        content.addView(playerLabel, marginParams(bottom = 8, top = 4))

        playerView = PlayerView(this).apply {
            useController = true
            controllerAutoShow = true
            controllerHideOnTouch = true
            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
            setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
            setBackgroundColor(Color.BLACK)
        }
        val playerFrame = FrameLayout(this).apply {
            background = rounded(Color.BLACK, 16)
            clipToOutline = true
            addView(playerView, FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            ))
        }
        content.addView(playerFrame, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dp(228)
        ).apply { bottomMargin = dp(12) })

        val controls = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        content.addView(controls, marginParams(bottom = 14))

        playButton = makeButton("PLAY", true)
        controls.addView(playButton, weightedParams())
        playButton.setOnClickListener {
            val player = exoPlayer
            if (player == null) return@setOnClickListener
            if (player.isPlaying) player.pause() else player.play()
        }

        val stop = makeButton("STOP", false)
        controls.addView(stop, weightedParams(start = 8))
        stop.setOnClickListener {
            exoPlayer?.stop()
            exoPlayer?.clearMediaItems()
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            setStatus("Pemutaran dihentikan.")
        }

        val info = TextView(this).apply {
            text = "DRI memutar manifest HTTPS HLS (.m3u8) dan DASH (.mpd) secara langsung. URL halaman YouTube / YouTube Kids bukan URL manifest; aplikasi tidak membongkar URL internal, token, login, DRM, atau pembatasan layanan."
            textSize = 11f
            setTextColor(Color.rgb(137, 157, 179))
            setLineSpacing(dp(3).toFloat(), 1f)
        }
        content.addView(info)
        return scroll
    }

    private fun playManifest() {
        val raw = urlInput.text?.toString()?.trim().orEmpty()
        if (raw.isBlank()) {
            setStatus("Masukkan URL manifest terlebih dahulu.")
            return
        }

        val uri = runCatching { Uri.parse(raw) }.getOrNull()
        val scheme = uri?.scheme?.lowercase()
        val host = uri?.host?.lowercase().orEmpty()
        val path = uri?.encodedPath?.lowercase().orEmpty()

        if (uri == null || scheme != "https" || host.isBlank()) {
            setStatus("Gunakan URL HTTPS yang valid.")
            return
        }

        val isYoutubePage = host == "youtu.be" || host == "youtube.com" || host.endsWith(".youtube.com") || host == "youtubekids.com" || host.endsWith(".youtubekids.com")
        if (isYoutubePage) {
            setStatus("Ini URL halaman YouTube, bukan manifest. DRI hanya menerima URL manifest yang dipublikasikan untuk pemutaran langsung.")
            return
        }

        val isGoogleVideoHost = host == "googlevideo.com" || host.endsWith(".googlevideo.com")
        val isHls = path.endsWith(".m3u8") || (isGoogleVideoHost && path.contains("hls_playlist"))
        val isDash = path.endsWith(".mpd") || (isGoogleVideoHost && path.contains("dash"))
        val isGoogleManifest = isGoogleVideoHost && path.contains("manifest")
        if (!isHls && !isDash && !isGoogleManifest) {
            setStatus("Manifest belum dikenali. Masukkan URL HLS .m3u8 atau DASH .mpd yang sah.")
            return
        }

        val player = exoPlayer ?: run {
            setStatus("Player belum siap. Tutup lalu buka ulang aplikasi.")
            return
        }

        prefs.edit().putString(KEY_LAST_URL, raw).apply()
        setStatus("Menyiapkan stream…")
        runCatching {
            player.setMediaItem(MediaItem.fromUri(uri))
            player.prepare()
            player.playWhenReady = true
        }.onFailure {
            setStatus("Tidak dapat membuka manifest: ${it.message ?: "URL tidak valid"}")
        }
    }

    private fun currentKind(): String {
        val path = runCatching { Uri.parse(urlInput.text.toString()).encodedPath.orEmpty().lowercase() }.getOrDefault("")
        return when {
            path.endsWith(".mpd") || path.contains("dash") -> "DASH"
            else -> "HLS / manifest"
        }
    }

    private fun pasteFromClipboard() {
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val clip = clipboard.primaryClip
        if (clip == null || clip.itemCount == 0) {
            setStatus("Clipboard kosong.")
            return
        }
        urlInput.setText(clip.getItemAt(0).coerceToText(this)?.toString().orEmpty())
        urlInput.setSelection(urlInput.text.length)
        setStatus("URL ditempel dari clipboard. Tekan PUTAR untuk melanjutkan.")
    }

    private fun makeButton(title: String, primary: Boolean): Button = Button(this).apply {
        text = title
        textSize = 10f
        letterSpacing = 0.03f
        isAllCaps = false
        typeface = Typeface.DEFAULT_BOLD
        setTextColor(if (primary) Color.rgb(8, 25, 25) else Color.rgb(220, 233, 245))
        background = rounded(
            if (primary) Color.rgb(72, 216, 178) else Color.rgb(35, 51, 70),
            12
        )
        minHeight = dp(44)
        minimumHeight = dp(44)
        setPadding(dp(7), 0, dp(7), 0)
        stateListAnimator = null
    }

    private fun rounded(color: Int, radius: Int, stroke: Int? = null): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
            if (stroke != null) setStroke(dp(1), stroke)
        }

    private fun marginParams(bottom: Int = 0, top: Int = 0): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            bottomMargin = dp(bottom)
            topMargin = dp(top)
        }

    private fun weightedParams(start: Int = 0): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(0, dp(44), 1f).apply { marginStart = dp(start) }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun setStatus(message: String) {
        if (::statusText.isInitialized) statusText.text = message
    }

    override fun onDestroy() {
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (::playerView.isInitialized) playerView.player = null
        exoPlayer?.release()
        exoPlayer = null
        super.onDestroy()
    }

    companion object {
        private const val KEY_LAST_URL = "last_manifest_url"
    }
}
