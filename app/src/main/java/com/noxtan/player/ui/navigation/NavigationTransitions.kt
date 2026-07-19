package com.noxtan.player.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith

object NavigationTransitions {

  private const val ANIM_DURATION = 300

  fun AnimatedContentTransitionScope<*>.popTransitionSpec(): ContentTransform {
    return slideInHorizontally(animationSpec = tween(ANIM_DURATION)) { -it / 4 } togetherWith
      slideOutHorizontally(animationSpec = tween(ANIM_DURATION)) { it }
  }

  fun AnimatedContentTransitionScope<*>.transitionSpec(): ContentTransform {
    return slideInHorizontally(animationSpec = tween(ANIM_DURATION)) { it } togetherWith
      slideOutHorizontally(animationSpec = tween(ANIM_DURATION)) { -it / 4 }
  }

  fun AnimatedContentTransitionScope<*>.predictivePopTransitionSpec(): ContentTransform {
    return slideInHorizontally(animationSpec = tween(ANIM_DURATION)) { -it / 4 } togetherWith
      slideOutHorizontally(animationSpec = tween(ANIM_DURATION)) { it }
  }
}
