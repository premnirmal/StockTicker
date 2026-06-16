import SwiftUI
import Shared

/// Minimal watchlist view that proves the shared `IStocksProvider` is live on iOS.
///
/// The full SwiftUI / Compose-Multiplatform UI is a later phase; for Phase 2 this renders the shared
/// portfolio `StateFlow` and lets you trigger a refresh through the shared provider so the iOS
/// implementations (preferences, persistence, networking, scheduling) can be exercised end-to-end.
struct ContentView: View {

    @StateObject private var model = WatchlistModel()

    var body: some View {
        NavigationView {
            List(model.quotes, id: \.symbol) { quote in
                HStack {
                    VStack(alignment: .leading) {
                        Text(quote.symbol).font(.headline)
                        Text(quote.name).font(.caption).foregroundColor(.secondary)
                    }
                    Spacer()
                    Text(quote.priceString())
                }
            }
            .navigationTitle("Watchlist")
            .toolbar {
                Button("Refresh") { model.refresh() }
            }
            .task { model.start() }
        }
    }
}

/// Bridges the shared `IStocksProvider` `StateFlow` to SwiftUI.
final class WatchlistModel: ObservableObject {

    @Published var quotes: [Quote] = []

    private let provider: StocksProvider = KoinHelper.shared.stocksProvider()
    private var subscription: Closeable?

    func start() {
        subscription = KoinHelper.shared.observePortfolio { [weak self] list in
            self?.quotes = list
        }
        provider.schedule()
    }

    func refresh() {
        Task { _ = try? await provider.fetch(allowScheduling: true) }
    }

    deinit {
        subscription?.close()
    }
}
