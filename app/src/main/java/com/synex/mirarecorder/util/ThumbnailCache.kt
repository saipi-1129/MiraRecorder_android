package com.synex.mirarecorder.util

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ThumbnailCache @Inject constructor() {

    private val cache = ConcurrentHashMap<String, Bitmap?>()
    private val loading = ConcurrentHashMap<String, Boolean>()

    fun get(url: String): Bitmap? = cache[url]

    suspend fun loadThumbnail(url: String): Bitmap? {
        cache[url]?.let { return it }
        if (loading.putIfAbsent(url, true) != null) return null

        return withContext(Dispatchers.IO) {
            try {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(url, HashMap())
                val bitmap = retriever.getFrameAtTime(
                    1_000_000, // 1 second in microseconds
                    MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                )
                retriever.release()
                bitmap?.let {
                    val scaled = Bitmap.createScaledBitmap(it, 320, 180, true)
                    cache[url] = scaled
                    scaled
                }
            } catch (_: Exception) {
                null
            } finally {
                loading.remove(url)
            }
        }
    }
}
