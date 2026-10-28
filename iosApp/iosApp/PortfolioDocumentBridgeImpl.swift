import Foundation
import SwiftUI
import UniformTypeIdentifiers
import Shared

/// iOS implementation of the shared `PortfolioDocumentBridge`.
///
/// The shared `IosPortfolioExchange` (Kotlin) owns all serialization and the provider mutations and
/// only calls out here for the native file presentation — the iOS analogue of Android's
/// `ActivityResultContracts.CreateDocument` / `OpenDocument` launchers. This presents the system
/// `UIDocumentPickerViewController` (export / import) and `UIActivityViewController` (share) from the
/// key window's top-most view controller.
final class PortfolioDocumentBridgeImpl: NSObject, PortfolioDocumentBridge {

    /// Retains the active picker delegate for the lifetime of the (modal) presentation.
    private var activeDelegate: PortfolioDocumentPickerDelegate?

    // MARK: - Export (write a new document)

    func exportDocument(suggestedName: String, content: String, uti: String) {
        guard let url = writeTemporaryFile(named: suggestedName, content: content) else { return }
        DispatchQueue.main.async {
            let picker = UIDocumentPickerViewController(forExporting: [url])
            self.present(picker)
        }
    }

    // MARK: - Share (system share sheet)

    func shareDocument(suggestedName: String, content: String) {
        guard let url = writeTemporaryFile(named: suggestedName, content: content) else { return }
        DispatchQueue.main.async {
            let activity = UIActivityViewController(activityItems: [url], applicationActivities: nil)
            guard let presenter = Self.topViewController() else { return }
            // iPad requires a popover anchor.
            activity.popoverPresentationController?.sourceView = presenter.view
            activity.popoverPresentationController?.sourceRect = CGRect(
                x: presenter.view.bounds.midX,
                y: presenter.view.bounds.midY,
                width: 0,
                height: 0
            )
            activity.popoverPresentationController?.permittedArrowDirections = []
            presenter.present(activity, animated: true)
        }
    }

    // MARK: - Import (read an existing document)

    func importDocument(onResult: @escaping (String?, String?) -> Void) {
        DispatchQueue.main.async {
            let types: [UTType] = [.json, .plainText, .text]
            let picker = UIDocumentPickerViewController(forOpeningContentTypes: types)
            picker.allowsMultipleSelection = false
            let delegate = PortfolioDocumentPickerDelegate { [weak self] url in
                defer { self?.activeDelegate = nil }
                guard let url else {
                    onResult(nil, nil)
                    return
                }
                let needsAccess = url.startAccessingSecurityScopedResource()
                defer { if needsAccess { url.stopAccessingSecurityScopedResource() } }
                let content = try? String(contentsOf: url, encoding: .utf8)
                onResult(content, url.lastPathComponent)
            }
            self.activeDelegate = delegate
            picker.delegate = delegate
            self.present(picker)
        }
    }

    // MARK: - Helpers

    private func writeTemporaryFile(named name: String, content: String) -> URL? {
        let url = FileManager.default.temporaryDirectory.appendingPathComponent(name)
        do {
            try content.data(using: .utf8)?.write(to: url, options: .atomic)
            return url
        } catch {
            NSLog("Failed to write export file %@: %@", name, error.localizedDescription)
            return nil
        }
    }

    private func present(_ controller: UIViewController) {
        guard let presenter = Self.topViewController() else { return }
        presenter.present(controller, animated: true)
    }

    /// Finds the top-most presented view controller from the foreground key window.
    static func topViewController() -> UIViewController? {
        let scenes = UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
        let keyWindow = scenes
            .flatMap { $0.windows }
            .first { $0.isKeyWindow } ?? scenes.first?.windows.first
        var top = keyWindow?.rootViewController
        while let presented = top?.presentedViewController {
            top = presented
        }
        return top
    }
}

/// Bridges `UIDocumentPickerViewController` selection/cancellation back to a single completion.
private final class PortfolioDocumentPickerDelegate: NSObject, UIDocumentPickerDelegate {

    private let completion: (URL?) -> Void

    init(completion: @escaping (URL?) -> Void) {
        self.completion = completion
    }

    func documentPicker(_ controller: UIDocumentPickerViewController, didPickDocumentsAt urls: [URL]) {
        completion(urls.first)
    }

    func documentPickerWasCancelled(_ controller: UIDocumentPickerViewController) {
        completion(nil)
    }
}
