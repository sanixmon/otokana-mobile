package com.otokana.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack

/** Plays sounds over what plays already, each on a static AudioTrack. */
class AudioOut {
    private val tracks = mutableListOf<AudioTrack>()

    /** Plays [sounds] one after another; null and empty ones play nothing. */
    @Synchronized
    fun play(sounds: List<Sound?>) {
        val pcm = Sound.join(sounds)
        if (pcm.isEmpty()) return
        tracks.removeAll { t ->
            (t.playState != AudioTrack.PLAYSTATE_PLAYING).also { if (it) t.release() }
        }
        val track = try {
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setTransferMode(AudioTrack.MODE_STATIC)
                .setBufferSizeInBytes(pcm.size * 2)
                .build()
        } catch (e: Exception) {
            return // no device: practice goes on silently
        }
        track.write(pcm, 0, pcm.size)
        track.setNotificationMarkerPosition(pcm.size)
        track.setPlaybackPositionUpdateListener(object : AudioTrack.OnPlaybackPositionUpdateListener {
            override fun onMarkerReached(t: AudioTrack) { t.stop() }
            override fun onPeriodicNotification(t: AudioTrack) {}
        })
        track.play()
        tracks += track
    }

    @Synchronized
    fun release() { tracks.forEach { it.release() }; tracks.clear() }
}
