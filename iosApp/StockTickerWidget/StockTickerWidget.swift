import WidgetKit
import SwiftUI
import AppIntents
import Shared

/// The data a single widget timeline entry renders. Built from the shared `WidgetSnapshot` written by
/// the app to the App Group store (`WidgetSnapshotStore`), filtered/sorted per the widget's own
/// `StockTickerConfigurationIntent`.
struct StockTickerEntry: TimelineEntry {
    let date: Date
    let quotes: [WidgetQuoteRow]
    let isPlaceholder: Bool
    /// The per-widget configuration controlling appearance (header, change amount, bold, …).
    let configuration: StockTickerConfigurationIntent
}

/// A flattened, Swift-value copy of the shared `WidgetQuoteSnapshot` (so SwiftUI views don't hold
/// Kotlin objects).
struct WidgetQuoteRow: Identifiable {
    let id = UUID()
    let symbol: String
    let name: String
    let price: String
    let changePercent: String
    let changeAmount: String
    let changeInPercent: Double
    let positive: Bool
}

/// Reads the shared App Group snapshot and supplies the WidgetKit timeline, applying each widget
/// instance's own `StockTickerConfigurationIntent` (watchlist selection + appearance).
///
/// The widget extension runs in its own process, so it cannot reach the app's Koin graph / Room
/// database. It instead reads the compact JSON snapshot the app persists after every refresh through
/// the shared `WidgetSnapshotStore` (the iOS counterpart of Android's `WidgetDataProvider`).
struct StockTickerProvider: AppIntentTimelineProvider {

    func placeholder(in context: Context) -> StockTickerEntry {
        StockTickerEntry(date: Date(), quotes: Self.sampleRows, isPlaceholder: true,
                         configuration: StockTickerConfigurationIntent())
    }

    func snapshot(for configuration: StockTickerConfigurationIntent, in context: Context) async -> StockTickerEntry {
        loadEntry(for: configuration)
    }

    func timeline(for configuration: StockTickerConfigurationIntent, in context: Context) async -> Timeline<StockTickerEntry> {
        let entry = loadEntry(for: configuration)
        // The app reloads timelines on every refresh; also poll periodically as a fallback.
        let next = Calendar.current.date(byAdding: .minute, value: 30, to: Date()) ?? Date()
        return Timeline(entries: [entry], policy: .after(next))
    }

    private func loadEntry(for configuration: StockTickerConfigurationIntent) -> StockTickerEntry {
        let snapshot = WidgetSnapshotStore.companion.create().read()
        var rows = (snapshot?.quotes ?? []).map { quote in
            WidgetQuoteRow(
                symbol: quote.symbol,
                name: quote.name,
                price: quote.price,
                changePercent: quote.changePercent,
                changeAmount: quote.changeAmount,
                changeInPercent: Double(quote.changeInPercent),
                positive: quote.positive
            )
        }
        // Per-widget watchlist selection: keep only the chosen symbols (or all when none selected).
        if let selected = configuration.selectedSymbols {
            rows = rows.filter { selected.contains($0.symbol) }
        }
        // Per-widget sort: optionally show the largest movers first.
        if configuration.sortByChange {
            rows.sort { abs($0.changeInPercent) > abs($1.changeInPercent) }
        }
        let date = snapshot.map { Date(timeIntervalSince1970: Double($0.lastUpdatedMillis) / 1000.0) } ?? Date()
        return StockTickerEntry(date: date, quotes: rows, isPlaceholder: false, configuration: configuration)
    }

    private static let sampleRows: [WidgetQuoteRow] = [
        WidgetQuoteRow(symbol: "AAPL", name: "Apple Inc.", price: "$192.32",
                       changePercent: "+1.24%", changeAmount: "+2.35", changeInPercent: 1.24, positive: true),
        WidgetQuoteRow(symbol: "MSFT", name: "Microsoft", price: "$421.10",
                       changePercent: "-0.42%", changeAmount: "-1.78", changeInPercent: -0.42, positive: false),
        WidgetQuoteRow(symbol: "GOOG", name: "Alphabet", price: "$175.98",
                       changePercent: "+0.88%", changeAmount: "+1.54", changeInPercent: 0.88, positive: true),
    ]
}

