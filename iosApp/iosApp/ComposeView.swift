import SwiftUI
import Shared
import UIKit

/// Hosts the shared Compose Multiplatform UI (the Kotlin `MainViewController`) inside SwiftUI.
///
/// Phase 5 deliverable: the Phase 4 Compose Multiplatform screens now run on iOS. This
/// `UIViewControllerRepresentable` bridges the `UIViewController` produced by
/// `MainViewControllerKt.MainViewController()` into the SwiftUI view hierarchy. Koin is already
/// started in `StockTickerApp.init()`, so the shared screens resolve their dependencies from the
/// graph.
struct ComposeView: UIViewControllerRepresentable {

    func makeUIViewController(context: Context) -> UIViewController {
        ComposeContainerViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

private final class ComposeContainerViewController: UIViewController {
    private let contentViewController = MainViewControllerKt.MainViewController()

    override func viewDidLoad() {
        super.viewDidLoad()

        addChild(contentViewController)
        view.addSubview(contentViewController.view)
        contentViewController.view.translatesAutoresizingMaskIntoConstraints = false
        NSLayoutConstraint.activate([
            contentViewController.view.topAnchor.constraint(equalTo: view.topAnchor),
            contentViewController.view.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            contentViewController.view.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            contentViewController.view.bottomAnchor.constraint(equalTo: view.bottomAnchor),
        ])
        contentViewController.didMove(toParent: self)

        let dismissKeyboardTap = UITapGestureRecognizer(target: self, action: #selector(dismissKeyboard))
        dismissKeyboardTap.cancelsTouchesInView = false
        view.addGestureRecognizer(dismissKeyboardTap)
    }

    @objc private func dismissKeyboard() {
        view.endEditing(true)
    }
}
