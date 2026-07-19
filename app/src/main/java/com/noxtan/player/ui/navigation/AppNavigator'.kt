package com.noxtan.player.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import com.noxtan.player.features.local.viewmodel.LocalVideoViewModel
import com.noxtan.player.presentation.Screen
import com.noxtan.player.presentation.components.RecentPlayFab
import com.noxtan.player.ui.Settings.SettingsScreen
import com.noxtan.player.ui.home.HomeScreen
import com.noxtan.player.ui.home.VideoListScreen
import com.noxtan.player.ui.navigation.NavigationTransitions.popTransitionSpec
import com.noxtan.player.ui.navigation.NavigationTransitions.predictivePopTransitionSpec
import com.noxtan.player.ui.navigation.NavigationTransitions.transitionSpec
import com.noxtan.player.ui.utils.AppBackStack
import com.noxtan.player.ui.utils.LocalBackStack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AppNavigator() {
  val context = LocalContext.current

  val activity = remember(context) {
    var ctx = context
    while (ctx is android.content.ContextWrapper) {
      if (ctx is androidx.activity.ComponentActivity) break
      ctx = ctx.baseContext
    }
    ctx as androidx.activity.ComponentActivity
  }

  val localVideoViewModel = koinViewModel<LocalVideoViewModel>(
    viewModelStoreOwner = activity
  )

  val backstack = remember {
    val list = mutableStateListOf<Screen>(HomeScreen)
    if (activity.intent.getBooleanExtra("open_settings", false)) {
      list.add(SettingsScreen)
    }
    list
  }
  val currentScreen = backstack.lastOrNull()

  val scope = rememberCoroutineScope()
  var lastPopTime by remember { androidx.compose.runtime.mutableLongStateOf(0L) }
  var pendingPopJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
  val transitionDuration = 350L

  val safeBackStack = remember(backstack, scope) {
    object : AppBackStack {
      override val size: Int get() = backstack.size
      override fun add(screen: Screen) {
        pendingPopJob?.cancel()
        backstack.add(screen)
      }
      override fun removeLastOrNull(): Screen? {
        if (backstack.size <= 1) return null

        val now = System.currentTimeMillis()
        if (backstack.size == 2 && backstack.lastOrNull() == SettingsScreen) {
          val elapsed = now - lastPopTime
          if (elapsed < transitionDuration) {
            val remainingDelay = transitionDuration - elapsed
            pendingPopJob?.cancel()
            pendingPopJob = scope.launch(Dispatchers.Main) {
              delay(remainingDelay)
              if (backstack.size > 1 && backstack.lastOrNull() == SettingsScreen) {
                backstack.removeLastOrNull()
              }
            }
            return null
          }
        }
        if (backstack.size == 3 && backstack.lastOrNull() != SettingsScreen && backstack.lastOrNull() != HomeScreen) {
          pendingPopJob?.cancel()
          lastPopTime = now
        }
        return backstack.removeLastOrNull()
      }
    }
  }

  CompositionLocalProvider(LocalBackStack provides safeBackStack) {
    Box(modifier = Modifier.fillMaxSize()) {
      NavDisplay(
        backStack = backstack,
        onBack = { safeBackStack.removeLastOrNull() },
        entryProvider = { route ->
          NavEntry(route) {
            route.Content()
          }
        },
        popTransitionSpec = { popTransitionSpec() },
        transitionSpec = { transitionSpec() },
        predictivePopTransitionSpec = { predictivePopTransitionSpec() },
      )

      val showFabOnCurrentScreen = currentScreen is HomeScreen || currentScreen is VideoListScreen
      val recentVideo by localVideoViewModel.recentVideo.collectAsState()
      val isFabVisible by localVideoViewModel.isFabVisible.collectAsState()

      RecentPlayFab(
        visible = isFabVisible && showFabOnCurrentScreen,
        hasRecentVideo = recentVideo != null,
        onClick = {
          recentVideo?.let {
            HomeScreen.playFile(it.path, activity)
          }
        },
        modifier = Modifier
          .align(Alignment.BottomEnd)
          .navigationBarsPadding()
          .padding(end = 16.dp, bottom = 44.dp)
      )
    }
  }
}
