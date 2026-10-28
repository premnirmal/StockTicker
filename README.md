# Stocks Widget
[![Android](https://github.com/premnirmal/StockTicker/workflows/Android/badge.svg)](https://github.com/premnirmal/StockTicker/actions) [![Unit tests](https://github.com/premnirmal/StockTicker/workflows/Run%20unit%20tests/badge.svg)](https://github.com/premnirmal/StockTicker/actions) [![Detekt](https://github.com/premnirmal/StockTicker/workflows/Detekt/badge.svg)](https://github.com/premnirmal/StockTicker/actions) [![iOS](https://github.com/premnirmal/StockTicker/workflows/iOS/badge.svg)](https://github.com/premnirmal/StockTicker/actions)

<a href="https://play.google.com/store/apps/details?id=com.github.premnirmal.tickerwidget" target="_blank">
<img src="https://upload.wikimedia.org/wikipedia/commons/thumb/7/78/Google_Play_Store_badge_EN.svg/500px-Google_Play_Store_badge_EN.svg.png" alt="Get it on Google Play" height="40"/>
</a> 
<a href="https://apps.apple.com/us/app/todaystocks/id993467855" target="_blank">
<img src="https://developer.apple.com/assets/elements/badges/download-on-the-app-store.svg" alt="Available on the App Store" height="40"/>
</a> 
<a href="https://f-droid.org/en/packages/com.github.premnirmal.tickerwidget/" target="_blank">
<img src="https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQEslT1h9Dawi4R80LvhKKeLvCKNsi5nzvKmg&s" alt="Get it on F-Droid" height="40"/>
</a>

## App features

- A home screen widget that shows your stock portfolio in a resizable grid
- Stocks can be sorted by dragging and dropping the list
- Only performs automatic fetching of stocks during trading hours
- Displays price change and summary alerts

## Kotlin Multiplatform

This project has been migrated to Kotlin Multiplatform (KMP) with shared Compose Multiplatform
UI, so Android and iOS run from one shared codebase. Platform-agnostic code and the shared
in-app screens live in the `:shared` module (`androidTarget` + iOS targets), with thin
platform shells in `:app` (Android) and `iosApp` (iOS). See [MULTIPLATFORM.md](MULTIPLATFORM.md)
for the module layout and the history of the migration. The iOS app is a thin SwiftUI shell that hosts the same shared
Compose Multiplatform UI as Android, plus a native WidgetKit home-screen widget. It shares the
full business logic (networking, persistence, preferences, background refresh) and the in-app
screens (watchlist, trending, search, quote detail, settings) with Android via the `:shared`
module. See [`iosApp/README.md`](iosApp/README.md) for build instructions.

## Screenshots
![](https://play-lh.googleusercontent.com/R9khJ5kNzXHUjO4BxNw1cNKTx62grZ7FtLRT_F2H0BhC99iuMWDxvuGTYvyydtqE3w=h400-rw)
<img src="https://i.postimg.cc/dvjnChBZ/Screenshot-20260627-113556.png" height="400"/>
<img src="https://github.com/user-attachments/assets/75e65d1d-70ac-4464-a2f3-d9b3674d226c" height="400"/>
<img src="https://github.com/user-attachments/assets/cd0b7bae-26f3-44af-a3fc-339c76507471" height="400"/>

## License

GPL

### Author
[Prem Nirmal](http://premnirmal.me/)
