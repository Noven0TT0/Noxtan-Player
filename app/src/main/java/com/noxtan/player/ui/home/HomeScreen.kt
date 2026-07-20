package com.noxtan.player.ui.home

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Deselect
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.SdStorage
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
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
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil3.compose.AsyncImage
import com.noxtan.player.R
import com.noxtan.player.features.local.viewmodel.LocalVideoViewModel
import com.noxtan.player.presentation.Screen
import com.noxtan.player.presentation.components.CollapsingTopBar
import com.noxtan.player.presentation.components.ConfirmDialog
import com.noxtan.player.ui.components.SelectionBottomBar
import com.noxtan.player.ui.components.shareVideos
import com.noxtan.player.ui.home.components.ViewConfigBottomSheet
import com.noxtan.player.ui.utils.LocalBackStack
import com.noxtan.player.ui.utils.topAndBottomNoise
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel
import java.io.File

@Serializable
object HomeScreen : Screen {
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
    val recentVideo by viewModel.getGlobalRecentVideo().collectAsState(null)

    val recentVideoParent = remember(recentVideo) {
      recentVideo?.path?.let { java.io.File(it).parent } ?: ""
    }

    val folders by viewModel.folderList.collectAsState()

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

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var videosToDelete by remember { mutableStateOf<List<com.noxtan.player.data.local.db.VideoEntity>>(emptyList()) }

