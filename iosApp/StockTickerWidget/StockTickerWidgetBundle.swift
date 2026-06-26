import WidgetKit
import SwiftUI

/// The WidgetKit extension entry point, bundling the home-screen widget(s).
@main
struct StockTickerWidgetBundle: WidgetBundle {
    var body: some Widget {
        CompactStockTickerWidget()
        StockTickerWidget()
    }
}
