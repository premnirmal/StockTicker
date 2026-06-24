package com.github.premnirmal.ticker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.github.premnirmal.shared.resources.Res
import com.github.premnirmal.shared.resources.ic_news
import com.github.premnirmal.shared.resources.ic_search
import com.github.premnirmal.shared.resources.ic_trending_up
import com.github.premnirmal.shared.resources.ic_widget
import com.github.premnirmal.ticker.UserPreferences
import org.jetbrains.compose.resources.painterResource
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

private object OnboardingKoin : KoinComponent {
    val userPreferences: UserPreferences by inject()
}

private data class OnboardingPage(
    val icon: Painter,
    val title: String,
    val message: String,
)

/**
 * Drives the iOS first-run onboarding tutorial. It mirrors Android's tutorial, which is gated on the
 * shared [UserPreferences.tutorialShown] preference (Android shows it as a bottom sheet from
 * `HomeViewModel.checkShowTutorial()`). On iOS the same preference decides whether to present the
 * onboarding flow on first launch, and the Settings "Tutorial" row re-opens it on demand via [show].
 */
class OnboardingController(private val prefs: UserPreferences) {

    var visible by mutableStateOf(false)
        private set

    /** Presents the onboarding flow only the first time the app is launched. */
    fun showIfFirstRun() {
        if (!prefs.tutorialShown()) {
            visible = true
        }
    }

    /** Presents the onboarding flow on demand (e.g. from the Settings "Tutorial" row). */
    fun show() {
        visible = true
    }

    /** Dismisses the flow and records that the tutorial has been shown. */
    fun dismiss() {
        prefs.setTutorialShown(true)
        visible = false
    }
}

@Composable
fun rememberOnboardingController(): OnboardingController =
    remember { OnboardingController(OnboardingKoin.userPreferences) }

/**
 * iOS onboarding tutorial. A multi-step modal [Dialog] that introduces the watchlist, search,
 * trending and home-screen widget features, ending in a "Get started" action. Tailored to iOS, where
 * widgets are added from the Home Screen (WidgetKit) rather than configured in-app like Android's
 * Glance widgets. Shown by an [OnboardingController]; dismissing it persists [UserPreferences.tutorialShown].
 */
@Composable
fun OnboardingTutorial(controller: OnboardingController) {
    if (!controller.visible) return

    val pages = listOf(
        OnboardingPage(
            icon = painterResource(Res.drawable.ic_trending_up),
            title = "Welcome to Stocks Widget",
            message = "Keep an eye on your favorite stocks with a clean, real-time watchlist.",
        ),
        OnboardingPage(
            icon = painterResource(Res.drawable.ic_search),
            title = "Add your stocks",
            message = "Use the Search tab to find symbols and tap the + button to add them to your watchlist.",
        ),
        OnboardingPage(
            icon = painterResource(Res.drawable.ic_news),
            title = "Dive into the details",
            message = "Tap any stock to see price charts, news, positions, price alerts and notes.",
        ),
        OnboardingPage(
            icon = painterResource(Res.drawable.ic_widget),
            title = "Add a Home Screen widget",
            message = "Touch and hold your Home Screen, tap the + button, then search for Stocks Widget " +
                "to add a widget.",
        ),
    )

    var pageIndex by remember { mutableStateOf(0) }
    val page = pages[pageIndex]
    val isLastPage = pageIndex == pages.lastIndex

    Dialog(onDismissRequest = { controller.dismiss() }) {
        Card {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Icon(
                        painter = page.icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier
                            .padding(16.dp)
                            .size(40.dp),
                    )
                }
                Text(
                    modifier = Modifier.padding(top = 20.dp),
                    text = page.title,
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                )
                Text(
                    modifier = Modifier.padding(top = 8.dp),
                    text = page.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(20.dp))
                PageIndicator(pageCount = pages.size, selectedIndex = pageIndex)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = { controller.dismiss() }) {
                        Text(if (isLastPage) "" else "Skip")
                    }
                    TextButton(
                        onClick = {
                            if (isLastPage) controller.dismiss() else pageIndex++
                        }
                    ) {
                        Text(if (isLastPage) "Done" else "Next")
                    }
                }
            }
        }
    }
}

@Composable
private fun PageIndicator(pageCount: Int, selectedIndex: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        for (i in 0 until pageCount) {
            val color = if (i == selectedIndex) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
            }
            Surface(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape),
                color = color,
            ) {}
        }
    }
}