    val deleteLauncher = rememberLauncherForActivityResult(
      contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
      if (result.resultCode == Activity.RESULT_OK) {
        val deletedPaths = videosToDelete.map { it.path }
        viewModel.removeDeletedVideosFromDatabase(deletedPaths)
      } else {
        viewModel.clearSelection()
      }
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
          viewModel.updateCurrentFolder(null)
          if (!isSearchActive && !viewModel.isSelectionModeActive) viewModel.setFabVisibility(true)
        }
      }
      lifecycleOwner.lifecycle.addObserver(observer)
      onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val permissionsToCheck = when {
      Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> {
        arrayOf(Manifest.permission.READ_MEDIA_VIDEO, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
      }
      Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
        arrayOf(Manifest.permission.READ_MEDIA_VIDEO)
      }
      else -> {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
      }
    }

    var hasPermission by remember {
      mutableStateOf(
        permissionsToCheck.any {
          ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
      )
    }

    var isInitialLoading by remember { mutableStateOf(true) }

    val launcher = rememberLauncherForActivityResult(
      contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
      val isGranted = permissions.values.any { it }
      if (isGranted) {
        isInitialLoading = true
        hasPermission = true
      } else {
        hasPermission = false
      }
    }

    LaunchedEffect(hasPermission) {
      if (hasPermission) {
        isInitialLoading = true
        viewModel.silentRefresh()
        kotlinx.coroutines.delay(1000)
        isInitialLoading = false
      } else {
        isInitialLoading = false
      }
    }

    val viewConfig by viewModel.viewConfig.collectAsState()
    var showViewConfigSheet by remember { mutableStateOf(false) }

    var showInfoDialog by remember { mutableStateOf(false) }

    val density = LocalDensity.current
    val layoutDirection = androidx.compose.ui.platform.LocalLayoutDirection.current
    val navigationBars = WindowInsets.navigationBars
    val leftPadding = with(density) { navigationBars.getLeft(density, layoutDirection).toDp() }
    val rightPadding = with(density) { navigationBars.getRight(density, layoutDirection).toDp() }
    val bottomPadding = with(density) { navigationBars.getBottom(density).toDp() }

    val gridState = rememberLazyGridState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
      state = rememberTopAppBarState(),
      snapAnimationSpec = null // Snapping (အတင်းဆွဲကပ်ခြင်း) ကို ပိတ်လိုက်ပြီး ကြိုက်တဲ့နေရာမှာ ရပ်ခွင့်ပြုသည်
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
            viewModel.setFabVisibility(false, "HomeScreen:Scroll_Down_Solid")
          } else if (accumulatedScroll >= 30f) {
            viewModel.setFabVisibility(true, "HomeScreen:Scroll_Up_Solid")
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
          val isAllSelected = selectedPaths.size == folders.size && folders.isNotEmpty()

          CollapsingTopBar(
            title = "${selectedPaths.size} Selected",
            scrollBehavior = scrollBehavior,
            onBackClick = { viewModel.clearSelection() },
            navIcon = Icons.Rounded.Close,
            isSearchActive = false,
            actions = {
              IconButton(
                onClick = {
                  if (isAllSelected) {
                    viewModel.deselectAll()
                  } else {
                    viewModel.selectAll(folders.map { it.path })
                  }
                }
              ) {
                Icon(
                  imageVector = if (isAllSelected) Icons.Rounded.Deselect else Icons.Rounded.DoneAll,
                  contentDescription = stringResource(if (isAllSelected) R.string.a11y_deselect_all else R.string.a11y_select_all)
                )
              }
            }
          )
        } else {
          CollapsingTopBar(
            title = "Folders",
            scrollBehavior = scrollBehavior,
            onBackClick = null,
            isSearchActive = isSearchActive,
            actions = { iconBgColor ->
              com.noxtan.player.presentation.components.SearchPill(
                isSearchActive = isSearchActive,
                searchQuery = searchQuery,
                onSearchActiveChange = { isSearchActive = it },
                onSearchQueryChange = viewModel::onSearchQueryChanged,
                iconBgColor = iconBgColor,
                otherActions = {
                  IconButton(onClick = { showViewConfigSheet = true }) {
                    Icon(Icons.Rounded.GridView, contentDescription = stringResource(R.string.a11y_change_layout))
                  }
                  IconButton(onClick = { backstack.add(com.noxtan.player.ui.Settings.SettingsScreen) }) {
                    Icon(Icons.Rounded.Settings, contentDescription = stringResource(R.string.a11y_settings))
                  }
                }
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
        if (!hasPermission) {
          Column(
            modifier = Modifier
              .fillMaxSize()
              .verticalScroll(rememberScrollState())
              .padding(padding) // Top Bar ၏နောက်ကွယ်သို့ ရောက်မသွားစေရန် အောက်သို့ တွန်းချလိုက်ပါသည်
              .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            // App Logo
            androidx.compose.material3.Surface(
              modifier = Modifier.size(200.dp),
              color = androidx.compose.ui.graphics.Color.Transparent, // ဘောင်အရောင်ကို ဖျောက်လိုက်ပါသည်
              tonalElevation = 0.dp                                  // Shadow ကို ဖျောက်လိုက်ပါသည်
            ) {
              Box(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  painter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_noxtan_logo),
                  contentDescription = stringResource(R.string.a11y_app_logo),
                  modifier = Modifier.fillMaxSize(),
                  tint = androidx.compose.ui.graphics.Color.Unspecified
                )
              }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
              text = stringResource(R.string.welcome_to_noxtan),
              style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
              color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
              text = stringResource(R.string.permission_description),
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              textAlign = androidx.compose.ui.text.style.TextAlign.Center,
              lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
              onClick = { launcher.launch(permissionsToCheck) },
              modifier = Modifier.fillMaxWidth().height(50.dp),
              shape = RoundedCornerShape(12.dp)
            ) {
              Icon(Icons.Rounded.FolderOpen, contentDescription = null, modifier = Modifier.size(20.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text(stringResource(R.string.grant_storage_access), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Open Source Link
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(
                  role = androidx.compose.ui.semantics.Role.Button,
                  onClickLabel = stringResource(R.string.open_source)
                ) {
                  context.startActivity(
                    Intent(Intent.ACTION_VIEW, android.net.Uri.parse(context.getString(R.string.github_repo_url)))
                  )
                }
                .padding(8.dp)
                .semantics(mergeDescendants = true) {}
            ) {
              Icon(
                imageVector = androidx.compose.material.icons.Icons.Rounded.Code,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = stringResource(R.string.open_source),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
              )
            }
          }
        } else {
          val searchResults by remember(viewModel) {
            viewModel.getVideosForFolder(null)
          }.collectAsState()

          if (isSearchActive && searchQuery.isNotEmpty()) {
            if (searchResults.isEmpty()) {
              if (isInitialLoading || isRefreshing) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                  androidx.compose.material3.CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
              } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                  Text("No videos found", color = MaterialTheme.colorScheme.outline)
                }
              }
            } else {
              val listState = rememberLazyListState()
              LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                userScrollEnabled = !isAppResuming,
                contentPadding = PaddingValues(
                  start = 16.dp + leftPadding,
                  top = padding.calculateTopPadding() + 16.dp,
                  end = 16.dp + rightPadding,
                  bottom = 120.dp + bottomPadding
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                items(searchResults, key = { it.id }) { video ->
                  val isRecent = video.id == recentVideo?.id

                  val videoDesc = stringResource(R.string.a11y_video_desc, video.title, formatDuration(video.duration))
                  Row(
                    modifier = Modifier
                      .clearAndSetSemantics {
                        contentDescription = videoDesc
                      }
                      .fillMaxWidth()
                      .clip(RoundedCornerShape(16.dp))
                      .clickable(onClickLabel = stringResource(R.string.a11y_action_play)) {
                        viewModel.onSearchQueryChanged("")
                        isSearchActive = false
                        playFile(video.path, context)
                      }
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

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                      Text(
                        text = video.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isRecent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
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
                              Text(text = formatFileSizeUS(context, video.size),
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
                }
              }
            }
          } else {
            if (folders.isEmpty()) {
              if (isInitialLoading || isRefreshing) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                  androidx.compose.material3.CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
              } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                  Text("No videos found on device", color = MaterialTheme.colorScheme.outline)
                }
              }
            } else {
              // Color Object အသစ်တွေ အကြိမ်ကြိမ်မဆောက်အောင် LazyVerticalGrid အပြင်မှာ တစ်ကြိမ်တည်း ကြိုတင်သတ်မှတ်ထားပါသည်
              val selectedBgColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
              val itemBgColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)

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
                items(folders, key = { it.path }) { folder ->
                  val isRecentFolder = recentVideoParent == folder.path
                  val isSelected = selectedPaths.contains(folder.path)

                  val folderDesc = stringResource(R.string.a11y_folder_desc, folder.name, folder.videoCount)
                  val selectActionLabel = stringResource(R.string.a11y_action_select)
                  val openActionLabel = stringResource(R.string.a11y_action_open_folder)
                  val stateDesc = if (isSelectionMode) stringResource(if (isSelected) R.string.a11y_selected else R.string.a11y_not_selected) else null

                  if (viewConfig.gridCount == 1) {
                    Row(
                      modifier = Modifier
                        .clearAndSetSemantics {
                          contentDescription = folderDesc
                          if (stateDesc != null) stateDescription = stateDesc
                        }
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSelected) selectedBgColor else Color.Transparent) // Pre-allocated Color အား သုံးထားပါသည်
                        .combinedClickable(
                          onClickLabel = if (isSelectionMode) selectActionLabel else openActionLabel,
                          onLongClickLabel = selectActionLabel,
                          onClick = {
                            if (isSelectionMode) {
                              viewModel.toggleSelection(folder.path)
                            } else {
                              viewModel.onSearchQueryChanged("")
                              isSearchActive = false
                              viewModel.updateCurrentFolder(folder.path)
                              backstack.add(VideoListScreen(folder.path, folder.name))
                            }
                          },
                          onLongClick = {
                            viewModel.toggleSelection(folder.path)
                          }
                        )
                        .padding(12.dp),
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      Box(
                        modifier = Modifier
                          .size(56.dp)
                          .clip(RoundedCornerShape(16.dp))
                          .background(itemBgColor), // Pre-allocated Color အား သုံးထားပါသည်
                        contentAlignment = Alignment.Center
                      ) {
                        Icon(
                          imageVector = Icons.Rounded.FolderOpen,
                          contentDescription = null,
                          modifier = Modifier.size(28.dp),
                          tint = MaterialTheme.colorScheme.primary
                        )
                        if (isSelected) {
                          Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = "Selected",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                              .align(Alignment.BottomEnd)
                              .size(20.dp)
                              .background(MaterialTheme.colorScheme.background, CircleShape)
                          )
                        }
                      }

                      Spacer(modifier = Modifier.width(16.dp))

                      Column(modifier = Modifier.weight(1f)) {
                        Text(
                          text = folder.name,
                          style = MaterialTheme.typography.titleMedium,
                          fontWeight = FontWeight.Bold,
                          color = if (isRecentFolder) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
                          maxLines = 1,
                          overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                          text = "${folder.videoCount} videos",
                          style = MaterialTheme.typography.bodySmall,
                          color = MaterialTheme.colorScheme.onSurfaceVariant,
                          fontWeight = FontWeight.Medium
                        )
                      }

                      Icon(
                        imageVector = Icons.Rounded.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(24.dp)
                      )
                    }
                  } else {
                    Column(
                      modifier = Modifier
                        .clearAndSetSemantics {
                          contentDescription = folderDesc
                          if (stateDesc != null) stateDescription = stateDesc
                        }
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else Color.Transparent)
                        .combinedClickable(
                          onClickLabel = if (isSelectionMode) selectActionLabel else openActionLabel,
                          onLongClickLabel = selectActionLabel,
                          onClick = {
                            if (isSelectionMode) {
                              viewModel.toggleSelection(folder.path)
                            } else {
                              viewModel.onSearchQueryChanged("")
                              isSearchActive = false
                              viewModel.updateCurrentFolder(folder.path)
                              backstack.add(VideoListScreen(folder.path, folder.name))
                            }
                          },
                          onLongClick = {
                            viewModel.toggleSelection(folder.path)
                          }
                        )
                        .padding(12.dp),
                      horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                      Box(
                        modifier = Modifier
                          .size(72.dp)
                          .clip(RoundedCornerShape(16.dp))
                          .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center
                      ) {
                        Icon(Icons.Rounded.FolderOpen, contentDescription = null, modifier = Modifier.size(36.dp), tint = MaterialTheme.colorScheme.primary)
                        if (isSelected) {
                          Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = "Selected",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                              .align(Alignment.BottomEnd)
                              .size(24.dp)
                              .background(MaterialTheme.colorScheme.background, CircleShape)
                          )
                        }
                      }
                      Spacer(modifier = Modifier.height(12.dp))
                      Text(
                        text = folder.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isRecentFolder) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                      )
                      Spacer(modifier = Modifier.height(4.dp))
                      Text(text = "${folder.videoCount} videos", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
          showRename = false,
          showMove = false,
          onShareClick = {
            val videosToShare = viewModel.getVideosInSelectedFolders()
            if (videosToShare.isNotEmpty()) {
              shareVideos(context, videosToShare)
              viewModel.clearSelection()
            }
          },
          onRenameClick = {},
          onMoveClick = {},
          onInfoClick = { showInfoDialog = true },
          onDeleteClick = {
            videosToDelete = viewModel.getVideosInSelectedFolders()
            if (videosToDelete.isNotEmpty()) {
              showDeleteConfirmDialog = true
            }
          },
          modifier = Modifier.align(Alignment.BottomCenter)
        )

      }
    }

    if (showDeleteConfirmDialog) {
      ConfirmDialog(
        title = "Delete Videos?",
        subtitle = "Are you sure you want to permanently delete ${videosToDelete.size} video(s) from device storage? This action cannot be undone.",
        onConfirm = {
          showDeleteConfirmDialog = false

          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
              val urisToDelete = videosToDelete.mapNotNull { video ->
                val videoId = video.id
                if (videoId > 0) {
                  android.content.ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, videoId)
                } else null
              }

              val pendingIntent = MediaStore.createDeleteRequest(context.contentResolver, urisToDelete)
              deleteLauncher.launch(IntentSenderRequest.Builder(pendingIntent).build())
            } catch (e: Exception) {
              e.printStackTrace()
            }
          } else {
            val deletedPaths = mutableListOf<String>()
            videosToDelete.forEach { video ->
              val file = File(video.path)
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

    if (showInfoDialog && selectedPaths.isNotEmpty()) {
      val selectedFolders = folders.filter { it.path in selectedPaths }

      if (selectedFolders.isNotEmpty()) {
        val isSingle = selectedFolders.size == 1

        val title = "Properties"
        val name = if (isSingle) selectedFolders.first().name else "${selectedFolders.size} Folders Selected"
        val path = if (isSingle) selectedFolders.first().path else "Multiple Locations"

        val totalSize = selectedFolders.sumOf { it.totalSize }
        val totalVideos = selectedFolders.sumOf { it.videoCount }

        com.noxtan.player.ui.components.MediaInfoBottomSheet(
          title = title,
          name = name,
          path = path,
          size = formatFileSizeUS(context, totalSize),
          extraInfoLabel = "Contains",
          extraInfoValue = "$totalVideos videos",
          onDismiss = { showInfoDialog = false }
        )
      } else {
        showInfoDialog = false
      }
    }

    if (showViewConfigSheet) {
      ViewConfigBottomSheet(
        config = viewConfig,
        onConfigChange = viewModel::updateViewConfig,
        onDismiss = { showViewConfigSheet = false }
      )
    }
  }

  fun playFile(
    filepath: String,
    context: Context,
  ) {
    val fileUri = android.net.Uri.fromFile(java.io.File(filepath))
    val i = Intent(Intent.ACTION_VIEW, fileUri)
    i.setClass(context, com.noxtan.player.ui.player.PlayerActivity::class.java)

    i.putExtra("from_app", true)
    context.startActivity(i)
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
