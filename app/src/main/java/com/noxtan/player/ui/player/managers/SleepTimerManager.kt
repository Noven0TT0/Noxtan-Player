package com.noxtan.player.ui.player.managers

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SleepTimerManager(
  private val coroutineScope: CoroutineScope,
  private val onTimerEnd: () -> Unit,
  private val showToast: () -> Unit
) {
  private var timerJob: Job? = null
  private val _remainingTime = MutableStateFlow(0)
  val remainingTime = _remainingTime.asStateFlow()

  fun startTimer(seconds: Int) {
    timerJob?.cancel()
    _remainingTime.value = seconds
    if (seconds < 1) return
    timerJob = coroutineScope.launch {
      for (time in seconds downTo 0) {
        _remainingTime.value = time
        delay(1000)
      }
      onTimerEnd()
      showToast()
    }
  }
}
