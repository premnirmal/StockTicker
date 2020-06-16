package com.github.premnirmal.ticker

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.PorterDuff.Mode.SRC_IN
import android.net.Uri
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import androidx.browser.customtabs.CustomTabsIntent
import androidx.browser.customtabs.CustomTabsService
import com.github.premnirmal.tickerwidget.R

object CustomTabs {

  private var packageNameToUse: String? = null
  private const val chromePackage = "com.android.chrome"
  private const val firefoxPreviewPackage = "org.mozilla.fenix"
  private const val firefoxPackage = "org.mozilla.firefox"
  private const val edgePackage = "com.microsoft.emmx"

  fun openTab(
    context: Context,
    url: String
  ) {
    val closeButton = context.resources.getDrawable(R.drawable.ic_close)
    if (VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
      closeButton.setTint(context.resources.getColor(R.color.icon_tint))
      closeButton.setTintMode(SRC_IN)
    }
    val customTabsIntent = CustomTabsIntent.Builder()
        .addDefaultShareMenuItem()
        .setToolbarColor(context.resources.getColor(R.color.color_primary))
        .setShowTitle(true)
        .setCloseButtonIcon(closeButton.toBitmap())
        .setExitAnimations(context, android.R.anim.fade_in, android.R.anim.fade_out)
        .build()
    val packageName = getPackageNameToUse(context, url)
    if (packageName == null) {
      val browserActivityIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
      context.startActivity(browserActivityIntent)
    } else {
      customTabsIntent.intent.setPackage(packageName)
      customTabsIntent.launchUrl(context, Uri.parse(url))
    }
  }

  private fun getPackageNameToUse(
    context: Context,
    url: String
  ): String? {
    packageNameToUse?.let {
      return it
    }
    val pm = context.packageManager
    val activityIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    val defaultViewHandlerInfo = pm.resolveActivity(activityIntent, 0)
    var defaultViewHandlerPackageName: String? = null
    defaultViewHandlerInfo?.let {
      defaultViewHandlerPackageName = it.activityInfo.packageName
    }
    val resolvedActivityList = pm.queryIntentActivities(activityIntent, 0)
    val packagesSupportingCustomTabs = ArrayList<String>()
    for (info in resolvedActivityList) {
      val serviceIntent = Intent()
      serviceIntent.action = CustomTabsService.ACTION_CUSTOM_TABS_CONNECTION
      serviceIntent.setPackage(info.activityInfo.packageName)

      pm.resolveService(serviceIntent, 0)
          ?.let {
            packagesSupportingCustomTabs.add(info.activityInfo.packageName)
          }
    }

    when {
      packagesSupportingCustomTabs.isEmpty() -> packageNameToUse = null
      packagesSupportingCustomTabs.size == 1 -> packageNameToUse = packagesSupportingCustomTabs[0]
      !defaultViewHandlerPackageName.isNullOrEmpty()
          && !hasSpecializedHandlerIntents(context, activityIntent)
          && packagesSupportingCustomTabs.contains(defaultViewHandlerPackageName!!) ->
        packageNameToUse = defaultViewHandlerPackageName
      packagesSupportingCustomTabs.contains(chromePackage) -> packageNameToUse = chromePackage
      packagesSupportingCustomTabs.contains(edgePackage) -> packageNameToUse = edgePackage
      packagesSupportingCustomTabs.contains(firefoxPreviewPackage) -> packageNameToUse = firefoxPreviewPackage
      packagesSupportingCustomTabs.contains(firefoxPackage) -> packageNameToUse = firefoxPackage
    }
    return packageNameToUse
  }

  private fun hasSpecializedHandlerIntents(
    context: Context,
    intent: Intent
  ): Boolean {
    try {
      val pm = context.packageManager
      val handlers = pm.queryIntentActivities(
          intent,
          PackageManager.GET_RESOLVED_FILTER
      )
      if (handlers == null || handlers.size == 0) {
        return false
      }
      for (resolveInfo in handlers) {
        val filter = resolveInfo.filter ?: continue
        if (filter.countDataAuthorities() == 0 || filter.countDataPaths() == 0) continue
        if (resolveInfo.activityInfo == null) continue
        return true
      }
    } catch (e: RuntimeException) {
    }
    return false
  }
}