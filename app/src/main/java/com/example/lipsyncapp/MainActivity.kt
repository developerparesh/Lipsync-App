package com.example.lipsyncapp

import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.webkit.PermissionRequest
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.lipsyncapp.databinding.ActivityMainBinding
import com.example.lipsyncapp.models.ImageItem
import com.example.lipsyncapp.repository.AudioRecordRepository
import com.example.lipsyncapp.repository.LipSyncRepository
import com.example.lipsyncapp.repository.MediaRepository
import com.example.lipsyncapp.viewmodel.MainViewModel
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MainViewModel
    private var mediaPlayer: MediaPlayer? = null

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            val imageItem = ImageItem(it, it.lastPathSegment ?: "image")
            viewModel.selectImage(imageItem)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        // Manual Dependency Injection
        val mediaRepository = MediaRepository(this)
        val audioRecordRepository = AudioRecordRepository(this)
        val lipSyncRepository = LipSyncRepository(this)

        viewModel = ViewModelProvider(this, object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return MainViewModel(mediaRepository, audioRecordRepository, lipSyncRepository) as T
            }
        })[MainViewModel::class.java]

        checkPermissions()
        setupObservers()
        setupClickListeners()

    }

    private fun checkPermissions() {
        Dexter.withContext(this)
            .withPermissions(
                android.Manifest.permission.READ_EXTERNAL_STORAGE,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                android.Manifest.permission.RECORD_AUDIO,
                android.Manifest.permission.CAMERA
            )
            .withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport) {
                    if (report.areAllPermissionsGranted()) {
                        // Permissions granted, observers will handle the rest
                    } else {
                        Toast.makeText(
                            this@MainActivity,
                            "All permissions are required!",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                override fun onPermissionRationaleShouldBeShown(
                    permissions: List<com.karumi.dexter.listener.PermissionRequest?>?,
                    token: PermissionToken?,
                ) {
                    token!!.continuePermissionRequest()
                }
            }).check()
    }

    private fun setupObservers() {
        viewModel.selectedImage.observe(this) { image ->
            image?.let {
                binding.selectedImageText.text = "Selected: ${it.name}"
                binding.selectedImagePreview.setImageURI(it.uri)
            }
        }

        viewModel.audioRecording.observe(this) { recording ->
            recording?.let {
                binding.recordingStatus.text = "Recording ready (${it.duration}ms)"
                binding.playRecordingButton.isEnabled = true
            } ?: run {
                binding.recordingStatus.text = "No recording"
                binding.playRecordingButton.isEnabled = false
            }
        }

        viewModel.isRecording.observe(this) { recording ->
            binding.recordButton.text = if (recording) "Stop Recording" else "Start Recording"
            binding.recordButton.setBackgroundColor(
                ContextCompat.getColor(
                    this,
                    if (recording) android.R.color.holo_red_light else R.color.green_500
                )
            )
        }

        viewModel.lipSyncResult.observe(this) { result ->
            result?.let {
                if (it.success) {
                    binding.resultText.text = "✅ Video generated successfully!"
                    it.outputUri?.let { uri -> playVideo(uri) }
                } else {
                    binding.resultText.text = "❌ Failed: ${it.error}"
                }
            }
        }

        viewModel.processingProgress.observe(this) { progress ->
            progress?.let {
                binding.progressBar.progress = it.percentage
                binding.progressText.text = "${it.percentage}% - ${it.status}"
            } ?: run {
                binding.progressText.text = ""
            }
        }

        viewModel.isProcessing.observe(this) { processing ->
            binding.progressBar.visibility =
                if (processing) android.view.View.VISIBLE else android.view.View.GONE
            binding.generateButton.isEnabled = !processing
            binding.cancelButton.isEnabled = processing

            if (processing) {
                binding.resultText.text = "Processing..."
            }
        }

        viewModel.serverConnected.observe(this) { connected ->
            binding.serverStatus.text =
                if (connected) "✅ Server Connected" else "❌ Server Disconnected"
            binding.serverStatus.setTextColor(
                ContextCompat.getColor(
                    this,
                    if (connected) R.color.green_500 else android.R.color.holo_red_dark
                )
            )
        }

        viewModel.generatedVideos.observe(this) { videos ->
            binding.videosCountText.text = "Generated Videos: ${videos.size}"
        }

        viewModel.error.observe(this) { error ->
            error?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }
    }

    private fun setupClickListeners() {
        binding.pickImageButton.setOnClickListener {
            pickImage.launch("image/*")
        }

        binding.recordButton.setOnClickListener {
            if (viewModel.isRecording.value == true) {
                viewModel.stopRecording()
            } else {
                viewModel.startRecording()
            }
        }

        binding.playRecordingButton.setOnClickListener {
            playAudioRecording()
        }

        binding.generateButton.setOnClickListener {
            viewModel.generateLipSyncVideo()
        }

        binding.cancelButton.setOnClickListener {
            viewModel.cancelProcessing()
        }

        binding.checkServerButton.setOnClickListener {
            viewModel.checkServerConnection()
        }

        binding.viewVideosButton.setOnClickListener {
            val videosDir = File(getExternalFilesDir(null), "generated_videos")
            val hasVideos = videosDir.exists() && videosDir.listFiles()?.isNotEmpty() == true

            if (hasVideos) {
                // Open video gallery
                val intent = Intent(this, VideoGalleryActivity::class.java)
                startActivity(intent)
            } else {
                Toast.makeText(this, "No videos generated yet", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Add this function to show videos gallery
    private fun showVideosGallery() {
        val videos = viewModel.generatedVideos.value ?: emptyList()

        if (videos.isEmpty()) {
            Toast.makeText(this, "No videos generated yet", Toast.LENGTH_SHORT).show()
            return
        }

        // Create a dialog to show available videos
        val videoNames = videos.map { it.name }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Generated Videos (${videos.size})")
            .setItems(videoNames) { dialog, which ->
                // Play selected video
                val selectedVideo = videos[which]
                playVideo(Uri.fromFile(selectedVideo))
            }
            .setPositiveButton("Close", null)
            .show()
    }

    // Update the click listener in setupClickListeners():


    private fun playAudioRecording() {
        val recording = viewModel.audioRecording.value
        recording?.let {
            try {
                mediaPlayer?.release()
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(this@MainActivity, it.uri)
                    prepare()
                    start()
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Failed to play recording", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun playVideo(videoUri: Uri) {
        // Implement video playback using ExoPlayer or Intent
        try {
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                setDataAndType(videoUri, "video/mp4")
                flags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "No app to play video", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        viewModel.cancelProcessing()
    }
}