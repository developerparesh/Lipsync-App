package com.example.lipsyncapp.repository

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.example.lipsyncapp.models.ImageItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class MediaRepository(private val context: Context) {

    suspend fun getGalleryImages(): List<ImageItem> = withContext(Dispatchers.IO) {
        val images = mutableListOf<ImageItem>()
        
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.DATE_ADDED
        )
        
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"
        
        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
            val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
            
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn)
                val size = cursor.getLong(sizeColumn)
                val date = cursor.getLong(dateColumn)
                val contentUri = Uri.withAppendedPath(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    id.toString()
                )
                
                images.add(ImageItem(contentUri, name, size, date))
            }
        }
        
        return@withContext images
    }

    suspend fun getGeneratedVideos(): List<File> = withContext(Dispatchers.IO) {
        val videosDir = File(context.getExternalFilesDir(null), "generated_videos")
        if (!videosDir.exists()) videosDir.mkdirs()
        
        videosDir.listFiles()?.filter { file ->
            file.extension.lowercase() in listOf("mp4", "avi", "mov")
        }?.sortedByDescending { it.lastModified() } ?: emptyList()
    }

    fun createVideoOutputFile(): File {
        val videosDir = File(context.getExternalFilesDir(null), "generated_videos")
        if (!videosDir.exists()) videosDir.mkdirs()
        
        return File(videosDir, "lipsync_${System.currentTimeMillis()}.mp4")
    }
}