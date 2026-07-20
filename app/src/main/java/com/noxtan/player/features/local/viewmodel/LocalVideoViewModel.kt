package com.noxtan.player.features.local.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.noxtan.player.data.local.db.VideoEntity
import com.noxtan.player.data.repository.VideoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

enum class SortType { NAME, DATE, SIZE, DURATION }

data class ViewConfig(
  val gridCount: Int = 1,
  val showSize: Boolean = true,
  val showDuration: Boolean = true,
  val showResolution: Boolean = true,
  val showExtension: Boolean = true,
  val sortBy: SortType = SortType.NAME,
  val isAscending: Boolean = true
)

data class VideoFolder(
  val name: String,
  val path: String,
  val videoCount: Int,
  val totalSize: Long = 0L,
  val latestDate: Long = 0L,
  val hasNewVideos: Boolean = false
)

class LocalVideoViewModel(
  private val repository: VideoRepository,
  private val preferenceStore: com.noxtan.player.preferences.preference.PreferenceStore
) : ViewModel() {

  val videoDao = repository.videoDao

  private val _isAppResuming = MutableStateFlow(false)
  val isAppResuming = _isAppResuming.asStateFlow()

  fun setAppResuming(isResuming: Boolean) {
    _isAppResuming.value = isResuming
  }

  private val _currentFolderPath = MutableStateFlow<String?>(null)

  fun updateCurrentFolder(path: String?) {
    _currentFolderPath.value = path
  }

  val recentVideo = _currentFolderPath
    .flatMapLatest { path ->
      kotlinx.coroutines.flow.flow {
        emit(null)

        val sourceFlow = if (path == null) {
          repository.getGlobalRecentVideo()
        } else {
          repository.getRecentVideoInFolder(path)
        }

        sourceFlow.collect { emit(it) }
      }
    }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

  private val _searchQuery = MutableStateFlow("")
  val searchQuery = _searchQuery.asStateFlow()

  private val _isFabVisible = MutableStateFlow(true)
  val isFabVisible = _isFabVisible.asStateFlow()

  private val _selectedPaths = MutableStateFlow<Set<String>>(emptySet())
  val selectedPaths = _selectedPaths.asStateFlow()

  private val _isSelectionMode = MutableStateFlow(false)
  val isSelectionMode = _isSelectionMode.asStateFlow()

  private val _isUserRefreshing = MutableStateFlow(false)
  val isUserRefreshing = _isUserRefreshing.asStateFlow()

  var isSearchMode = false

  val isSelectionModeActive: Boolean
    get() = _isSelectionMode.value

  @OptIn(FlowPreview::class)
  private val _allVideos = repository.allVideos
    .debounce(300)
    .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

  private val _viewConfig = MutableStateFlow(ViewConfig())
  private val gridCountPref = preferenceStore.getInt("view_config_grid_count", 1)
  private val showSizePref = preferenceStore.getBoolean("view_config_show_size", true)
  private val showDurationPref = preferenceStore.getBoolean("view_config_show_duration", true)
  private val showResolutionPref = preferenceStore.getBoolean("view_config_show_resolution", true)
  private val showExtensionPref = preferenceStore.getBoolean("view_config_show_extension", true)
  private val sortByPref = preferenceStore.getString("view_config_sort_by", "NAME")
  private val isAscendingPref = preferenceStore.getBoolean("view_config_is_ascending", true)

  val viewConfig: StateFlow<ViewConfig> = combine<Any, ViewConfig>(
    gridCountPref.changes(),
    showSizePref.changes(),
    showDurationPref.changes(),
    showResolutionPref.changes(),
    showExtensionPref.changes(),
    sortByPref.changes(),
    isAscendingPref.changes()
  ) { args ->
    val gridCount = args[0] as Int
    val showSize = args[1] as Boolean
    val showDuration = args[2] as Boolean
    val showResolution = args[3] as Boolean
    val showExtension = args[4] as Boolean
    val sortBy = args[5] as String
    val isAscending = args[6] as Boolean

    ViewConfig(
      gridCount = gridCount,
      showSize = showSize,
      showDuration = showDuration,
      showResolution = showResolution,
      showExtension = showExtension,
      sortBy = try { SortType.valueOf(sortBy) } catch (e: Exception) { SortType.NAME },
      isAscending = isAscending
    )
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.Eagerly,
    initialValue = ViewConfig(
      gridCount = gridCountPref.get(),
      showSize = showSizePref.get(),
      showDuration = showDurationPref.get(),
      showResolution = showResolutionPref.get(),
      showExtension = showExtensionPref.get(),
      sortBy = try { SortType.valueOf(sortByPref.get()) } catch (e: Exception) { SortType.NAME },
      isAscending = isAscendingPref.get()
    )
  )

  val folderList: StateFlow<List<VideoFolder>> = combine(_allVideos, _viewConfig) { videos, config ->
    val folders = videos.groupBy { File(it.path).parentFile?.path ?: "Unknown" }
      .map { (path, videoList) ->
        VideoFolder(
          name = File(path).name,
          path = path,
          videoCount = videoList.size,
          totalSize = videoList.sumOf { it.size },
          latestDate = videoList.maxOfOrNull { it.dateAdded } ?: 0L,
          hasNewVideos = videoList.any { it.markState == "NEW" }
        )
      }
    val sorted = when (config.sortBy) {
      SortType.NAME -> folders.sortedBy { it.name.lowercase() }
      SortType.DATE -> folders.sortedBy { it.latestDate }
      SortType.SIZE -> folders.sortedBy { it.totalSize }
      else -> folders.sortedBy { it.name.lowercase() }
    }

    if (config.isAscending) sorted else sorted.reversed()
  }
    .flowOn(Dispatchers.Default)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  fun getVideosForFolder(folderPath: String?): StateFlow<List<VideoEntity>> {
    return combine(_allVideos, _searchQuery, _viewConfig) { videos, query, config ->
      var filtered = videos
      if (folderPath != null) {
        filtered = filtered.filter { (File(it.path).parentFile?.path ?: "") == folderPath }
      }
      if (query.isNotBlank()) {
        filtered = filtered.filter { it.title.contains(query, ignoreCase = true) }
      }
      val sorted = when (config.sortBy) {
        SortType.NAME -> filtered.sortedBy { it.title.lowercase() }
        SortType.DATE -> filtered.sortedBy { it.dateAdded }
        SortType.SIZE -> filtered.sortedBy { it.size }
        SortType.DURATION -> filtered.sortedBy { it.duration }
      }
      if (config.isAscending) sorted else sorted.reversed()
    }
      .flowOn(Dispatchers.Default)
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
  }

  fun getGlobalRecentVideo() = repository.getGlobalRecentVideo()
  fun getRecentVideoInFolder(folderPath: String) = repository.getRecentVideoInFolder(folderPath)

  init {
    silentRefresh()
    viewModelScope.launch(Dispatchers.IO) {
      repository.getMediaStoreChanges()
        .debounce(1500)
        .collect { silentRefresh() }
    }
  }

  fun manualRefresh() {
    viewModelScope.launch {
      _isUserRefreshing.value = true
      try {
        repository.scanDeviceForVideos()
      } catch (e: Exception) {
        e.printStackTrace()
      } finally {
        kotlinx.coroutines.delay(400)
        _isUserRefreshing.value = false
      }
    }
  }

  fun silentRefresh() {
    viewModelScope.launch(Dispatchers.IO) {
      _isUserRefreshing.value = true
      try {
        repository.scanDeviceForVideos()
      } catch (e: Exception) {
        e.printStackTrace()
      } finally {
        kotlinx.coroutines.delay(400)
        _isUserRefreshing.value = false
      }
    }
  }

  fun onSearchQueryChanged(query: String) {
    _searchQuery.value = query
  }

  fun updateViewConfig(config: ViewConfig) {
    gridCountPref.set(config.gridCount)
    showSizePref.set(config.showSize)
    showDurationPref.set(config.showDuration)
    showResolutionPref.set(config.showResolution)
    showExtensionPref.set(config.showExtension)
    sortByPref.set(config.sortBy.name)
    isAscendingPref.set(config.isAscending)
  }

  fun setFabVisibility(visible: Boolean, source: String = "Unknown") {
    if (_isFabVisible.value != visible) {
      _isFabVisible.value = visible
    }
  }

  fun toggleSelection(path: String) {
    val current = _selectedPaths.value
    val newSelection = if (current.contains(path)) current - path else current + path
    _selectedPaths.value = newSelection

    if (!_isSelectionMode.value) {
      _isSelectionMode.value = true
      setFabVisibility(false, "SelectionMode_ON")
    }
  }

  fun selectAll(paths: List<String>) {
    _selectedPaths.value = paths.toSet()
    if (!_isSelectionMode.value) {
      _isSelectionMode.value = true
      setFabVisibility(false, "SelectionMode_ON")
    }
  }

  fun deselectAll() {
    _selectedPaths.value = emptySet()
  }

  fun clearSelection() {
    _selectedPaths.value = emptySet()
    _isSelectionMode.value = false
    if (!isSearchMode) {
      setFabVisibility(true, "SelectionMode_OFF")
    }
  }

  fun getVideosInSelectedFolders(): List<VideoEntity> {
    val currentSelectedFolders = _selectedPaths.value
    if (currentSelectedFolders.isEmpty()) return emptyList()

    return _allVideos.value.filter { video ->
      val parentPath = File(video.path).parentFile?.path ?: ""
      currentSelectedFolders.contains(parentPath)
    }
  }

  fun removeDeletedVideosFromDatabase(deletedPaths: List<String>) {
    viewModelScope.launch(Dispatchers.IO) {
      repository.videoDao.deleteVideosByPaths(deletedPaths)
      clearSelection()
      silentRefresh()
    }
  }

  private var thumbnailJob: kotlinx.coroutines.Job? = null

  fun generateThumbnailsForList(videos: List<VideoEntity>) {
    thumbnailJob?.cancel()
    thumbnailJob = viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
      repository.generateMissingThumbnails(videos)
    }
  }

  fun stopThumbnailGeneration() {
    thumbnailJob?.cancel()
  }

  fun markSelectedVideosAs(state: String, isFolderMode: Boolean = false) {
    viewModelScope.launch(Dispatchers.IO) {
      val videosToUpdate = if (isFolderMode) {
        getVideosInSelectedFolders()
      } else {
        _allVideos.value.filter { it.path in _selectedPaths.value }
      }

      val paths = videosToUpdate.map { it.path }
      if (paths.isNotEmpty()) {
        repository.updateMarkState(paths, state)
        silentRefresh()
      }
      clearSelection()
    }
  }
}
