import SwiftUI
import Shared

/// Root SwiftUI view for the iOS app.
///
/// Phase 5: this now hosts the shared Compose Multiplatform UI (`ComposeView`) edge-to-edge,
/// replacing the Phase 2 SwiftUI watchlist placeholder. All UI is rendered by the shared Kotlin
/// Compose screens; the SwiftUI shell only provides the hosting window and the platform plumbing
/// (Koin start-up, background scheduling, analytics, WidgetKit reloads).
struct ContentView: View {
    var body: some View {
//        ComposeView()
//            .ignoresSafeArea() // let the shared Compose UI draw edge-to-edge
        Button("Crash") {
          fatalError("Crash was triggered")
        }
    }
}
