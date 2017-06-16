package com.github.premnirmal.ticker.settings

import android.animation.Animator
import android.animation.Animator.AnimatorListener
import android.animation.AnimatorListenerAdapter
import android.annotation.TargetApi
import android.app.Activity
import android.app.AlertDialog
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Build
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.support.annotation.ArrayRes
import android.support.annotation.RequiresApi
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewAnimationUtils
import android.view.ViewTreeObserver
import com.github.premnirmal.ticker.base.BaseActivity
import com.github.premnirmal.ticker.components.InAppMessage
import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.ticker.ui.SettingsTextView
import com.github.premnirmal.ticker.widget.WidgetData
import com.github.premnirmal.ticker.widget.WidgetDataProvider
import com.github.premnirmal.tickerwidget.R
import kotlinx.android.synthetic.main.activity_widget_settings.activity_root
import kotlinx.android.synthetic.main.activity_widget_settings.setting_add_stock
import kotlinx.android.synthetic.main.activity_widget_settings.setting_autosort
import kotlinx.android.synthetic.main.activity_widget_settings.setting_autosort_checkbox
import kotlinx.android.synthetic.main.activity_widget_settings.setting_background
import kotlinx.android.synthetic.main.activity_widget_settings.setting_bold
import kotlinx.android.synthetic.main.activity_widget_settings.setting_bold_checkbox
import kotlinx.android.synthetic.main.activity_widget_settings.setting_layout_type
import kotlinx.android.synthetic.main.activity_widget_settings.setting_text_color
import kotlinx.android.synthetic.main.activity_widget_settings.setting_widget_name
import kotlinx.android.synthetic.main.activity_widget_settings.toolbar
import javax.inject.Inject

class WidgetSettingsActivity : BaseActivity(), OnClickListener {

  companion object {
    val ARG_WIDGET_ID = AppWidgetManager.EXTRA_APPWIDGET_ID

    fun launchIntent(context: Context, widgetId: Int): Intent {
      val intent = Intent(context, WidgetSettingsActivity::class.java)
      intent.putExtra(ARG_WIDGET_ID, widgetId)
      return intent
    }
  }

  @Inject lateinit internal var widgetDataProvider: WidgetDataProvider

  internal var widgetId = 0
  internal var shouldPerformTransition = false

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    shouldPerformTransition = intent.hasExtra(EXTRA_CENTER_X) && intent.hasExtra(EXTRA_CENTER_Y)
    if (shouldPerformTransition) {
      overridePendingTransition(0, 0)
    }
    Injector.appComponent.inject(this)
    widgetId = intent.getIntExtra(ARG_WIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
    if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
      val result: Intent = Intent()
      result.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
      setResult(Activity.RESULT_OK, result)
    } else {
      setResult(Activity.RESULT_CANCELED)
    }
    val widgetData = widgetDataProvider.dataForWidgetId(widgetId)

