package com.noxtan.player.data.logic

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class MediaObserver(private val context: Context) {
  fun observeChanges(): Flow<Unit> = callbackFlow {
    val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
      override fun onChange(selfChange: Boolean) {
        trySend(Unit)
      }
    }
    context.contentResolver.registerContentObserver(
      MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
      true,
      observer
    )
    awaitClose { context.contentResolver.unregisterContentObserver(observer) }
  }
}
