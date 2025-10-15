package com.example.lipsyncapp.api

import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface Wav2LipApiService {
    @Multipart
    @POST("process")
    suspend fun processLipSync(
        @Part image: MultipartBody.Part,
        @Part audio: MultipartBody.Part
    ): Response<LipSyncResponse>

    @GET
    suspend fun downloadVideo(@Url url: String): Response<ResponseBody>

    @GET("status")
    suspend fun getServerStatus(): Response<ServerStatusResponse>
}

data class LipSyncResponse(
    val success: Boolean,
    val video_url: String? = null,
    val error: String? = null,
    val job_id: String? = null
)

data class ServerStatusResponse(
    val status: String,
    val ready: Boolean? = null
)