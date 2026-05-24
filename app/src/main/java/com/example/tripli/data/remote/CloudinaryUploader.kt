package com.example.tripli.data.remote

import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class CloudinaryUploader {

    suspend fun upload(imageBytes: ByteArray, folder: String = "tripli/posts"): String = suspendCancellableCoroutine { cont ->
        MediaManager.get()
            .upload(imageBytes)
            .unsigned(UPLOAD_PRESET)
            .option("folder", folder)
            .callback(object : UploadCallback {
                override fun onStart(requestId: String) {}
                override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {}
                override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                    val url = resultData["secure_url"] as? String
                    if (url != null) cont.resume(url)
                    else cont.resumeWithException(Exception("Cloudinary response missing secure_url"))
                }
                override fun onError(requestId: String, error: ErrorInfo) {
                    cont.resumeWithException(Exception("Cloudinary upload failed: ${error.description}"))
                }
                override fun onReschedule(requestId: String, error: ErrorInfo) {
                    cont.resumeWithException(Exception("Cloudinary upload rescheduled: ${error.description}"))
                }
            })
            .dispatch()
    }

    companion object {
        const val CLOUD_NAME = "dthqh7un9"
        private const val UPLOAD_PRESET = "tripli"
    }
}
