package com.example.lipsyncapp

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.lipsyncapp.databinding.ItemVideoBinding
import java.io.File
import java.util.Date

class VideoAdapter(private val onVideoClick: (File) -> Unit) :
    ListAdapter<File, VideoAdapter.VideoViewHolder>(DiffCallback) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val binding = ItemVideoBinding.inflate(
            LayoutInflater.from(parent.context),
            parent, 
            false
        )
        return VideoViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class VideoViewHolder(private val binding: ItemVideoBinding) :
        RecyclerView.ViewHolder(binding.root) {
        
        fun bind(videoFile: File) {
            binding.videoName.text = videoFile.name
            binding.videoDate.text = "Created: ${Date(videoFile.lastModified())}"
            binding.videoSize.text = "Size: ${videoFile.length() / 1024} KB"
            
            binding.root.setOnClickListener {
                onVideoClick(videoFile)
            }
        }
    }
    
    companion object DiffCallback : DiffUtil.ItemCallback<File>() {
        override fun areItemsTheSame(oldItem: File, newItem: File): Boolean {
            return oldItem.absolutePath == newItem.absolutePath
        }
        
        override fun areContentsTheSame(oldItem: File, newItem: File): Boolean {
            return oldItem.lastModified() == newItem.lastModified() && 
                   oldItem.length() == newItem.length()
        }
    }
}