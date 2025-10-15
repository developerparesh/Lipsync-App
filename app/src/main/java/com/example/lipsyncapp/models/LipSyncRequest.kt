package com.example.lipsyncapp.models

import android.net.Uri

data class LipSyncRequest(
    val imageUri: Uri,
    val audioUri: Uri,
    val outputFileName: String = "lipsync_${System.currentTimeMillis()}.mp4"
)

data class LipSyncResult(
    val success: Boolean,
    val outputUri: Uri? = null,
    val error: String? = null,
    val processingTime: Long = 0
)

data class ProcessingProgress(
    val current: Int,
    val total: Int,
    val percentage: Int,
    val status: String
)