package com.example.lipsyncapp.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lipsyncapp.models.*
import com.example.lipsyncapp.repository.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.io.File

class MainViewModel(
    private val mediaRepository: MediaRepository,
    private val audioRecordRepository: AudioRecordRepository,
    private val lipSyncRepository: LipSyncRepository
) : ViewModel() {

    private val _galleryImages = MutableLiveData<List<ImageItem>>()
    val galleryImages: LiveData<List<ImageItem>> = _galleryImages

    private val _selectedImage = MutableLiveData<ImageItem?>()
    val selectedImage: LiveData<ImageItem?> = _selectedImage

    private val _audioRecording = MutableLiveData<AudioRecording?>()
    val audioRecording: LiveData<AudioRecording?> = _audioRecording

    private val _isRecording = MutableLiveData(false)
    val isRecording: LiveData<Boolean> = _isRecording

    private val _lipSyncResult = MutableLiveData<LipSyncResult?>()
    val lipSyncResult: LiveData<LipSyncResult?> = _lipSyncResult

    private val _processingProgress = MutableLiveData<ProcessingProgress?>()
    val processingProgress: LiveData<ProcessingProgress?> = _processingProgress

    private val _isProcessing = MutableLiveData(false)
    val isProcessing: LiveData<Boolean> = _isProcessing

    private val _serverConnected = MutableLiveData<Boolean>()
    val serverConnected: LiveData<Boolean> = _serverConnected

    private val _generatedVideos = MutableLiveData<List<File>>()
    val generatedVideos: LiveData<List<File>> = _generatedVideos

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private var processingJob: Job? = null

    init {
        loadGalleryImages()
        loadGeneratedVideos()
        checkServerConnection()
    }

    fun loadGalleryImages() {
        viewModelScope.launch {
            try {
                val images = mediaRepository.getGalleryImages()
                _galleryImages.value = images
            } catch (e: Exception) {
                _error.value = "Failed to load images: ${e.message}"
            }
        }
    }

    fun loadGeneratedVideos() {
        viewModelScope.launch {
            try {
                val videos = mediaRepository.getGeneratedVideos()
                _generatedVideos.value = videos
            } catch (e: Exception) {
                _error.value = "Failed to load videos: ${e.message}"
            }
        }
    }

    fun checkServerConnection() {
        viewModelScope.launch {
            try {
                val connected = lipSyncRepository.checkServerConnection()
                _serverConnected.value = connected
                if (!connected) {
                    _error.value = "Cannot connect to server. Check PC IP address."
                }
            } catch (e: Exception) {
                _serverConnected.value = false
                _error.value = "Connection failed: ${e.message}"
            }
        }
    }

    fun selectImage(image: ImageItem) {
        _selectedImage.value = image
    }

    fun startRecording() {
        viewModelScope.launch {
            _isRecording.value = true
            _error.value = null
            
            val result = audioRecordRepository.startRecording()
            if (result.isFailure) {
                _error.value = "Recording failed: ${result.exceptionOrNull()?.message}"
                _isRecording.value = false
            }
        }
    }

    fun stopRecording() {
        viewModelScope.launch {
            val result = audioRecordRepository.stopRecording()
            _isRecording.value = false
            
            if (result.isSuccess) {
                _audioRecording.value = result.getOrNull()
            } else {
                _error.value = "Stop recording failed: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    fun generateLipSyncVideo() {
        val image = _selectedImage.value
        val audio = _audioRecording.value
        
        if (image == null || audio == null) {
            _error.value = "Please select both image and record audio"
            return
        }

        // Cancel previous job if exists
        processingJob?.cancel()
        
        processingJob = viewModelScope.launch {
            _isProcessing.value = true
            _error.value = null
            _processingProgress.value = ProcessingProgress(0, 100, 0, "Starting...")
            
            val request = LipSyncRequest(image.uri, audio.uri)
            
            lipSyncRepository.generateLipSyncVideoWithProgress(request)
                .onEach { progress ->
                    _processingProgress.value = progress
                }
                .catch { e ->
                    _error.value = "Processing failed: ${e.message}"
                    _isProcessing.value = false
                    _processingProgress.value = null
                }
                .launchIn(this)
            
            // Wait for completion and get final result
            val result = lipSyncRepository.generateLipSyncVideo(request)
            _lipSyncResult.value = result
            _isProcessing.value = false
            _processingProgress.value = null
            
            if (result.success) {
                loadGeneratedVideos() // Refresh videos list
            } else {
                _error.value = result.error
            }
        }
    }

    fun cancelProcessing() {
        processingJob?.cancel()
        _isProcessing.value = false
        _processingProgress.value = null
    }

    fun clearError() {
        _error.value = null
    }

    fun clearResults() {
        _lipSyncResult.value = null
    }
}