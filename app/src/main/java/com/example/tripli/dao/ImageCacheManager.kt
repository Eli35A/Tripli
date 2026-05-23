package com.example.tripli.dao

import android.content.Context
import android.graphics.Bitmap
import com.squareup.picasso.Picasso
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

class  ImageCacheManager(
    context: Context,
    private val postDao: HomePostDao
) {
    private val cacheDir = File(context.filesDir, "post_images").also { it.mkdirs() }
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Fire-and-forget: downloads image and stores local path in Room
    fun scheduleCache(postId: String, imageUrl: String) {
        if (imageUrl.isBlank() || imageFile(postId).exists()) return
        scope.launch {
            try {
                val bitmap = Picasso.get().load(imageUrl).get()
                val file = imageFile(postId)
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                }
                postDao.updateLocalImagePath(postId, file.absolutePath)
            } catch (_: Exception) { }
        }
    }

    fun localPathFor(postId: String): String? {
        val f = imageFile(postId)
        return if (f.exists()) f.absolutePath else null
    }

    private fun imageFile(postId: String) = File(cacheDir, "$postId.jpg")
}
