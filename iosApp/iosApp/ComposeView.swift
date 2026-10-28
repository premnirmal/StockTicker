import SwiftUI
import Shared

/// Hosts the shared Compose Multiplatform UI (the Kotlin `MainViewController`) inside SwiftUI.
///
/// Phase 5 deliverable: the Phase 4 Compose Multiplatform screens now run on iOS. This
/// `UIViewControllerRepresentable` bridges the `UIViewController` produced by
/// `MainViewControllerKt.MainViewController()` into the SwiftUI view hierarchy. Koin is already
/// started in `StockTickerApp.init()`, so the shared screens resolve their dependencies from the
/// graph.
///
/// Keyboard dismissal on a tap outside a text field is handled inside Compose (see
/// `SearchScreen`'s `clearFocusOnContentTap`). A UIKit-level tap gesture recognizer must NOT be
/// added here: Compose renders the whole UI (including text fields) into a single Metal surface, so
/// a global recognizer cannot tell a field-focusing tap apart from a background tap and would
/// dismiss the keyboard before it can appear.
struct ComposeView: UIViewControllerRepresentable {

    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
