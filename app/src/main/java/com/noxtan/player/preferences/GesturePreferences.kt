package com.noxtan.player.preferences

import com.noxtan.player.preferences.preference.PreferenceStore
import com.noxtan.player.preferences.preference.getEnum
import com.noxtan.player.ui.player.EdgeOrientation
import com.noxtan.player.ui.player.SingleActionGesture

class GesturePreferences(preferenceStore: PreferenceStore) {
  val edgeSwipePosition = preferenceStore.getFloat("edge_swipe_position", 0.5f)
  val edgeSwipeEnabled = preferenceStore.getBoolean("edge_swipe_enabled", false)
  val edgeSwipePortraitEnabled = preferenceStore.getBoolean("edge_swipe_portrait_enabled", false)
  val edgeSwipeLandscapeEnabled = preferenceStore.getBoolean("edge_swipe_landscape_enabled", false)
  val edgeSwipeTarget = preferenceStore.getEnum("edge_swipe_target", com.noxtan.player.ui.player.EdgeTarget.Both)
  val edgeSwipeLandscapeLeftPos = preferenceStore.getFloat("edge_swipe_landscape_left_pos", 0.5f)
  val edgeSwipeLandscapeRightPos = preferenceStore.getFloat("edge_swipe_landscape_right_pos", 0.5f)
  val edgeSwipePortraitLeftPos = preferenceStore.getFloat("edge_swipe_portrait_left_pos", 0.5f)
  val edgeSwipePortraitRightPos = preferenceStore.getFloat("edge_swipe_portrait_right_pos", 0.5f)
  val edgeSwipeOrientation = preferenceStore.getEnum("edge_swipe_orientation", EdgeOrientation.Landscape)
  val edgeSwipeRangeStart = preferenceStore.getFloat("edge_swipe_range_start", 0.2f)
  val edgeSwipeRangeEnd = preferenceStore.getFloat("edge_swipe_range_end", 0.8f)

  val doubleTapToSeekDuration = preferenceStore.getInt("double_tap_to_seek_duration", 10)
  val leftSingleActionGesture = preferenceStore.getEnum("left_double_tap_gesture", SingleActionGesture.Seek)
  val centerSingleActionGesture = preferenceStore.getEnum("center_drag_gesture", SingleActionGesture.PlayPause)
  val rightSingleActionGesture = preferenceStore.getEnum("right_drag_gesture", SingleActionGesture.Seek)

  val mediaPreviousGesture = preferenceStore.getEnum("meda_previous_gesture", SingleActionGesture.Seek)
  val mediaPlayGesture = preferenceStore.getEnum("media_play_gesture", SingleActionGesture.PlayPause)
  val mediaNextGesture = preferenceStore.getEnum("media_next_gesture", SingleActionGesture.Seek)
}
