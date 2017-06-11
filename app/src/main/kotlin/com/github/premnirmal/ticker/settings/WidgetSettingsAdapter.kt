package com.github.premnirmal.ticker.settings

import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import com.github.premnirmal.ticker.settings.WidgetSettingsAdapter.SettingsVH

class WidgetSettingsAdapter(
    val callback: SettingsClickCallback) : RecyclerView.Adapter<SettingsVH>() {

  interface SettingsClickCallback {
    fun onSettingsClick(settingsItem: Int)
  }

  override fun onBindViewHolder(holder: SettingsVH, position: Int) {

  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SettingsVH {

  }

  override fun getItemCount(): Int {
    return 0
  }

  class SettingsVH(itemView: View?) : RecyclerView.ViewHolder(itemView) {

  }

}