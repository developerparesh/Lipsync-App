package com.example.lipsyncapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.example.lipsyncapp.databinding.ActivityVideoGalleryBinding
import java.io.File

class VideoGalleryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVideoGalleryBinding
    private lateinit var videoAdapter: VideoAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityVideoGalleryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        setupRecyclerView()
        loadVideos()
    }
        private fun setupRecyclerView() {
            videoAdapter = VideoAdapter { videoFile ->
                playVideo(Uri.fromFile(videoFile))
            }

            binding.videosRecyclerView.apply {
                layoutManager = GridLayoutManager(this@VideoGalleryActivity, 2)
                adapter = videoAdapter
            }
        }

        private fun loadVideos() {
            val videosDir = File(getExternalFilesDir(null), "generated_videos")
            val videoFiles = videosDir.listFiles()?.filter {
                it.extension.lowercase() == "mp4"
            }?.sortedByDescending { it.lastModified() } ?: emptyList()

            videoAdapter.submitList(videoFiles)
            binding.emptyText.visibility = if (videoFiles.isEmpty()) View.VISIBLE else View.GONE
        }

        private fun playVideo(videoUri: Uri) {
            try {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(videoUri, "video/mp4")
                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                }
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "No app to play video", Toast.LENGTH_SHORT).show()
            }
        }

}