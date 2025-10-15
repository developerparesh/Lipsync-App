package com.example.lipsyncapp.repository

import android.content.Context
import android.net.Uri
import com.example.lipsyncapp.api.Wav2LipApiService
import com.example.lipsyncapp.models.LipSyncRequest
import com.example.lipsyncapp.models.LipSyncResult
import com.example.lipsyncapp.models.ProcessingProgress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

class LipSyncRepository(private val context: Context) {

    // ⚠️ UPDATE THIS WITH YOUR PC's IP ADDRESS ⚠️
//    private val BASE_URL = "http://192.168.1.100:8000/"
    private val BASE_URL = "http://192.168.1.7:8000/"

    private val apiService: Wav2LipApiService by lazy {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val client = okhttp3.OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(300, TimeUnit.SECONDS) // Longer timeout for processing
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(loggingInterceptor)
            .build()

        retrofit2.Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
            .build()
            .create(Wav2LipApiService::class.java)
    }

    fun generateLipSyncVideoWithProgress(request: LipSyncRequest): Flow<ProcessingProgress> = flow {
        emit(ProcessingProgress(0, 100, 0, "Starting..."))

        try {
            // Step 1: Prepare files (10%)
            emit(ProcessingProgress(10, 100, 10, "Preparing files..."))
            val imagePart = createImagePart(request.imageUri)
            val audioPart = createAudioPart(request.audioUri)

            // Step 2: Upload to server (20%)
            emit(ProcessingProgress(30, 100, 30, "Uploading to server..."))
            val response = apiService.processLipSync(imagePart, audioPart)

            if (response.isSuccessful) {
                val apiResponse = response.body()
                if (apiResponse?.success == true && apiResponse.video_url != null) {

                    // Step 3: Download video (50%)
                    emit(ProcessingProgress(60, 100, 60, "Processing video..."))

                    // Simulate processing time
                    for (i in 61..90) {
                        kotlinx.coroutines.delay(100)
                        emit(ProcessingProgress(i, 100, i, "Processing..."))
                    }

                    // Step 4: Download result
                    emit(ProcessingProgress(95, 100, 95, "Downloading result..."))
                    val videoUrl = "${BASE_URL}${apiResponse.video_url.removePrefix("/")}"
                    val outputFile = downloadVideoFile(videoUrl, request.outputFileName)

                    emit(ProcessingProgress(100, 100, 100, "Complete!"))
                } else {
                    throw Exception(apiResponse?.error ?: "Unknown error")
                }
            } else {
                throw Exception("Server error: ${response.code()}")
            }
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun generateLipSyncVideo(request: LipSyncRequest): LipSyncResult {
        return withContext(Dispatchers.IO) {
            try {
                val startTime = System.currentTimeMillis()

                val imagePart = createImagePart(request.imageUri)
                val audioPart = createAudioPart(request.audioUri)

                val response = apiService.processLipSync(imagePart, audioPart)

                if (response.isSuccessful) {
                    val apiResponse = response.body()
                    if (apiResponse?.success == true && apiResponse.video_url != null) {
                        val videoUrl = "${BASE_URL}${apiResponse.video_url.removePrefix("/")}"
                        val outputFile = downloadVideoFile(videoUrl, request.outputFileName)

                        val processingTime = System.currentTimeMillis() - startTime
                        LipSyncResult(true, Uri.fromFile(outputFile), processingTime = processingTime)
                    } else {
                        LipSyncResult(false, error = apiResponse?.error ?: "Unknown error")
                    }
                } else {
                    LipSyncResult(false, error = "Server error: ${response.code()}")
                }
            } catch (e: Exception) {
                LipSyncResult(false, error = "Network error: ${e.message}")
            }
        }
    }

    private fun createImagePart(uri: Uri): MultipartBody.Part {
        val file = File(getFilePathFromUri(uri) ?: "")
        val requestFile = RequestBody.create("image/jpeg".toMediaType(), file)
        return MultipartBody.Part.createFormData("image", "image.jpg", requestFile)
    }

    private fun createAudioPart(uri: Uri): MultipartBody.Part {
        val file = File(getFilePathFromUri(uri) ?: "")
        val requestFile = RequestBody.create("audio/wav".toMediaType(), file)
        return MultipartBody.Part.createFormData("audio", "audio.wav", requestFile)
    }

    private suspend fun downloadVideoFile(videoUrl: String, fileName: String): File {
        return withContext(Dispatchers.IO) {
            val outputFile = File(context.getExternalFilesDir(null), "generated_videos/$fileName")
            outputFile.parentFile?.mkdirs()

            val response = apiService.downloadVideo(videoUrl)
            if (response.isSuccessful) {
                response.body()?.byteStream()?.use { inputStream ->
                    FileOutputStream(outputFile).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
            }
            outputFile
        }
    }

    suspend fun checkServerConnection(): Boolean {
        return try {
            println("🔄 Checking server connection to: $BASE_URL")
            val response = apiService.getServerStatus()
            println("📡 Server response: ${response.code()} - ${response.body()}")
            response.isSuccessful
        } catch (e: Exception) {
            println("❌ Server connection failed: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    private fun getFilePathFromUri(uri: Uri): String? {
        return when (uri.scheme) {
            "content" -> {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val columnIndex = cursor.getColumnIndexOrThrow("_data")
                        cursor.getString(columnIndex)
                    } else null
                }
            }
            "file" -> uri.path
            else -> null
        }
    }
}