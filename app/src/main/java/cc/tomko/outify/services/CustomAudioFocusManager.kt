package cc.tomko.outify.services

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager

open class CustomAudioFocusManager(mContext: Context, onAudioFocusChanged: (Int) -> Unit) {
    private val attributes = AudioAttributes.Builder().apply {
        setUsage(AudioAttributes.USAGE_MEDIA)
        setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
    }.build()
    private var focusRequest: AudioFocusRequest? = null
    private var audioManager: AudioManager? = null

    init {
        audioManager = mContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    private val focusChangeListener = AudioManager.OnAudioFocusChangeListener { state ->
        onAudioFocusChanged(state)
    }

    @Suppress("DEPRECATION")
    fun setupAudioFocusRequest() {
        // set the playback attributes for the focus requester
        focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(attributes)
            .setAcceptsDelayedFocusGain(true)
            .setOnAudioFocusChangeListener(focusChangeListener)
            .build()

        // request the audio focus
        focusRequest?.let {
            audioManager?.requestAudioFocus(it)
        }
    }

    @Suppress("DEPRECATION")
    fun releaseAudioFocus() {
        focusRequest?.let {
            audioManager?.abandonAudioFocusRequest(it)
        }
    }
}