package com.github.premnirmal.ticker.ui.navigation

import androidx.compose.ui.graphics.vector.ImageVector

object Route {
  const val Watchlist = "Watchlist"
  const val Trending = "Trending"
  const val Search = "Search"
  const val Widgets = "Widgets"
  const val Settings = "Settings"
}

data class TopLevelDestination(
  val route: String,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector,
  val iconTextId: Int
)