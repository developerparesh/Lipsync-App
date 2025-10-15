package com.example.lipsyncapp.models

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class AudioRecording(
    val uri: Uri,
    val duration: Long,
    val fileSize: Long,
    val dateRecorded: Long = System.currentTimeMillis()
) : Parcelable