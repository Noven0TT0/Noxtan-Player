package com.noxtan.player.ui.player.controls.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun SubtitleOverlay(
  cues: List<String>,
  verticalPosition: Float,
  isBackgroundEnabled: Boolean,
  fontSize: Int,
  scale: Float,
  isBold: Boolean,
  isItalic: Boolean,
  textColor: Int,
  borderColor: Int,
  borderSize: Int,
  shadowOffset: Int,
  onNextSubtitle: () -> Unit,
  onPreviousSubtitle: () -> Unit
) {
  val currentLatestCues by rememberUpdatedState(cues)
  val offsetX = remember { Animatable(0f) }
  val scope = rememberCoroutineScope()

  var accumulatedDrag by remember { mutableFloatStateOf(0f) }
  var isHolding by remember { mutableStateOf(false) }
  var frozenCues by remember { mutableStateOf<List<String>>(emptyList()) }

  val currentCuesToDisplay = if (isHolding && frozenCues.isNotEmpty()) frozenCues else currentLatestCues

  val biasY = (verticalPosition * 2) - 1
  val scaleValue = if (isHolding) 1.05f else 1.0f

  val hasCues = currentCuesToDisplay.isNotEmpty()
  val backgroundColor = if (hasCues && isBackgroundEnabled) {
    if (isHolding) Color.DarkGray.copy(alpha = 0.9f) else Color.Black.copy(alpha = 0.6f)
  } else {
    if (hasCues && isHolding) Color.Black.copy(alpha = 0.5f) else Color.Transparent
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .padding(bottom = 24.dp, start = 16.dp, end = 16.dp),
    contentAlignment = BiasAlignment(0f, biasY)
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth(0.9f)
        .wrapContentHeight()
        .offset { IntOffset(offsetX.value.roundToInt(), 0) }
        .pointerInput(Unit) {
          detectHorizontalDragGestures(
            onDragStart = {
              if (currentLatestCues.isNotEmpty()) {
                frozenCues = currentLatestCues
                isHolding = true
              }
              accumulatedDrag = 0f
            },
            onDragEnd = {
              val threshold = 100f
              val screenWidthOffset = 2000f
              scope.launch {
                if (accumulatedDrag < -threshold) {
                  offsetX.animateTo(-screenWidthOffset, tween(300))
                  onNextSubtitle()
                  offsetX.snapTo(screenWidthOffset)
                  offsetX.animateTo(0f, tween(300))
                } else if (accumulatedDrag > threshold) {
                  offsetX.animateTo(screenWidthOffset, tween(300))
                  onPreviousSubtitle()
                  offsetX.snapTo(-screenWidthOffset)
                  offsetX.animateTo(0f, tween(300))
                } else {
                  offsetX.animateTo(0f)
                }
                isHolding = false
              }
            },
            onDragCancel = {
              isHolding = false
              scope.launch { offsetX.animateTo(0f) }
            }
          ) { change, dragAmount ->
            change.consume()
            accumulatedDrag += dragAmount
          }
        }
        .pointerInput(currentLatestCues.isNotEmpty()) {
          if (currentLatestCues.isNotEmpty()) {
            detectTapGestures(onPress = {
              frozenCues = currentLatestCues
              isHolding = true
              tryAwaitRelease()
              isHolding = false
            })
          }
        },
      contentAlignment = Alignment.Center
    ) {
      if (hasCues) {
        val actualFontSize = (fontSize * scale * 0.4f).sp
        val fWeight = if (isBold) FontWeight.Bold else FontWeight.Normal
        val fStyle = if (isItalic) FontStyle.Italic else FontStyle.Normal
        val tColor = Color(textColor)
        val bColor = Color(borderColor)

        val shadow = if (!isBackgroundEnabled && !isHolding && shadowOffset > 0) {
          Shadow(color = bColor, offset = Offset(shadowOffset.toFloat(), shadowOffset.toFloat()), blurRadius = shadowOffset * 1.5f)
        } else null

        Box(
          modifier = Modifier
            .scale(scaleValue)
            .then(
              if (backgroundColor != Color.Transparent) {
                Modifier
                  .background(color = backgroundColor, shape = RoundedCornerShape(8.dp))
                  .padding(horizontal = 16.dp, vertical = 8.dp)
              } else {
                Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
              }
            ),
          contentAlignment = Alignment.Center
        ) {
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
          ) {
            val allLines = currentCuesToDisplay.flatMap { it.split("\n") }

            allLines.forEach { text ->
              Box(contentAlignment = Alignment.Center) {
                if (!isBackgroundEnabled && !isHolding && borderSize > 0) {
                  Text(
                    text = text,
                    style = TextStyle(
                      color = bColor,
                      fontSize = actualFontSize,
                      fontWeight = fWeight,
                      fontStyle = fStyle,
                      textAlign = TextAlign.Center,
                      platformStyle = PlatformTextStyle(includeFontPadding = true),
                      drawStyle = Stroke(width = borderSize * 1.5f, join = StrokeJoin.Round)
                    )
                  )
                }

                Text(
                  text = text,
                  style = TextStyle(
                    color = tColor,
                    fontSize = actualFontSize,
                    fontWeight = fWeight,
                    fontStyle = fStyle,
                    textAlign = TextAlign.Center,
                    platformStyle = PlatformTextStyle(includeFontPadding = true),
                    shadow = shadow
                  )
                )
              }
            }
          }
        }
      }
    }
  }
}