    setContentView(R.layout.activity_widget_settings)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      toolbar.setPadding(toolbar.paddingLeft, getStatusBarHeight(),
          toolbar.paddingRight, toolbar.paddingBottom)
    }
    toolbar.setNavigationOnClickListener {
      onBackPressed()
    }

    setWidgetNameSetting(widgetData)
    setLayoutTypeSetting(widgetData)
    setBgSetting(widgetData)
    setTextColorSetting(widgetData)
    setBoldSetting(widgetData)
    setAutoSortSetting(widgetData)

    arrayOf(setting_add_stock, setting_widget_name, setting_layout_type,
        setting_background, setting_text_color, setting_bold, setting_autosort)
        .forEach { it.setOnClickListener(this@WidgetSettingsActivity) }

    if (savedInstanceState == null && shouldPerformTransition &&
        VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
      activity_root.visibility = View.INVISIBLE

      if (activity_root.viewTreeObserver.isAlive) {
        activity_root.viewTreeObserver
            .addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
              @TargetApi(VERSION_CODES.LOLLIPOP)
              @RequiresApi(VERSION_CODES.LOLLIPOP)
              override fun onGlobalLayout() {
                if (VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN) {
                  activity_root.viewTreeObserver.removeOnGlobalLayoutListener(this)
                } else {
                  activity_root.viewTreeObserver.removeGlobalOnLayoutListener(this)
                }
                doCircularReveal()
              }
            })
      }
    }
  }

  override fun onClick(v: View?) {
    val widgetData = widgetDataProvider.dataForWidgetId(widgetId)
    when (v?.id) {
      R.id.setting_add_stock -> {
        openTickerSelector(v, widgetId)
      }
      R.id.setting_widget_name -> {
        v.setOnClickListener(null)
        (v as SettingsTextView).setIsEditable(true, { s ->
          widgetData.setWidgetName(s)
          setWidgetNameSetting(widgetData)
          v.setIsEditable(false)
          v.setOnClickListener(this)
          InAppMessage.showMessage(this, R.string.widget_name_updated)
        })
      }
      R.id.setting_layout_type -> {
        showDialogPreference(R.array.layout_types,
            DialogInterface.OnClickListener { dialog, which ->
              widgetData.setLayoutPref(which)
              setLayoutTypeSetting(widgetData)
              dialog.dismiss()
              broadcastUpdateWidget()
              if (which == 2) {
                showDialog(getString(R.string.change_instructions))
              }
            })
      }
      R.id.setting_background -> {
        showDialogPreference(R.array.backgrounds,
            DialogInterface.OnClickListener { dialog, which ->
              widgetData.setBgPref(which)
              setBgSetting(widgetData)
              dialog.dismiss()
              broadcastUpdateWidget()
              InAppMessage.showMessage(this, R.string.bg_updated_message)
            })
      }
      R.id.setting_text_color -> {
        showDialogPreference(R.array.text_colors,
            DialogInterface.OnClickListener { dialog, which ->
              widgetData.setTextColorPref(which)
              setTextColorSetting(widgetData)
              dialog.dismiss()
              broadcastUpdateWidget()
              InAppMessage.showMessage(this, R.string.text_coor_updated_message)
            })
      }
      R.id.setting_bold -> {
        val isChecked = !setting_bold_checkbox.isChecked
        widgetData.setBoldEnabled(isChecked)
        setBoldSetting(widgetData)
        broadcastUpdateWidget()
      }
      R.id.setting_autosort -> {
        val isChecked = !setting_autosort_checkbox.isChecked
        widgetData.setAutoSort(isChecked)
        setAutoSortSetting(widgetData)
        broadcastUpdateWidget()
      }
    }
  }

  internal fun broadcastUpdateWidget() {
    widgetDataProvider.broadcastUpdateWidget(widgetId)
  }

  internal fun showDialogPreference(@ArrayRes itemRes: Int,
      listener: DialogInterface.OnClickListener) {
    AlertDialog.Builder(this)
        .setItems(itemRes, listener)
        .create().show()
  }

  internal fun setWidgetNameSetting(widgetData: WidgetData) {
    setting_widget_name.setSubtitle(widgetData.widgetName())
  }

  internal fun setLayoutTypeSetting(widgetData: WidgetData) {
    val layoutTypeDesc = resources.getStringArray(R.array.layout_types)[widgetData.layoutPref()]
    setting_layout_type.setSubtitle(layoutTypeDesc)
  }

  internal fun setBgSetting(widgetData: WidgetData) {
    val bgDesc = resources.getStringArray(R.array.backgrounds)[widgetData.bgPref()]
    setting_background.setSubtitle(bgDesc)
  }

  internal fun setTextColorSetting(widgetData: WidgetData) {
    val textColorDesc = resources.getStringArray(R.array.text_colors)[widgetData.textColorPref()]
    setting_text_color.setSubtitle(textColorDesc)
  }

  internal fun setBoldSetting(widgetData: WidgetData) {
    setting_bold_checkbox.isChecked = widgetData.isBoldEnabled()
  }

  internal fun setAutoSortSetting(widgetData: WidgetData) {
    setting_autosort_checkbox.isChecked = widgetData.autoSortEnabled()
  }

  override fun finishAfterTransition() {
    if (shouldPerformTransition && VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
      doCircularReveal(true, object : AnimatorListenerAdapter() {
        override fun onAnimationEnd(animation: Animator?) {
          activity_root.visibility = View.INVISIBLE
          finish()
          overridePendingTransition(0, 0)
        }
      })
    } else {
      super.finishAfterTransition()
    }
  }

  @TargetApi(VERSION_CODES.LOLLIPOP)
  @RequiresApi(VERSION_CODES.LOLLIPOP)
  internal fun doCircularReveal(reverse: Boolean = false, listener: AnimatorListener? = null) {
    val cx = intent.getIntExtra(EXTRA_CENTER_X, resources.displayMetrics.widthPixels)
    val cy = intent.getIntExtra(EXTRA_CENTER_Y, resources.displayMetrics.heightPixels)
    val finalRadius = Math.max(activity_root.width, activity_root.height)
    val circularRevealAnim = ViewAnimationUtils
        .createCircularReveal(activity_root, cx, cy,
            if (reverse) finalRadius.toFloat() else 0.toFloat(),
            if (reverse) 0.toFloat() else finalRadius.toFloat())
    circularRevealAnim.duration = resources.getInteger(
        android.R.integer.config_mediumAnimTime).toLong()
    activity_root.visibility = View.VISIBLE
    listener?.let { circularRevealAnim.addListener(it) }
    circularRevealAnim.start()
  }
}