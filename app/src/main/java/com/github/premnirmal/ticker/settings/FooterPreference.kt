package com.github.premnirmal.ticker.settings

import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.TextView
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.github.premnirmal.ticker.CustomTabs
import com.github.premnirmal.tickerwidget.BuildConfig
import com.github.premnirmal.tickerwidget.R
import android.view.ViewConfiguration
import android.widget.Toast
import android.view.MotionEvent
import com.github.premnirmal.ticker.debug.DbViewerActivity

class FooterPreference(
    context: Context,
    attrs: AttributeSet
) : Preference(context, attrs) {

  private var clickCount = 0
  override fun onBindViewHolder(holder: PreferenceViewHolder) {
    super.onBindViewHolder(holder)
    val view = holder.itemView
    val versionView = view.findViewById<TextView>(R.id.version)
    val vName = "v${BuildConfig.VERSION_NAME}"
    versionView.text = vName
    val githubLink = view.findViewById<View>(R.id.github_link)
    githubLink.setOnClickListener {
      CustomTabs.openTab(
          context, view.resources.getString(R.string.checkout_open_source)
      )
    }
    versionView.setOnTouchListener(TapListener())
  }

  private inner class TapListener : View.OnTouchListener {

    private var numberOfTaps = 0
    private var lastTapTimeMs: Long = 0
    private var touchDownMs: Long = 0
    private var countToast: Toast? = null

    override fun onTouch(v: View, event: MotionEvent): Boolean {
      when (event.action) {
        MotionEvent.ACTION_DOWN -> {
          touchDownMs = System.currentTimeMillis()
          if (numberOfTaps > 0 && System.currentTimeMillis() - lastTapTimeMs < ViewConfiguration.getDoubleTapTimeout()) {
            numberOfTaps++
            countToast?.cancel()
            if (numberOfTaps < 5) {
              val toast =Toast.makeText(context,
                  context.getString(R.string.db_tap_count, 5 - numberOfTaps),
                    Toast.LENGTH_SHORT)
              toast.setGravity(Gravity.CENTER, 0, 0)
              toast.show()
              countToast = toast
            }
          } else {
            countToast?.cancel()
            numberOfTaps = 1
          }
        }
        MotionEvent.ACTION_UP -> {
          if (System.currentTimeMillis() - touchDownMs > ViewConfiguration.getTapTimeout()) {
            countToast?.cancel()
            numberOfTaps = 0
            lastTapTimeMs = 0
            return true
          }
          lastTapTimeMs = System.currentTimeMillis()
          if (numberOfTaps == 5) {
            val toast = Toast.makeText(context, R.string.discovered_db, Toast.LENGTH_LONG)
            toast.setGravity(Gravity.CENTER, 0, 0)
            toast.show()
            context.startActivity(Intent(context, DbViewerActivity::class.java))
            numberOfTaps = 0
            lastTapTimeMs = 0
          }
        }
      }
      return true
    }
  }
}