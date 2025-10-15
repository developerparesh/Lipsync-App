package com.example.lipsyncapp.repository

import android.content.Context
import android.media.MediaRecorder
import android.net.Uri
import android.os.Environment
import com.example.lipsyncapp.models.AudioRecording
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class AudioRecordRepository(private val context: Context) {

    private var mediaRecorder: MediaRecorder? = null
    private var currentAudioFile: File? = null

    suspend fun startRecording(): Result<File> = withContext(Dispatchers.IO) {
        try {
            stopRecording() // Stop any previous recording

            val audioFile = createAudioFile()
            currentAudioFile = audioFile

            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(44100)
                setAudioChannels(1)
                setAudioEncodingBitRate(128000)
                setOutputFile(audioFile.absolutePath)
                
                prepare()
                start()
            }

            Result.success(audioFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun stopRecording(): Result<AudioRecording> = withContext(Dispatchers.IO) {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null

            val file = currentAudioFile
            currentAudioFile = null

            if (file != null && file.exists()) {
                val duration = calculateDuration(file)
                Result.success(AudioRecording(Uri.fromFile(file), duration, file.length()))
            } else {
                Result.failure(IOException("Recording file not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun createAudioFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
        return File.createTempFile(
            "AUDIO_${timeStamp}_",
            ".mp4",
            storageDir
        )
    }

    private fun calculateDuration(file: File): Long {
        // Simplified duration calculation
        // In production, you'd use MediaMetadataRetriever
        return 0L
    }

    fun isRecording(): Boolean {
        return mediaRecorder != null
    }
}