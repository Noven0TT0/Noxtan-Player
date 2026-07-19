package com.noxtan.player.ui.player.controls.components

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noxtan.player.R
import com.noxtan.player.ui.player.controls.LocalPlayerButtonsClickEvent
import com.noxtan.player.ui.theme.spacing
import dev.vivvvek.seeker.Seeker
import dev.vivvvek.seeker.SeekerDefaults
import dev.vivvvek.seeker.Segment
import `is`.xyz.mpv.Utils
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Composable
fun SeekbarWithTimers(
  position: Float,
  duration: Float,
  remaining: Float,
  readAheadValue: Float,
  onValueChange: (Float) -> Unit,
  onValueChangeFinished: () -> Unit,
  timersInverted: Pair<Boolean, Boolean>,
  positionTimerOnClick: () -> Unit,
  durationTimerOnCLick: () -> Unit,
  chapters: ImmutableList<Segment>,
  modifier: Modifier = Modifier,
) {
  val clickEvent = LocalPlayerButtonsClickEvent.current
  Row(
    modifier = modifier.height(48.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall),
  ) {
    VideoTimer(
      value = position,
      isInverted = timersInverted.first,
      descRes = R.string.a11y_current_time,
      onClick = {
        clickEvent()
        positionTimerOnClick()
      },
      modifier = Modifier.width(68.dp),
    )
    Seeker(
      value = position.coerceIn(0f, duration),
      range = 0f..duration,
      onValueChange = onValueChange,
      onValueChangeFinished = onValueChangeFinished,
      readAheadValue = readAheadValue,
      segments = chapters
        .filter { it.start in 0f..duration }
        .let { (if (it.isNotEmpty() && it[0].start != 0f) persistentListOf(Segment("", 0f)) + it else it) + it },
      modifier = Modifier.weight(1f).semantics {
        stateDescription = Utils.prettyTime(position.toInt())
      },
      colors = SeekerDefaults.seekerColors(
        progressColor = MaterialTheme.colorScheme.primary,
        thumbColor = MaterialTheme.colorScheme.primary,
        trackColor = MaterialTheme.colorScheme.background,
        readAheadColor = MaterialTheme.colorScheme.inversePrimary,
      ),
    )
    VideoTimer(
      value = if (timersInverted.second) -remaining else duration,
      isInverted = timersInverted.second,
      descRes = if (timersInverted.second) R.string.a11y_remaining_time else R.string.a11y_total_time,
      onClick = {
        clickEvent()
        durationTimerOnCLick()
      },
      modifier = Modifier.width(68.dp),
    )
  }
}

@Composable
fun VideoTimer(
  value: Float,
  isInverted: Boolean,
  @StringRes descRes: Int,
  modifier: Modifier = Modifier,
  onClick: () -> Unit = {},
) {
  val interactionSource = remember { MutableInteractionSource() }

  val timeString = Utils.prettyTime(value.toInt(), isInverted)
    .replace('၀', '0').replace('၁', '1').replace('၂', '2')
    .replace('၃', '3').replace('၄', '4').replace('၅', '5')
    .replace('၆', '6').replace('၇', '7').replace('၈', '8')
    .replace('၉', '9')

  val desc = stringResource(descRes, timeString)

  Text(
    modifier = modifier
      .fillMaxHeight()
      .clickable(
        interactionSource = interactionSource,
        indication = ripple(),
        onClick = onClick,
        role = androidx.compose.ui.semantics.Role.Button
      )
      .clearAndSetSemantics { contentDescription = desc }
      .wrapContentHeight(Alignment.CenterVertically),
    text = timeString,
    color = Color.White,
    textAlign = TextAlign.Center,
    fontSize = 13.sp,
    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
  )
}

@Preview
@Composable
private fun PreviewSeekBar() {
  SeekbarWithTimers(
    5f,
    20f,
    15f,
    4f,
    {},
    {},
    Pair(false, true),
    {},
    {},
    persistentListOf<Segment>()
  )
}
