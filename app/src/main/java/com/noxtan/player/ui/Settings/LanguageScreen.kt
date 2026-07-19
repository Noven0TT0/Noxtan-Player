package com.noxtan.player.ui.Settings

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import com.noxtan.player.MainActivity
import com.noxtan.player.presentation.Screen
import com.noxtan.player.presentation.components.CollapsingTopBar
import com.noxtan.player.ui.utils.LocalBackStack
import com.noxtan.player.ui.utils.topAndBottomNoise
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
object LanguageScreen : Screen {
  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  override fun Content() {
    val context = LocalContext.current
    val backstack = LocalBackStack.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val scope = rememberCoroutineScope()

    val appLocales = AppCompatDelegate.getApplicationLocales()
    val currentLocale = if (appLocales.isEmpty) "" else appLocales.toLanguageTags().substringBefore("-")

    var isLoading by remember { mutableStateOf(false) }

    val languages = listOf(
      Pair("", "System Default"),
      Pair("en", "English"),
      Pair("my", "မြန်မာ (Burmese)")
    )

    Scaffold(
      modifier = Modifier
        .nestedScroll(scrollBehavior.nestedScrollConnection)
        .topAndBottomNoise(noiseAlpha = 0.15f, fadeFraction = 0.09f),
      topBar = {
        CollapsingTopBar(
          title = "Language",
          scrollBehavior = scrollBehavior,
          onBackClick = { if (!isLoading) backstack.removeLastOrNull() }
        )
      },
      containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
      Box(modifier = Modifier.fillMaxSize().padding(top = padding.calculateTopPadding())) {
        LazyColumn(
          modifier = Modifier.fillMaxSize().padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          items(languages) { (code, name) ->
            val isSelected = currentLocale == code

            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface)
                .selectable(
                  selected = isSelected,
                  enabled = !isLoading,
                  role = Role.RadioButton,
                  onClick = {
                    if (code != currentLocale) {
                      isLoading = true
                      scope.launch {
                        delay(150)

                        val localeList = if (code.isEmpty()) {
                          LocaleListCompat.getEmptyLocaleList()
                        } else {
                          LocaleListCompat.forLanguageTags(code)
                        }
                        AppCompatDelegate.setApplicationLocales(localeList)

                        val intent = Intent(context, MainActivity::class.java).apply {
                          flags =
                            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION
                          putExtra("open_settings", true)
                        }
                        context.startActivity(intent)
                        if (context is Activity) {
                          context.finish()
                          context.overridePendingTransition(0, 0)
                        }
                      }
                    }
                  }
                )
                .padding(16.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text(
                text = name,
                style = MaterialTheme.typography.titleMedium.copy(
                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                ),
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
              )
              if (isSelected) {
                Icon(
                  imageVector = Icons.Rounded.Check,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary
                )
              }
            }
          }
        }

        if (isLoading) {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .background(MaterialTheme.colorScheme.background)
              .clickable(enabled = false) {},
            contentAlignment = Alignment.Center
          ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
          }
        }
      }
    }
  }
}
