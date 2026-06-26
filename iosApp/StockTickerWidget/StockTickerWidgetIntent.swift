import AppIntents
import WidgetKit
import Shared

/// A selectable watchlist symbol, surfaced to the WidgetKit configuration UI.
///
/// The options are derived from the shared App Group snapshot (`WidgetSnapshotStore`) the app writes
/// after every refresh, so the configuration sheet always offers exactly the symbols currently on the
/// user's watchlist — without the widget extension needing the app's Koin graph / Room database.
struct WatchlistSymbolEntity: AppEntity {
    let id: String
    let name: String

    static var typeDisplayRepresentation: TypeDisplayRepresentation = "Symbol"
    static var defaultQuery = WatchlistSymbolQuery()

    var displayRepresentation: DisplayRepresentation {
        DisplayRepresentation(title: "\(id)", subtitle: "\(name)")
    }
}

/// Supplies the watchlist symbols the user can pick for a widget, reading them from the shared
/// snapshot store. Implemented as an `EntityStringQuery` so the configuration sheet supports both the
/// full list and search.
struct WatchlistSymbolQuery: EntityStringQuery {

    func entities(for identifiers: [String]) async throws -> [WatchlistSymbolEntity] {
        let wanted = Set(identifiers)
        return allSymbols().filter { wanted.contains($0.id) }
    }

    func entities(matching string: String) async throws -> [WatchlistSymbolEntity] {
        let needle = string.lowercased()
        return allSymbols().filter {
            $0.id.lowercased().contains(needle) || $0.name.lowercased().contains(needle)
        }
    }

    func suggestedEntities() async throws -> [WatchlistSymbolEntity] {
        allSymbols()
    }

    private func allSymbols() -> [WatchlistSymbolEntity] {
        let snapshot = WidgetSnapshotStore.companion.create().read()
        return (snapshot?.quotes ?? []).map {
            WatchlistSymbolEntity(id: $0.symbol, name: $0.name)
        }
    }
}

/// Per-widget configuration for the StocksWidget home-screen widget.
///
/// This is the iOS counterpart of Android's per-widget Glance options: each placed widget instance
/// keeps its own watchlist selection and appearance. Editing it (touch & hold the widget → *Edit
/// Widget*) presents these parameters; WidgetKit then rebuilds that instance's timeline with the new
/// configuration. Appearance is applied purely on the render side, while the watchlist selection
/// filters the shared snapshot, so no extra data needs to cross the App Group boundary.
struct StockTickerConfigurationIntent: WidgetConfigurationIntent {
    static var title: LocalizedStringResource = "Stocks Widget"
    static var description = IntentDescription("Choose which watchlist symbols to show and how this widget looks.")

    /// The symbols this widget should show. Empty (the default) means the whole watchlist.
    @Parameter(title: "Symbols", description: "Symbols this widget shows. Leave empty to show the whole watchlist.")
    var symbols: [WatchlistSymbolEntity]?

    /// Sort the rows by the largest movers first instead of the watchlist order.
    @Parameter(title: "Sort by change", default: false)
    var sortByChange: Bool

    /// Show each symbol's absolute change amount under the price.
    @Parameter(title: "Show change amount", default: false)
    var showChangeAmount: Bool

    /// Render the change percentage in a bold weight.
    @Parameter(title: "Bold change", default: true)
    var boldChange: Bool

    /// Show the "last fetch" timestamp header (used by the compact widget).
    @Parameter(title: "Show header", default: true)
    var showHeader: Bool

    /// The set of symbols (upper-cased) selected for this widget, or `nil` when showing everything.
    var selectedSymbols: Set<String>? {
        guard let symbols, !symbols.isEmpty else { return nil }
        return Set(symbols.map { $0.id })
    }
}
