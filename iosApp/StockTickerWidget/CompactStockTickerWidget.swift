import WidgetKit
import SwiftUI
import AppIntents
import Shared

/// A text-only, tightly compacted home-screen widget — the iOS counterpart of the Android "text"
/// widget: every quote occupies a single, dense row (symbol · price · change%) so many more symbols
/// fit on screen than in the card-style ``StockTickerWidget``.
///
/// It reuses the shared ``StockTickerProvider`` / ``StockTickerConfigurationIntent`` (so it honours the
/// same per-widget watchlist selection, sort and appearance options) and only swaps in a denser view.

// MARK: - Row

/// One tightly packed quote line: `SYMBOL …… price  change%`.
///
/// Everything is constrained to a single line and allowed to scale down slightly so a quote never
/// exceeds the two-line budget, matching the compact Android widget.
private struct CompactQuoteRowView: View {
    let row: WidgetQuoteRow
    let configuration: StockTickerConfigurationIntent

    private var changeColor: Color { row.positive ? .green : .red }

    var body: some View {
        HStack(alignment: .firstTextBaseline, spacing: 4) {
            Text(row.symbol)
                .font(.system(.caption2, design: .rounded).weight(.semibold))
                .lineLimit(1)
                .minimumScaleFactor(0.7)
                .layoutPriority(1)

            Spacer(minLength: 2)

            Text(row.price)
                .font(.system(.caption2, design: .rounded).monospacedDigit())
                .lineLimit(1)
                .minimumScaleFactor(0.7)

            Text(configuration.showChangeAmount ? row.changeAmount : row.changePercent)
                .font(.system(.caption2, design: .rounded)
                    .weight(configuration.boldChange ? .bold : .regular)
                    .monospacedDigit())
                .foregroundStyle(changeColor)
                .lineLimit(1)
                .minimumScaleFactor(0.7)
        }
    }
}

// MARK: - Grid

/// Lays the compact rows out in one or more columns, capped at `maxItems` so nothing clips (iOS
/// widgets cannot scroll). A small "last fetch" header mirrors the Android compact widget.
private struct CompactGridView: View {
    let entry: StockTickerEntry
    let columns: Int
    let maxItems: Int

    private let gridColumns: [GridItem]

    init(entry: StockTickerEntry, columns: Int, maxItems: Int) {
        self.entry = entry
        self.columns = columns
        self.maxItems = maxItems
        self.gridColumns = Array(repeating: GridItem(.flexible(), spacing: 10), count: columns)
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 2) {
            if entry.configuration.showHeader && !entry.isPlaceholder {
                Text("Last fetch: \(entry.date, format: .dateTime.hour().minute())")
                    .font(.caption2)
                    .foregroundStyle(.secondary)
                    .lineLimit(1)
            }
            if entry.quotes.isEmpty {
                CompactEmptyView()
            } else {
                LazyVGrid(columns: gridColumns, alignment: .leading, spacing: 2) {
                    ForEach(entry.quotes.prefix(maxItems)) { row in
                        CompactQuoteRowView(row: row, configuration: entry.configuration)
                    }
                }
            }
            Spacer(minLength: 0)
        }
    }
}

private struct CompactEmptyView: View {
    var body: some View {
        VStack(alignment: .leading, spacing: 2) {
            Text("Stocks")
                .font(.caption.weight(.semibold))
            Text("Add symbols to your watchlist.")
                .font(.caption2)
                .foregroundStyle(.secondary)
        }
    }
}

// MARK: - Entry view

/// Routes the entry to a column count / item cap appropriate for the current widget family.
struct CompactStockTickerWidgetEntryView: View {
    @Environment(\.widgetFamily) private var family
    let entry: StockTickerEntry

    var body: some View {
        Group {
            switch family {
            case .systemSmall:
                CompactGridView(entry: entry, columns: 1, maxItems: 8)
            case .systemMedium:
                CompactGridView(entry: entry, columns: 2, maxItems: 16)
            default:
                CompactGridView(entry: entry, columns: 2, maxItems: 32)
            }
        }
        .containerBackgroundCompat()
    }
}

// MARK: - Widget

struct CompactStockTickerWidget: Widget {
    private let kind = "CompactStockTickerWidget"

    var body: some WidgetConfiguration {
        AppIntentConfiguration(
            kind: kind,
            intent: StockTickerConfigurationIntent.self,
            provider: StockTickerProvider()
        ) { entry in
            CompactStockTickerWidgetEntryView(entry: entry)
        }
        .configurationDisplayName("Stocks (Compact)")
        .description("A dense, text-only watchlist — every symbol on its own line.")
        .supportedFamilies([.systemSmall, .systemMedium, .systemLarge])
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
