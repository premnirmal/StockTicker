import SwiftUI
import Shared

/// Hosts the shared Compose Multiplatform UI (the Kotlin `MainViewController`) inside SwiftUI.
///
/// Phase 5 deliverable: the Phase 4 Compose Multiplatform screens now run on iOS. This
/// `UIViewControllerRepresentable` bridges the `UIViewController` produced by
/// `MainViewControllerKt.MainViewController()` into the SwiftUI view hierarchy. Koin is already
/// started in `StockTickerApp.init()`, so the shared screens resolve their dependencies from the
/// graph.
struct ComposeView: UIViewControllerRepresentable {

    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
