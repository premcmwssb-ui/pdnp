package com.pdnp.dailydigest.meeting

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import java.io.File

/** Records meeting audio to an .m4a file in app-private storage, with simple playback. */
class MeetingRecorder(private val context: Context) {

    private var recorder: MediaRecorder? = null
    private var player: MediaPlayer? = null

    var currentPath: String? = null
        private set

    /** @return the path of the file being recorded. Throws if the mic is unavailable. */
    fun start(): String {
        stopPlayback()
        val dir = File(context.filesDir, "meetings").apply { mkdirs() }
        val file = File(dir, "meeting_${System.currentTimeMillis()}.m4a")

        @Suppress("DEPRECATION")
        val r = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            MediaRecorder()
        }
        r.setAudioSource(MediaRecorder.AudioSource.MIC)
        r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        r.setAudioEncodingBitRate(96_000)
        r.setAudioSamplingRate(44_100)
        r.setOutputFile(file.absolutePath)
        r.prepare()
        r.start()

        recorder = r
        currentPath = file.absolutePath
        return file.absolutePath
    }

    fun stop(): String? {
        val path = currentPath
        try {
            recorder?.stop()
        } catch (_: Exception) {
            // stop() throws if nothing was captured; keep whatever file exists
        }
        recorder?.release()
        recorder = null
        currentPath = null
        return path
    }

    /** Plays the file at [path]; invokes [onComplete] when playback finishes. */
    fun play(path: String, onComplete: () -> Unit) {
        stopPlayback()
        val p = MediaPlayer()
        p.setDataSource(path)
        p.setOnCompletionListener {
            stopPlayback()
            onComplete()
        }
        p.prepare()
        p.start()
        player = p
    }

    fun stopPlayback() {
        try {
            player?.stop()
        } catch (_: Exception) {
        }
        player?.release()
        player = null
    }
}
