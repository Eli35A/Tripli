package com.example.tripli.base

import android.app.Application
import com.cloudinary.android.MediaManager
import com.example.tripli.data.remote.CloudinaryUploader

class TripliApp : Application() {

    override fun onCreate() {
        super.onCreate()
        MediaManager.init(this, mapOf("cloud_name" to CloudinaryUploader.CLOUD_NAME))
    }
}
