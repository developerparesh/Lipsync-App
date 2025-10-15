package com.example.lipsyncapp

import android.app.Application
import com.example.lipsyncapp.repository.AudioRecordRepository
import com.example.lipsyncapp.repository.LipSyncRepository
import com.example.lipsyncapp.repository.MediaRepository
import com.example.lipsyncapp.viewmodel.MainViewModel


class LipSyncApp : Application() {

    override fun onCreate() {
        super.onCreate()
    }
}