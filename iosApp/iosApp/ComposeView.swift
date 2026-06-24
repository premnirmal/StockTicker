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

private final class ComposeContainerViewController: UIViewController, UIGestureRecognizerDelegate {
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
        // Let Compose receive the tap as well so text fields can still gain focus.
        dismissKeyboardTap.cancelsTouchesInView = false
        dismissKeyboardTap.delaysTouchesBegan = false
        dismissKeyboardTap.delaysTouchesEnded = false
        dismissKeyboardTap.delegate = self
        view.addGestureRecognizer(dismissKeyboardTap)
    }

    @objc private func dismissKeyboard() {
        // The same tap that reaches this recognizer is also delivered to Compose, which may focus a
        // text field and make its input view the first responder. Defer the dismissal to the next
        // run-loop turn so Compose has a chance to update the first responder, then only dismiss when
        // the tap did NOT focus a (new) text field. Without this, tapping a text field would request
        // focus and immediately resign it, so the keyboard would never appear.
        let responderBeforeTap = UIResponder.currentFirstResponder
        DispatchQueue.main.async { [weak self] in
            guard let self = self else { return }
            let responderAfterTap = UIResponder.currentFirstResponder
            if responderAfterTap != nil, responderAfterTap !== responderBeforeTap {
                // The tap focused a text field; keep the keyboard.
                return
            }
            self.view.endEditing(true)
        }
    }

    // Allow the recognizer to coexist with Compose's own gesture handling.
    func gestureRecognizer(
        _ gestureRecognizer: UIGestureRecognizer,
        shouldRecognizeSimultaneouslyWith otherGestureRecognizer: UIGestureRecognizer
    ) -> Bool {
        true
    }
}

private extension UIResponder {
    private static weak var resolvedFirstResponder: UIResponder?

    /// Returns the current first responder by sending an action to the responder chain.
    static var currentFirstResponder: UIResponder? {
        resolvedFirstResponder = nil
        UIApplication.shared.sendAction(
            #selector(UIResponder.resolveFirstResponder(_:)),
            to: nil,
            from: nil,
            for: nil
        )
        return resolvedFirstResponder
    }

    @objc func resolveFirstResponder(_ sender: Any?) {
        UIResponder.resolvedFirstResponder = self
    }
}
