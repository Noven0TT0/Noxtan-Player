package com.noxtan.player.ui.player.controls.components.panels

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noxtan.player.R
import com.noxtan.player.ui.player.PlayerViewModel
import com.noxtan.player.ui.player.controls.PanelLayout

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SleepTimerPanel(
  viewModel: PlayerViewModel,
  onDismissRequest: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val sleepTimerTimeRemaining by viewModel.remainingTime.collectAsState()

  PanelLayout(modifier = modifier) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 16.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp, start = 4.dp)
      ) {
        Text(
          text = stringResource(R.string.timer_title),
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Black
        )
        IconButton(onClick = onDismissRequest, modifier = Modifier.size(32.dp)) {
          Icon(Icons.Default.Close, contentDescription = stringResource(R.string.a11y_back_close))
        }
      }

      if (sleepTimerTimeRemaining > 0) {
        Card(
          modifier = Modifier.fillMaxWidth(),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f))
        ) {
          Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Text("Time Remaining", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            Text(
              text = android.text.format.DateUtils.formatElapsedTime(sleepTimerTimeRemaining.toLong()),
              style = MaterialTheme.typography.headlineMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary
            )
            Button(
              onClick = { viewModel.startTimer(0) },
              colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
              modifier = Modifier.fillMaxWidth().height(36.dp),
              contentPadding = PaddingValues(0.dp)
            ) {
              Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Turn Off", style = MaterialTheme.typography.bodyMedium)
            }
          }
        }
      }

      Text("Quick Presets", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)

      val presets = listOf(
        Pair("15m", 15 * 60),
        Pair("30m", 30 * 60),
        Pair("45m", 45 * 60),
        Pair("60m", 60 * 60),
        Pair("90m", 90 * 60),
        Pair("120m", 120 * 60)
      )

      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        presets.chunked(3).forEach { row ->
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            row.forEach { (label, seconds) ->
              FilledTonalButton(
                onClick = {
                  viewModel.startTimer(seconds)
                },
                modifier = Modifier.weight(1f).height(38.dp),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
              ) {
                Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
              }
            }
          }
        }
      }

      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

      Text("Custom Duration", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)

      val hoursList = remember { (0..23).map { String.format(java.util.Locale.US, "%02d", it) } }
      val minutesList = remember { (0..59).map { String.format(java.util.Locale.US, "%02d", it) } }

      var selectedHourIndex by remember { mutableIntStateOf(0) }
      var selectedMinuteIndex by remember { mutableIntStateOf(0) }

      Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(vertical = 8.dp),
          horizontalArrangement = Arrangement.Center,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Hours", style = MaterialTheme.typography.bodySmall, color = Color.Gray, modifier = Modifier.padding(bottom = 4.dp))
            SlotWheelPicker(
              items = hoursList,
              initialIndex = selectedHourIndex,
              onIndexSelected = { selectedHourIndex = it }
            )
          }

          Text(
            text = ":",
            style = MaterialTheme.typography.headlineMedium.copy(fontSize = 24.sp),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            color = MaterialTheme.colorScheme.primary
          )

          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Minutes", style = MaterialTheme.typography.bodySmall, color = Color.Gray, modifier = Modifier.padding(bottom = 4.dp))
            SlotWheelPicker(
              items = minutesList,
              initialIndex = selectedMinuteIndex,
              onIndexSelected = { selectedMinuteIndex = it }
            )
          }
        }

        Button(
          onClick = {
            val customSeconds = (selectedHourIndex * 3600) + (selectedMinuteIndex * 60)
            if (customSeconds > 0) {
              viewModel.startTimer(customSeconds)
            }
          },
          modifier = Modifier.fillMaxWidth().height(40.dp)
        ) {
          Icon(Icons.Default.Timer, contentDescription = null)
          Spacer(modifier = Modifier.width(6.dp))
          Text("Set Custom Timer", style = MaterialTheme.typography.bodyMedium)
        }
      }
    }
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SlotWheelPicker(
  items: List<String>,
  initialIndex: Int,
  onIndexSelected: (Int) -> Unit,
  modifier: Modifier = Modifier,
  visibleItemsCount: Int = 3,
  itemHeight: Dp = 40.dp
) {
  val state = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)

  val flingBehavior = rememberSnapFlingBehavior(lazyListState = state)

  val selectedIndex by remember {
    derivedStateOf {
      val firstVisible = state.firstVisibleItemIndex
      val offset = state.firstVisibleItemScrollOffset
      if (offset > itemHeight.value / 2) {
        (firstVisible + 1).coerceAtMost(items.lastIndex)
      } else {
        firstVisible
      }
    }
  }

  LaunchedEffect(selectedIndex) {
    onIndexSelected(selectedIndex)
  }

  Box(
    modifier = modifier
      .height(itemHeight * visibleItemsCount)
      .width(70.dp),
    contentAlignment = Alignment.Center
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(itemHeight)
        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f))
        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
    )

    LazyColumn(
      state = state,
      flingBehavior = flingBehavior,
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(vertical = itemHeight * ((visibleItemsCount - 1) / 2)),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      itemsIndexed(items) { index, item ->
        val isSelected = index == selectedIndex
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(itemHeight),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = item,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.5f)
          )
        }
      }
    }
  }
}