// MARK: - Views

/// One watchlist row: symbol + price on the leading edge, change% (coloured) on the trailing edge.
private struct QuoteRowView: View {
    let row: WidgetQuoteRow
    let configuration: StockTickerConfigurationIntent

    var body: some View {
        HStack(alignment: .firstTextBaseline) {
            VStack(alignment: .leading, spacing: 1) {
                Text(row.symbol)
                    .font(.system(.subheadline, design: .rounded).weight(.semibold))
                    .lineLimit(1)
                Text(row.price)
                    .font(.caption2)
                    .foregroundStyle(.secondary)
                    .lineLimit(1)
            }
            Spacer(minLength: 4)
            VStack(alignment: .trailing, spacing: 1) {
                Text(row.changePercent)
                    .font(.system(.caption, design: .rounded)
                        .weight(configuration.boldChange ? .bold : .medium))
                    .foregroundStyle(row.positive ? Color.green : Color.red)
                    .lineLimit(1)
                Text(row.changeAmount)
                    .font(.caption2)
                    .foregroundStyle(row.positive ? Color.green : Color.red)
                    .lineLimit(1)
            }
        }
    }
}

/// Two-column grid layout matching the Android widget. Shows a limited number of quotes to avoid
/// clipping in each widget size (iOS widgets do not support scrolling).
private struct StockTickerGridView: View {
    let entry: StockTickerEntry
    let columns: Int
    let maxItems: Int

    private let gridColumns: [GridItem]

    init(entry: StockTickerEntry, columns: Int = 2, maxItems: Int = 16) {
        self.entry = entry
        self.columns = columns
        self.maxItems = maxItems
        self.gridColumns = Array(repeating: GridItem(.flexible(), spacing: 8), count: columns)
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            if entry.quotes.isEmpty {
                EmptyWatchlistView()
            } else {
                LazyVGrid(columns: gridColumns, alignment: .leading, spacing: 6) {
                    ForEach(entry.quotes.prefix(maxItems)) { row in
                        QuoteRowView(row: row, configuration: entry.configuration)
                    }
                }
            }
        }
    }
}

private struct EmptyWatchlistView: View {
    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text("Stock Ticker")
                .font(.headline)
            Text("Open the app and add symbols to your watchlist.")
                .font(.caption)
                .foregroundStyle(.secondary)
        }
    }
}

/// Routes the entry to the right layout for the current widget family.
struct StockTickerWidgetEntryView: View {
    @Environment(\.widgetFamily) private var family
    let entry: StockTickerEntry

    var body: some View {
        Group {
            switch family {
            case .systemSmall:
                StockTickerGridView(entry: entry, columns: 1, maxItems: 3)
            case .systemMedium:
                StockTickerGridView(entry: entry, columns: 2, maxItems: 8)
            default:
                StockTickerGridView(entry: entry, columns: 2, maxItems: 16)
            }
        }
        .containerBackgroundCompat()
    }
}

private extension View {
    /// Applies the WidgetKit container background required on iOS 17+, no-op on earlier versions.
    @ViewBuilder
    func containerBackgroundCompat() -> some View {
        if #available(iOS 17.0, *) {
            self.containerBackground(.fill.tertiary, for: .widget)
        } else {
            self.padding()
        }
    }
}

// MARK: - Widget

struct StockTickerWidget: Widget {
    private let kind = "StockTickerWidget"

    var body: some WidgetConfiguration {
        AppIntentConfiguration(
            kind: kind,
            intent: StockTickerConfigurationIntent.self,
            provider: StockTickerProvider()
        ) { entry in
            StockTickerWidgetEntryView(entry: entry)
        }
        .configurationDisplayName("Stock Ticker")
        .description("Track your watchlist symbols at a glance.")
        .supportedFamilies([.systemSmall, .systemMedium, .systemLarge])
    }
}
