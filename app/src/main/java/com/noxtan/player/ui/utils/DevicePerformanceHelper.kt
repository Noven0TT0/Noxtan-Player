package com.noxtan.player.ui.utils

import android.app.ActivityManager
import android.content.Context
import android.os.Build

object DevicePerformanceHelper {
  fun isLowEndDevice(context: Context): Boolean {
    val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val memoryInfo = ActivityManager.MemoryInfo()
    activityManager.getMemoryInfo(memoryInfo)

    val totalRamInGB = memoryInfo.totalMem / (1024 * 1024 * 1024.0)

    return when {
      activityManager.isLowRamDevice -> true
      totalRamInGB <= 3.5 -> true
      Build.VERSION.SDK_INT <= Build.VERSION_CODES.P -> true
      else -> false
    }
  }
}
