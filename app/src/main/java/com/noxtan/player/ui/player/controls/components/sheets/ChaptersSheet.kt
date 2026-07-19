package com.noxtan.player.ui.player.controls.components.sheets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.noxtan.player.R
import com.noxtan.player.ui.theme.spacing
import dev.vivvvek.seeker.Segment
import `is`.xyz.mpv.Utils
import kotlinx.collections.immutable.ImmutableList

@Composable
fun ChaptersSheet(
  chapters: ImmutableList<Segment>,
  currentChapter: Segment,
  onClick: (Segment) -> Unit,
  onDismissRequest: () -> Unit,
  modifier: Modifier = Modifier,
) {
  GenericTracksSheet(
    tracks = chapters,
    header = {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
      ) {
        Text(
          text = stringResource(R.string.a11y_player_chapters),
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold
        )
        androidx.compose.material3.IconButton(onClick = onDismissRequest) {
          androidx.compose.material3.Icon(
            imageVector = Icons.Default.Close,
            contentDescription = stringResource(R.string.a11y_back_close)
          )
        }
      }
    },
    track = {
      ChapterTrack(
        it,
        index = chapters.indexOf(it),
        selected = currentChapter == it,
        onClick = { onClick(it) },
      )
    },
    onDismissRequest = onDismissRequest,
    modifier = modifier.padding(vertical = MaterialTheme.spacing.medium)
  )
}

@Composable
fun ChapterTrack(
  chapter: Segment,
  index: Int,
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .padding(vertical = MaterialTheme.spacing.smaller, horizontal = MaterialTheme.spacing.medium),
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Text(
      stringResource(R.string.player_sheets_track_title_wo_lang, index + 1, chapter.name),
      fontStyle = if (selected) FontStyle.Italic else FontStyle.Normal,
      fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Normal,
      maxLines = 1,
      modifier = Modifier.weight(1f),
      overflow = TextOverflow.Ellipsis
    )
    Text(
      Utils.prettyTime(chapter.start.toInt()),
      fontStyle = if (selected) FontStyle.Italic else FontStyle.Normal,
      fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Normal,
    )
  }
}
