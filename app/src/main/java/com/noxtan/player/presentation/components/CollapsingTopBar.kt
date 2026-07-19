package com.noxtan.player.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.noxtan.player.R

private fun lerpFloat(start: Float, stop: Float, fraction: Float): Float = (1 - fraction) * start + fraction * stop

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollapsingTopBar(
  title: String,
  scrollBehavior: TopAppBarScrollBehavior,
  onBackClick: (() -> Unit)? = null,
  navIcon: ImageVector = Icons.AutoMirrored.Rounded.ArrowBack,
  isSearchActive: Boolean = false,
  actions: @Composable RowScope.(iconBgColor: Color) -> Unit = {}
) {
  val collapsedFraction = scrollBehavior.state.collapsedFraction.coerceIn(0f, 1f)
  val isCollapsed = collapsedFraction > 0.5f

  val iconBgColor by animateColorAsState(
    targetValue = if (isCollapsed) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0f),
    animationSpec = tween(200),
    label = "IconBackgroundColor"
  )

  val density = LocalDensity.current
  val layoutDirection = androidx.compose.ui.platform.LocalLayoutDirection.current
  val navigationBars = WindowInsets.navigationBars
  val leftNavBarPadding = with(density) { navigationBars.getLeft(density, layoutDirection).toDp() }
  val rightNavBarPadding = with(density) { navigationBars.getRight(density, layoutDirection).toDp() }

  val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
  val toolbarHeight = 64.dp
  val maxHeaderHeight = 130.dp

  val currentHeaderHeight = maxHeaderHeight - ((maxHeaderHeight - toolbarHeight) * collapsedFraction)
  val maxHeaderHeightPx = with(density) { maxHeaderHeight.toPx() }
  val toolbarHeightPx = with(density) { toolbarHeight.toPx() }

  SideEffect {
    val limit = toolbarHeightPx - maxHeaderHeightPx
    if (scrollBehavior.state.heightOffsetLimit != limit) {
      scrollBehavior.state.heightOffsetLimit = limit
    }
  }

  val bgAlpha = 1f - collapsedFraction
  val backgroundColor = MaterialTheme.colorScheme.background.copy(alpha = bgAlpha)

  Surface(
    modifier = Modifier.fillMaxWidth().height(currentHeaderHeight + statusBarHeight),
    color = backgroundColor,
    shadowElevation = 0.dp
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(start = leftNavBarPadding, end = rightNavBarPadding) // 💡 3-Button Navigation Bar ဘယ်/ညာ ရှိပါက အလိုအလျောက် တွန်းဖယ်ပေးမည်
    ) {

      val titleStartY = statusBarHeight + 60.dp
      val titleEndY = statusBarHeight
      val currentTitleY = androidx.compose.ui.unit.lerp(titleStartY, titleEndY, collapsedFraction)

      val hasNavIcon = onBackClick != null
      val expandedStartPadding = 24.dp
      val collapsedStartPadding = if (hasNavIcon) 64.dp else 24.dp

      val startPadding = androidx.compose.ui.unit.lerp(expandedStartPadding, collapsedStartPadding, collapsedFraction)
      val endPadding = 16.dp
      val titleScale = lerpFloat(1f, 0.7f, collapsedFraction)

      // 💡 ပြင်ဆင်ချက် (၁) : Title ကို ဘယ်တော့မှ မဖျောက်တော့ပါ (alpha ဖယ်လိုက်ပါပြီ)
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(toolbarHeight)
          .offset(y = currentTitleY)
          .padding(start = startPadding, end = endPadding)
      ) {
        Text(
          text = title,
          style = MaterialTheme.typography.headlineLarge,
          fontWeight = FontWeight.Black,
          color = MaterialTheme.colorScheme.onBackground,
          modifier = Modifier
            .align(Alignment.CenterStart)
            .graphicsLayer {
              scaleX = titleScale
              scaleY = titleScale
              transformOrigin = TransformOrigin(0f, 0.5f)
            }
            .semantics { heading(); paneTitle = title },
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }

      // 💡 ပြင်ဆင်ချက် (၂) : Row အစား Box ကိုသုံးလိုက်တဲ့အတွက် Search Pill က Back Button ရဲ့ အပေါ်ကနေ အုပ်ပြီး (Overlay) ပြန့်ထွက်သွားပါမယ်။ Layout ရွေ့တာ လုံးဝမရှိတော့ပါ။
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = statusBarHeight)
          .height(toolbarHeight)
      ) {
        // အောက်ဆုံးအလွှာ (Layer 1): Back Button
        if (onBackClick != null) {
          Box(modifier = Modifier.width(64.dp).fillMaxHeight(), contentAlignment = Alignment.Center) {
            IconButton(
              onClick = onBackClick,
              modifier = Modifier.clip(CircleShape).background(iconBgColor)
            ) {
              Icon(navIcon, contentDescription = stringResource(R.string.a11y_back_close))
            }
          }
        }

        // အပေါ်ဆုံးအလွှာ (Layer 2): Actions / Search Pill
        Row(
          modifier = Modifier.fillMaxSize(),
          horizontalArrangement = Arrangement.End,
          verticalAlignment = Alignment.CenterVertically
        ) {
          actions(iconBgColor)
        }
      }
    }
  }
}

@Composable
fun SearchPill(
  isSearchActive: Boolean,
  searchQuery: String,
  onSearchActiveChange: (Boolean) -> Unit,
  onSearchQueryChange: (String) -> Unit,
  iconBgColor: Color,
  modifier: Modifier = Modifier,
  otherActions: @Composable RowScope.() -> Unit
) {
  val focusRequester = remember { FocusRequester() }

  LaunchedEffect(isSearchActive) {
    if (isSearchActive) focusRequester.requestFocus()
  }

  Row(
    modifier = modifier
      .padding(end = 16.dp)
      .then(if (isSearchActive) Modifier.fillMaxWidth().padding(start = 16.dp) else Modifier)
      .clip(RoundedCornerShape(50))
      .background(if (isSearchActive) MaterialTheme.colorScheme.surfaceVariant else iconBgColor)
      .animateContentSize(tween(300)),
    verticalAlignment = Alignment.CenterVertically
  ) {
    if (isSearchActive) {
      IconButton(onClick = { onSearchActiveChange(false); onSearchQueryChange("") }) {
        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.a11y_close_search))
      }
      BasicTextField(
        value = searchQuery,
        onValueChange = onSearchQueryChange,
        modifier = Modifier.weight(1f).focusRequester(focusRequester),
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
        singleLine = true,
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        decorationBox = { innerTextField ->
          Box(contentAlignment = Alignment.CenterStart) {
            if (searchQuery.isEmpty()) {
              Text(stringResource(R.string.search_placeholder), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyLarge)
            }
            innerTextField()
          }
        }
      )
      if (searchQuery.isNotEmpty()) {
        IconButton(onClick = { onSearchQueryChange("") }) {
          Icon(Icons.Rounded.Clear, contentDescription = stringResource(R.string.a11y_clear_search))
        }
      }
    } else {
      IconButton(onClick = { onSearchActiveChange(true) }) {
        Icon(Icons.Rounded.Search, contentDescription = stringResource(R.string.a11y_search))
      }
      otherActions()
    }
  }
}
