import WidgetKit

/// Reloads all WidgetKit timelines after a quotes refresh. This is the iOS counterpart of Android's
/// `WidgetDataProvider`/app-widget broadcast: the shared `IosStocksProvider` invokes the
/// `onQuotesUpdated` hook (wired in `StockTickerApp`) which calls this.
enum WidgetCenterReloader {
    static func reloadAll() {
        if #available(iOS 14.0, *) {
            WidgetCenter.shared.reloadAllTimelines()
        }
    }
}
