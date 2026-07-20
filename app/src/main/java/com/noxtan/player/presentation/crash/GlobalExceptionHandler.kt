package com.noxtan.player.presentation.crash

import android.content.Context
import android.content.Intent
import android.util.Log
import kotlin.system.exitProcess

class GlobalExceptionHandler(
  private val context: Context,
  private val activity: Class<*>
) : Thread.UncaughtExceptionHandler {

  override fun uncaughtException(t: Thread, e: Throwable) {
    Log.e("Noxtan_Player_CRASH_DEBUG", "==================== CRASH DETECTED ====================")
    Log.e("Noxtan_Player_CRASH_DEBUG", "Thread: ${t.name} (ID: ${t.id})")
    Log.e("Noxtan_Player_CRASH_DEBUG", "Exception Type: ${e.javaClass.name}")
    Log.e("Noxtan_Player_CRASH_DEBUG", "Exception Message: ${e.message}")
    Log.e("Noxtan_Player_CRASH_DEBUG", "Full Stack Trace:\n${Log.getStackTraceString(e)}")
    Log.e("Noxtan_Player_CRASH_DEBUG", "========================================================")

    val intent = Intent(context, activity)
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
    intent.putExtra("exception", e.stackTraceToString())
    context.startActivity(intent)
    exitProcess(0)
  }
}
