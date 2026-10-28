package com.github.premnirmal.ticker.ui

import com.github.premnirmal.ticker.components.AppLogger
import platform.Foundation.NSURL
import platform.SafariServices.SFSafariViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowScene

/**
 * Opens [url] in-app using an [SFSafariViewController] presented over the current view controller,
 * instead of leaving the app for the system browser (or whichever app handles the link). This
 * mirrors the in-app browsing experience users expect when tapping news cards or links from the
 * settings screen.
 *
 * `SFSafariViewController` only supports `http`/`https` URLs, so non-web URLs (and the rare case
 * where no presenter can be found) fall back to [UIApplication.openURL].
 */
internal fun openUrlInApp(url: String?) {
    val target = url ?: return
    val nsUrl = NSURL.URLWithString(target) ?: return
    val scheme = nsUrl.scheme?.lowercase()
    val presenter = topViewController()
    if (presenter != null && (scheme == "http" || scheme == "https")) {
        val safari = SFSafariViewController(uRL = nsUrl)
        presenter.presentViewController(safari, animated = true, completion = null)
    } else {
        UIApplication.sharedApplication.openURL(nsUrl, options = emptyMap<Any?, Any?>(), completionHandler = null)
    }
}

/**
 * Returns the top-most presented [UIViewController] of the active window, which is the controller a
 * modal such as [SFSafariViewController] should be presented over.
 */
private fun topViewController(): UIViewController? {
    val scene = UIApplication.sharedApplication.connectedScenes
        .firstOrNull { it is UIWindowScene } as? UIWindowScene
    val window = scene?.windows
        ?.firstOrNull { (it as? UIWindow)?.keyWindow == true } as? UIWindow
        ?: scene?.windows?.firstOrNull() as? UIWindow
    var controller = window?.rootViewController
    if (controller == null) {
        AppLogger.w("No root view controller; cannot present SFSafariViewController")
    }
    while (controller?.presentedViewController != null) {
        controller = controller.presentedViewController
    }
    return controller
}
