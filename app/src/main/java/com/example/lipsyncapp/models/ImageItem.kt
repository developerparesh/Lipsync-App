package com.example.lipsyncapp.models

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ImageItem(
    val uri: Uri,
    val name: String,
    val size: Long = 0,
    val dateTaken: Long = System.currentTimeMillis()
) : Parcelable