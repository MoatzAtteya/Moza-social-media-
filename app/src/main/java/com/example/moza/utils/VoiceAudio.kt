package com.example.moza.utils

import android.media.MediaPlayer
import android.media.MediaRecorder
import android.util.Log
import java.io.IOException

class VoiceAudio {

    private var mediaPlayer: MediaPlayer = MediaPlayer()
    private var recorder: MediaRecorder? = null

    fun stopRecordPlayer() {
        if (recorder != null) {
            try {
                recorder?.release()
                recorder = null
                Log.d("Stop recording: ", "stopRecord")

            } catch (e: RuntimeException) {
                Log.e("Recording voice message: ", e.message.toString())
            }
        }
    }


    private fun playRecord(voiceLink: String) {
        if (mediaPlayer.isPlaying)
            mediaPlayer.reset()

        try {
            mediaPlayer.setDataSource(voiceLink)
            mediaPlayer.prepareAsync()
            mediaPlayer.setOnPreparedListener {
                mediaPlayer.start()
                // recordPlay.setImageResource(R.drawable.ic_pause_record)
                Log.d("Duration", "${it.duration}")
            }
        } catch (e: IOException) {
            Log.d("error", "$e")
        }
        mediaPlayer.setOnCompletionListener {
            mediaPlayer.reset()
            //recordPlay.setImageResource(R.drawable.ic_play_record)
        }
    }


    fun voiceRecord(fileName: String) {
        recorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setOutputFile(fileName)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            try {
                prepare()
            } catch (e: IOException) {
                Log.e("Recording voice message: ", "prepare() failed")
            }
            start()
        }
    }

}