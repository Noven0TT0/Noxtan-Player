package com.noxtan.player.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.Deselect
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.SdStorage
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil3.compose.AsyncImage
import com.noxtan.player.R
import com.noxtan.player.features.local.viewmodel.LocalVideoViewModel
import com.noxtan.player.presentation.Screen
import com.noxtan.player.presentation.components.CollapsingTopBar
import com.noxtan.player.ui.components.SelectionBottomBar
import com.noxtan.player.ui.components.shareVideos
import com.noxtan.player.ui.home.components.ViewConfigBottomSheet
import com.noxtan.player.ui.utils.LocalBackStack
import com.noxtan.player.ui.utils.topAndBottomNoise
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel

@Serializable
data class VideoListScreen(
  val folderPath: String,
  val folderName: String,
  val navId: Long = System.currentTimeMillis()
) : Screen {

  @android.annotation.SuppressLint("NewApi")
  @OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
  @Composable
  override fun Content() {
    val context = LocalContext.current
    val backstack = LocalBackStack.current

    val activity = remember(context) {
      var ctx = context
      while (ctx is android.content.ContextWrapper) {
        if (ctx is androidx.activity.ComponentActivity) break
        ctx = ctx.baseContext
      }
      ctx as androidx.activity.ComponentActivity
    }

    val viewModel = koinViewModel<LocalVideoViewModel>(viewModelStoreOwner = activity)

    val isAppResuming by viewModel.isAppResuming.collectAsState()
    val isRefreshing by viewModel.isUserRefreshing.collectAsState()
    val isFabVisible by viewModel.isFabVisible.collectAsState()
    val recentVideo by viewModel.getRecentVideoInFolder(folderPath).collectAsState(null)
    val videos by remember(viewModel, folderPath) {
      viewModel.getVideosForFolder(folderPath)
    }.collectAsState()

    val isSelectionMode by viewModel.isSelectionMode.collectAsState()
    val selectedPaths by viewModel.selectedPaths.collectAsState()

    var isSearchActive by remember { mutableStateOf(false) }
    val searchQuery by viewModel.searchQuery.collectAsState()

    androidx.activity.compose.BackHandler(enabled = isSearchActive || isSelectionMode) {
      if (isSelectionMode) {
        viewModel.clearSelection()
      } else if (isSearchActive) {
        isSearchActive = false
        viewModel.onSearchQueryChanged("")
      }
    }

    var showInfoDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var videosToDelete by remember { mutableStateOf<List<com.noxtan.player.data.local.db.VideoEntity>>(emptyList()) }

    val deleteLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
      contract = androidx.activity.result.contract.ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
      if (result.resultCode == android.app.Activity.RESULT_OK) {
        val deletedPaths = videosToDelete.map { it.path }
        viewModel.removeDeletedVideosFromDatabase(deletedPaths)
      } else {
        viewModel.clearSelection()
      }
    }

    var showRenameDialog by remember { mutableStateOf(false) }
    var videoToRename by remember { mutableStateOf<com.noxtan.player.data.local.db.VideoEntity?>(null) }
    var newVideoName by remember { mutableStateOf("") }

    val renameLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
      contract = androidx.activity.result.contract.ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
      if (result.resultCode == android.app.Activity.RESULT_OK) {
        videoToRename?.let { video ->
          val extension = java.io.File(video.path).extension
          val newFileName = if (extension.isNotBlank()) "$newVideoName.$extension" else newVideoName
          val uri = android.content.ContentUris.withAppendedId(android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI, video.id)
          val values = android.content.ContentValues().apply {
            put(android.provider.MediaStore.Video.Media.DISPLAY_NAME, newFileName)
          }
          try {
            context.contentResolver.update(uri, values, null, null)
            viewModel.clearSelection()
            viewModel.silentRefresh()
          } catch (e: Exception) { e.printStackTrace() }
        }
      }
      showRenameDialog = false
    }

    var showMoveDialog by remember { mutableStateOf(false) }
    var videoToMove by remember { mutableStateOf<com.noxtan.player.data.local.db.VideoEntity?>(null) }
    var selectedDestinationPath by remember { mutableStateOf("") }
    val folders by viewModel.folderList.collectAsState()

    var showNewFolderDialog by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }
    var customNewFolders by remember { mutableStateOf<List<java.io.File>>(emptyList()) }

    val moveLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
      contract = androidx.activity.result.contract.ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
      if (result.resultCode == android.app.Activity.RESULT_OK) {
        videoToMove?.let { video ->
          val destDir = java.io.File(selectedDestinationPath)
          val oldFile = java.io.File(video.path)
          val newFile = java.io.File(destDir, oldFile.name)
          val uri = android.content.ContentUris.withAppendedId(android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI, video.id)

          try {
            val basePath = android.os.Environment.getExternalStorageDirectory().absolutePath
            val relativePath = destDir.absolutePath.replace(basePath, "").removePrefix("/") + "/"

            val values = android.content.ContentValues().apply {
              if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
              } else {
                put(android.provider.MediaStore.MediaColumns.DATA, newFile.absolutePath)
              }
            }

            val updated = context.contentResolver.update(uri, values, null, null)
            if (updated > 0) {
              viewModel.clearSelection()
              viewModel.silentRefresh()
            }
          } catch (e: IllegalArgumentException) {
            try {
              context.contentResolver.openInputStream(uri)?.use { input ->
                java.io.FileOutputStream(newFile).use { output ->
                  input.copyTo(output)
                }
              }
              context.contentResolver.delete(uri, null, null)
              viewModel.clearSelection()
              viewModel.silentRefresh()
            } catch (fallbackEx: Exception) {
              android.widget.Toast.makeText(context, "Cannot move to this folder due to Android restrictions. Try 'Movies' or 'DCIM'.", android.widget.Toast.LENGTH_LONG).show()
            }
          } catch (e: Exception) {
            showMoveDialog = false
            showNewFolderDialog = false
          }
        }
      }
      showMoveDialog = false
      showNewFolderDialog = false
    }

    LaunchedEffect(isSearchActive) {
      viewModel.isSearchMode = isSearchActive

      if (isSearchActive) {
        viewModel.setFabVisibility(false, "Search_Mode_Toggle_On")
      } else if (!viewModel.isSelectionModeActive) {
        viewModel.setFabVisibility(true, "Search_Mode_Toggle_Off")
      }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
      val observer = LifecycleEventObserver { _, event ->
        if (event == Lifecycle.Event.ON_RESUME) {
          viewModel.updateCurrentFolder(folderPath)
          if (!isSearchActive && !viewModel.isSelectionModeActive) viewModel.setFabVisibility(true)
        }
      }
      lifecycleOwner.lifecycle.addObserver(observer)
      onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val viewConfig by viewModel.viewConfig.collectAsState()
    var showViewConfigSheet by remember { mutableStateOf(false) }

    var isInitialLoading by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
      kotlinx.coroutines.delay(600)
      isInitialLoading = false
    }

    val density = LocalDensity.current
    val layoutDirection = androidx.compose.ui.platform.LocalLayoutDirection.current
    val navigationBars = WindowInsets.navigationBars
    val leftPadding = with(density) { navigationBars.getLeft(density, layoutDirection).toDp() }
    val rightPadding = with(density) { navigationBars.getRight(density, layoutDirection).toDp() }
    val bottomPadding = with(density) { navigationBars.getBottom(density).toDp() }
    val gridState = rememberLazyGridState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
      state = rememberTopAppBarState(),
      snapAnimationSpec = null
    )

    val nestedScrollConnection = remember(gridState) {
      object : NestedScrollConnection {
        var accumulatedScroll = 0f

        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
          if (isSearchActive || viewModel.isSelectionModeActive) return Offset.Zero

          val delta = available.y

          if (delta > 0) {
            accumulatedScroll = (accumulatedScroll + delta).coerceAtMost(60f)
          } else {
            accumulatedScroll = (accumulatedScroll + delta).coerceAtLeast(-60f)
          }

          if (accumulatedScroll <= -30f) {
            viewModel.setFabVisibility(false, "VideoListScreen:Scroll_Down_Solid")
          } else if (accumulatedScroll >= 30f) {
            viewModel.setFabVisibility(true, "VideoListScreen:Scroll_Up_Solid")
          }

          return Offset.Zero
        }
      }
    }

    Scaffold(
      modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
        .topAndBottomNoise(noiseAlpha = 0.15f, fadeFraction = 0.09f),
      containerColor = MaterialTheme.colorScheme.background,
      topBar = {
        if (isSelectionMode) {
          val isAllSelected = selectedPaths.size == videos.size && videos.isNotEmpty()

          CollapsingTopBar(
            title = "${selectedPaths.size} Selected",
            scrollBehavior = scrollBehavior,
            onBackClick = { viewModel.clearSelection() },
            navIcon = androidx.compose.material.icons.Icons.Rounded.Close,
            isSearchActive = false,
            actions = {
              IconButton(
                onClick = {
                  if (isAllSelected) {
                    viewModel.deselectAll()
                  } else {
                    viewModel.selectAll(videos.map { it.path })
                  }
                }
              ) {
                Icon(
                  imageVector = if (isAllSelected) androidx.compose.material.icons.Icons.Rounded.Deselect else androidx.compose.material.icons.Icons.Rounded.DoneAll,
                  contentDescription = stringResource(if (isAllSelected) R.string.a11y_deselect_all else R.string.a11y_select_all)
                )
              }
            }
          )
        } else {
          CollapsingTopBar(
            title = folderName,
            scrollBehavior = scrollBehavior,
            onBackClick = { backstack.removeLastOrNull() },
            isSearchActive = isSearchActive,
            actions = { iconBgColor ->
              com.noxtan.player.presentation.components.SearchPill(
                isSearchActive = isSearchActive,
                searchQuery = searchQuery,
                onSearchActiveChange = { isSearchActive = it },
                onSearchQueryChange = viewModel::onSearchQueryChanged,
                iconBgColor = iconBgColor,
                otherActions = {}
              )
            }
          )
        }
      }
    ) { padding ->
      Box(
        modifier = Modifier
          .fillMaxSize()
          .nestedScroll(nestedScrollConnection)
      ) {
        if (videos.isEmpty()) {
          if (isInitialLoading || isRefreshing) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
              androidx.compose.material3.CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
          } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
              Text("No videos in this folder", color = MaterialTheme.colorScheme.outline)
            }
          }
        } else {
          val selectedBgColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
          val folderIconBgColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)

          LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Fixed(viewConfig.gridCount),
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = !isAppResuming,
            contentPadding = PaddingValues(
              start = 16.dp + leftPadding,
              top = padding.calculateTopPadding() + 16.dp,
              end = 16.dp + rightPadding,
              bottom = 120.dp + bottomPadding
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            items(videos, key = { it.id }) { video ->
              val isRecent = video.id == recentVideo?.id

              val titleColor = when {
                isRecent -> MaterialTheme.colorScheme.primary
                video.markState == "PLAYED" -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                else -> MaterialTheme.colorScheme.onSurface
              }

              val videoDesc = stringResource(R.string.a11y_video_desc, video.title, formatDuration(video.duration))
              val selectActionLabel = stringResource(R.string.a11y_action_select)
              val playActionLabel = stringResource(R.string.a11y_action_play)
              val isSelected = selectedPaths.contains(video.path)
              val stateDesc = if (isSelectionMode) stringResource(if (isSelected) R.string.a11y_selected else R.string.a11y_not_selected) else null

              if (viewConfig.gridCount == 1) {
                Row(
                  modifier = Modifier
                    .clearAndSetSemantics {
                      contentDescription = videoDesc
                      if (stateDesc != null) stateDescription = stateDesc
                    }
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else Color.Transparent)
                    .combinedClickable(
                      onClickLabel = if (isSelectionMode) selectActionLabel else playActionLabel,
                      onLongClickLabel = selectActionLabel,
                      onClick = {
                        if (isSelectionMode) {
                          viewModel.toggleSelection(video.path)
                        } else {
                          HomeScreen.playFile(video.path, context)
                        }
                      },
                      onLongClick = {
                        viewModel.toggleSelection(video.path)
                      }
                    )
                    .padding(8.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Box(
                    modifier = Modifier
                      .width(144.dp)
                      .aspectRatio(16f / 9f)
                      .clip(RoundedCornerShape(12.dp))
                      .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.BottomEnd
                  ) {
                    AsyncImage(
                      model = video.thumbnailPath ?: java.io.File(video.path),
                      contentDescription = "Thumbnail",
                      contentScale = ContentScale.Crop,
                      modifier = Modifier.fillMaxSize()
                    )
                    if (video.markState == "NEW") {
                      Text(
                        text = "NEW",
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.labelSmall.copy(
                          fontWeight = FontWeight.ExtraBold,
                          fontSize = 10.sp,
                          letterSpacing = 0.5.sp
                        ),
                        modifier = Modifier
                          .align(Alignment.TopStart)
                          .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(bottomEnd = 12.dp)
                          )
                          .padding(horizontal = 8.dp, vertical = 4.dp)
                      )
                    }
                  }

                  Spacer(modifier = Modifier.width(16.dp))

                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = video.title,
                      style = MaterialTheme.typography.titleMedium,
                      color = titleColor,
                      fontWeight = FontWeight.Bold,
                      maxLines = 2,
                      overflow = TextOverflow.Ellipsis,
                      lineHeight = 20.sp
                    )

                    if (viewConfig.showSize || (viewConfig.showResolution && video.resolution.isNotBlank())) {
                      Spacer(modifier = Modifier.height(6.dp))
                      Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                      ) {
                        if (viewConfig.showSize) {
                          Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                              imageVector = Icons.Rounded.SdStorage,
                              contentDescription = null,
                              tint = MaterialTheme.colorScheme.onSurfaceVariant,
                              modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                              text = formatFileSizeUS(context, video.size),
                              style = MaterialTheme.typography.bodySmall,
                              color = MaterialTheme.colorScheme.onSurfaceVariant,
                              fontWeight = FontWeight.Medium
                            )
                          }
                        }

                        if (viewConfig.showResolution && video.resolution.isNotBlank() && video.resolution != "0x0") {
                          Text(
                            text = formatResolution(video.resolution),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                          )
                        }

                        if (viewConfig.showExtension) {
                          val extension = java.io.File(video.path).extension.uppercase()
                          if (extension.isNotBlank()) {
                            Text(
                              text = extension,
                              style = MaterialTheme.typography.bodySmall,
                              color = MaterialTheme.colorScheme.onSurfaceVariant,
                              fontWeight = FontWeight.Bold
                            )
                          }
                        }
                      }
                    }
                  }
                }
              } else {
                Column(
                  modifier = Modifier
                    .clearAndSetSemantics {
                      contentDescription = videoDesc
                      if (stateDesc != null) stateDescription = stateDesc
                    }
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else Color.Transparent)
                    .combinedClickable(
                      onClickLabel = if (isSelectionMode) selectActionLabel else playActionLabel,
                      onLongClickLabel = selectActionLabel,
                      onClick = {
                        if (isSelectionMode) {
                          viewModel.toggleSelection(video.path)
                        } else {
                          HomeScreen.playFile(video.path, context)
                        }
                      },
                      onLongClick = {
                        viewModel.toggleSelection(video.path)
                      }
                    )
                    .padding(8.dp)
                ) {
                  Box(
                    modifier = Modifier
                      .fillMaxWidth()
                      .aspectRatio(16f / 9f)
                      .clip(RoundedCornerShape(12.dp))
                      .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.BottomEnd
                  ) {
                    AsyncImage(
                      model = video.thumbnailPath ?: java.io.File(video.path),
                      contentDescription = null,
                      contentScale = ContentScale.Crop,
                      modifier = Modifier.fillMaxSize()
                    )
                    if (video.markState == "NEW") {
                      Text(
                        text = "NEW",
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.labelSmall.copy(
                          fontWeight = FontWeight.ExtraBold,
                          fontSize = 10.sp,
                          letterSpacing = 0.5.sp
                        ),
                        modifier = Modifier
                          .align(Alignment.TopStart)
                          .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(bottomEnd = 12.dp)
                          )
                          .padding(horizontal = 8.dp, vertical = 4.dp)
                      )
                    }
                    if (viewConfig.showDuration) {
                      Text(
                        text = formatDuration(video.duration),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = Color.White,
                        modifier = Modifier
                          .padding(6.dp)
                          .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(6.dp))
                          .padding(horizontal = 6.dp, vertical = 2.dp)
                      )
                    }
                    if (selectedPaths.contains(video.path)) {
                      Icon(
                        imageVector = androidx.compose.material.icons.Icons.Rounded.CheckCircle,
                        contentDescription = "Selected",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                          .align(Alignment.TopEnd)
                          .padding(4.dp)
                          .size(24.dp)
                          .background(MaterialTheme.colorScheme.background, androidx.compose.foundation.shape.CircleShape)
                      )
                    }
                  }
                  Spacer(modifier = Modifier.height(8.dp))
                  Text(
                    text = video.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = titleColor,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 18.sp
                  )

                  if (viewConfig.showSize || (viewConfig.showResolution && video.resolution.isNotBlank() && video.resolution != "0x0") || viewConfig.showExtension) {
                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {

                      if (viewConfig.showSize) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                          Icon(
                            imageVector = Icons.Rounded.SdStorage,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(12.dp)
                          )
                          Spacer(modifier = Modifier.width(4.dp))
                          Text(
                            text = formatFileSizeUS(context, video.size),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                          )
                        }
                      }

                      if (viewConfig.showResolution && video.resolution.isNotBlank() && video.resolution != "0x0") {
                        Text(
                          text = formatResolution(video.resolution),
                          style = MaterialTheme.typography.bodySmall,
                          color = MaterialTheme.colorScheme.onSurfaceVariant,
                          fontWeight = FontWeight.Bold
                        )
                      }

                      if (viewConfig.showExtension) {
                        val extension = java.io.File(video.path).extension.uppercase()
                        if (extension.isNotBlank()) {
                          Text(
                            text = extension,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                          )
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }
        SelectionBottomBar(
          visible = isSelectionMode,
          selectedCount = selectedPaths.size,
          showRename = true,
          showMove = true,
          onShareClick = {
            val videosToShare = videos.filter { it.path in selectedPaths }
            if (videosToShare.isNotEmpty()) {
              shareVideos(context, videosToShare)
              viewModel.clearSelection()
            }
          },
          onMarkAsSelected = { state ->
            viewModel.markSelectedVideosAs(state.name, isFolderMode = false)
          },
          onRenameClick = {
            val targetVideo = videos.find { it.path == selectedPaths.first() }
            if (targetVideo != null) {
              videoToRename = targetVideo
              newVideoName = java.io.File(targetVideo.path).nameWithoutExtension
              showRenameDialog = true
            }
          },
          onMoveClick = {
            val targetVideo = videos.find { it.path == selectedPaths.first() }
            if (targetVideo != null) {
              videoToMove = targetVideo
              selectedDestinationPath = ""
              showMoveDialog = true
            }
          },
          onInfoClick = { showInfoDialog = true },
          onDeleteClick = {
            videosToDelete = videos.filter { it.path in selectedPaths }
            if (videosToDelete.isNotEmpty()) {
              showDeleteConfirmDialog = true
            }
          },
          modifier = Modifier.align(Alignment.BottomCenter)
        )
      }
    }

    if (showInfoDialog && selectedPaths.isNotEmpty()) {
      val selectedVideos = videos.filter { it.path in selectedPaths }

      if (selectedVideos.isNotEmpty()) {
        val isSingle = selectedVideos.size == 1

        val title = "Properties"
        val name = if (isSingle) selectedVideos.first().title else "${selectedVideos.size} Videos Selected"
        val path = if (isSingle) selectedVideos.first().path else "Multiple Locations"

        val totalSize = selectedVideos.sumOf { it.size }
        val totalDuration = selectedVideos.sumOf { it.duration }

        com.noxtan.player.ui.components.MediaInfoBottomSheet(
          title = title,
          name = name,
          path = path,
          size = formatFileSizeUS(context, totalSize),
          extraInfoLabel = "Total Duration",
          extraInfoValue = formatDuration(totalDuration),
          onDismiss = { showInfoDialog = false }
        )
      } else {
        showInfoDialog = false
      }
    }

    if (showDeleteConfirmDialog) {
      com.noxtan.player.presentation.components.ConfirmDialog(
        title = "Delete Videos?",
        subtitle = "Are you sure you want to permanently delete ${videosToDelete.size} video(s) from device storage? This action cannot be undone.",
        onConfirm = {
          showDeleteConfirmDialog = false

          if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            try {
              val urisToDelete = videosToDelete.mapNotNull { video ->
                if (video.id > 0) {
                  android.content.ContentUris.withAppendedId(android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI, video.id)
                } else null
              }
              val pendingIntent = android.provider.MediaStore.createDeleteRequest(context.contentResolver, urisToDelete)
              deleteLauncher.launch(androidx.activity.result.IntentSenderRequest.Builder(pendingIntent).build())
            } catch (e: Exception) {
              e.printStackTrace()
            }
          } else {
            val deletedPaths = mutableListOf<String>()
            videosToDelete.forEach { video ->
              val file = java.io.File(video.path)
              if (file.exists() && file.delete()) {
                deletedPaths.add(video.path)
              }
            }
            viewModel.removeDeletedVideosFromDatabase(deletedPaths)
          }
        },
        onCancel = { showDeleteConfirmDialog = false }
      )
    }

    if (showRenameDialog && videoToRename != null) {
      val video = videoToRename!!
      val originalFile = java.io.File(video.path)
      val originalName = originalFile.nameWithoutExtension
      val extension = originalFile.extension

      val isNameChanged = newVideoName != originalName
      val isNameEmpty = newVideoName.isBlank()

      val newFileNameFull = if (extension.isNotBlank()) "$newVideoName.$extension" else newVideoName
      val isFileExists = isNameChanged && java.io.File(originalFile.parent, newFileNameFull).exists()

      val canSave = isNameChanged && !isNameEmpty && !isFileExists

      androidx.compose.material3.AlertDialog(
        onDismissRequest = { showRenameDialog = false },
        title = { Text("Rename Video", fontWeight = FontWeight.Bold) },
        text = {
          androidx.compose.material3.OutlinedTextField(
            value = newVideoName,
            onValueChange = { newVideoName = it },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            suffix = {
              if (extension.isNotBlank()) {
                Text(text = ".$extension", color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
            },
            isError = isFileExists,
            supportingText = {
              if (isFileExists) {
                Text("A file with this name already exists.", color = MaterialTheme.colorScheme.error)
              }
            }
          )
        },
        confirmButton = {
          androidx.compose.material3.TextButton(
            onClick = {
              val uri = android.content.ContentUris.withAppendedId(android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI, video.id)
              val values = android.content.ContentValues().apply {
                put(android.provider.MediaStore.Video.Media.DISPLAY_NAME, newFileNameFull)
              }

              try {
                val updated = context.contentResolver.update(uri, values, null, null)
                if (updated == 0) {
                  val newFile = java.io.File(originalFile.parent, newFileNameFull)
                  originalFile.renameTo(newFile)
                }
                showRenameDialog = false
                viewModel.clearSelection()
                viewModel.silentRefresh()
              } catch (e: SecurityException) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                  val pendingIntent = android.provider.MediaStore.createWriteRequest(context.contentResolver, listOf(uri))
                  renameLauncher.launch(androidx.activity.result.IntentSenderRequest.Builder(pendingIntent).build())
                } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                  val recoverable = e as? android.app.RecoverableSecurityException
                  recoverable?.userAction?.actionIntent?.let {
                    renameLauncher.launch(androidx.activity.result.IntentSenderRequest.Builder(it).build())
                  }
                } else {
                  showRenameDialog = false
                }
              } catch (e: Exception) {
                showRenameDialog = false
              }
            },
            enabled = canSave
          ) {
            Text("Rename", fontWeight = FontWeight.Bold)
          }
        },
        dismissButton = {
          androidx.compose.material3.TextButton(onClick = { showRenameDialog = false }) {
            Text("Cancel")
          }
        }
      )
    }

    val executeMove = { destinationPath: String ->
      val video = videoToMove!!
      val oldFile = java.io.File(video.path)
      val destDir = java.io.File(destinationPath)
      val newFile = java.io.File(destDir, oldFile.name)
      val uri = android.content.ContentUris.withAppendedId(android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI, video.id)

      try {
        if (oldFile.renameTo(newFile)) {
          showMoveDialog = false
          showNewFolderDialog = false
          viewModel.clearSelection()
          viewModel.silentRefresh()
        } else {
          val basePath = android.os.Environment.getExternalStorageDirectory().absolutePath
          val relativePath = destDir.absolutePath.replace(basePath, "").removePrefix("/") + "/"
          val values = android.content.ContentValues().apply {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
              put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
            } else {
              put(android.provider.MediaStore.MediaColumns.DATA, newFile.absolutePath)
            }
          }
          val updated = context.contentResolver.update(uri, values, null, null)
          if (updated > 0) {
            showMoveDialog = false
            showNewFolderDialog = false
            viewModel.clearSelection()
            viewModel.silentRefresh()
          } else {
            throw SecurityException("Need permission")
          }
        }
      } catch (e: SecurityException) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
          val pendingIntent = android.provider.MediaStore.createWriteRequest(context.contentResolver, listOf(uri))
          moveLauncher.launch(androidx.activity.result.IntentSenderRequest.Builder(pendingIntent).build())
        } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
          val recoverable = e as? android.app.RecoverableSecurityException
          recoverable?.userAction?.actionIntent?.let {
            moveLauncher.launch(androidx.activity.result.IntentSenderRequest.Builder(it).build())
          }
        } else {
          showMoveDialog = false
          showNewFolderDialog = false
        }
      } catch (e: IllegalArgumentException) {
        try {
          context.contentResolver.openInputStream(uri)?.use { input ->
            java.io.FileOutputStream(newFile).use { output ->
              input.copyTo(output)
            }
          }
          context.contentResolver.delete(uri, null, null)
          showMoveDialog = false
          showNewFolderDialog = false
          viewModel.clearSelection()
          viewModel.silentRefresh()
        } catch (fallbackEx: Exception) {
          android.widget.Toast.makeText(context, "Cannot move to this folder. Try 'Movies' or 'DCIM'.", android.widget.Toast.LENGTH_LONG).show()
          showMoveDialog = false
          showNewFolderDialog = false
        }
      } catch (e: Exception) {
        showMoveDialog = false
        showNewFolderDialog = false
      }
    }

    if (showMoveDialog && videoToMove != null) {
      androidx.compose.material3.AlertDialog(
        onDismissRequest = { showMoveDialog = false },
        title = { Text("Move to...", fontWeight = FontWeight.Bold) },
        text = {
          androidx.compose.foundation.lazy.LazyColumn(
            modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {

            item {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(12.dp))
                  .clickable {
                    newFolderName = ""
                    showNewFolderDialog = true
                  }
                  .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Box(
                  modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(
                    imageVector = androidx.compose.material.icons.Icons.Rounded.CreateNewFolder,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(24.dp)
                  )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                  text = "Create New Folder",
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.primary
                )
              }
            }

            val existingPaths = folders.map { it.path }
            val combinedFolders = folders + customNewFolders
              .filter { it.absolutePath !in existingPaths }
              .map { com.noxtan.player.features.local.viewmodel.VideoFolder(it.name, it.absolutePath, 0, 0L, 0L) }

            items(combinedFolders.filter { it.path != folderPath }) { folder ->
              val allowedRoots = listOf("/Movies", "/DCIM", "/Pictures", "/Download")
              val isAllowed = allowedRoots.any { folder.path.contains(it, ignoreCase = true) }
              val isSelected = selectedDestinationPath == folder.path && isAllowed

              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(12.dp))
                  .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                  .then(
                    if (isAllowed) Modifier.clickable { selectedDestinationPath = folder.path }
                    else Modifier
                  )
                  .padding(12.dp)
                  .graphicsLayer { alpha = if (isAllowed) 1f else 0.4f },
                verticalAlignment = Alignment.CenterVertically
              ) {
                Box(
                  modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(
                    imageVector = if (isAllowed) androidx.compose.material.icons.Icons.Rounded.FolderOpen else androidx.compose.material.icons.Icons.Rounded.Block,
                    contentDescription = null,
                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                  )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                  Text(
                    text = folder.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                  )
                  Text(
                    text = if (isAllowed) {
                      if (folder.videoCount == 0) "New Folder" else "${folder.videoCount} videos"
                    } else "System Restricted",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }
            }
          }
        },
        confirmButton = {
          androidx.compose.material3.TextButton(
            onClick = {
              if (selectedDestinationPath.isNotBlank()) {
                executeMove(selectedDestinationPath)
              }
            },
            enabled = selectedDestinationPath.isNotBlank()
          ) {
            Text("Move", fontWeight = FontWeight.Bold)
          }
        },
        dismissButton = {
          androidx.compose.material3.TextButton(onClick = { showMoveDialog = false }) {
            Text("Cancel")
          }
        }
      )
    }

    if (showNewFolderDialog) {
      androidx.compose.material3.AlertDialog(
        onDismissRequest = { showNewFolderDialog = false },
        title = { Text("New Folder", fontWeight = FontWeight.Bold) },
        text = {
          androidx.compose.material3.OutlinedTextField(
            value = newFolderName,
            onValueChange = { newFolderName = it },
            label = { Text("Folder Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
          )
        },
        confirmButton = {
          androidx.compose.material3.TextButton(
            onClick = {
              if (newFolderName.isNotBlank()) {
                val moviesDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_MOVIES)
                val newDir = java.io.File(moviesDir, newFolderName)
                newDir.mkdirs()

                customNewFolders = customNewFolders + newDir
                selectedDestinationPath = newDir.absolutePath

                executeMove(newDir.absolutePath)
              }
            },
            enabled = newFolderName.isNotBlank()
          ) {
            Text("Create & Move", fontWeight = FontWeight.Bold)
          }
        },
        dismissButton = {
          androidx.compose.material3.TextButton(onClick = { showNewFolderDialog = false }) {
            Text("Cancel")
          }
        }
      )
    }

    if (showViewConfigSheet) {
      ViewConfigBottomSheet(
        config = viewConfig,
        onConfigChange = viewModel::updateViewConfig,
        onDismiss = { showViewConfigSheet = false }
      )
    }
  }

  private fun formatFileSizeUS(context: android.content.Context, size: Long): String {
    return android.text.format.Formatter.formatFileSize(context, size)
      .replace('၀', '0').replace('၁', '1').replace('၂', '2')
      .replace('၃', '3').replace('၄', '4').replace('၅', '5')
      .replace('၆', '6').replace('၇', '7').replace('၈', '8')
      .replace('၉', '9')
  }

  private fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
      String.format(java.util.Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
      String.format(java.util.Locale.US, "%02d:%02d", minutes, seconds)
    }
  }

  private fun formatResolution(resolution: String): String {
    if (resolution.isBlank() || resolution == "0x0") return ""
    val parts = resolution.lowercase().split("x")
    if (parts.size == 2) {
      val width = parts[0].toIntOrNull() ?: 0
      val height = parts[1].toIntOrNull() ?: 0
      if (width >= 3840 || height >= 2160) return "4K"
      if (height > 0) return "${height}p"
    }
    return resolution
  }
}
