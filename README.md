# Stocks Widget
[![Circle CI](https://circleci.com/gh/premnirmal/StockTicker.svg?style=svg)](https://circleci.com/gh/premnirmal/StockTicker)

[![Google play link](graphics/google-play-badge.png)](https://play.google.com/store/apps/details?id=com.github.premnirmal.tickerwidget)

[<img src="https://i.imgur.com/u6kj7yf.png"
      alt="Get it on F-Droid"
      height="80">](https://f-droid.org/app/com.github.premnirmal.tickerwidget)

![](https://lh3.googleusercontent.com/R9khJ5kNzXHUjO4BxNw1cNKTx62grZ7FtLRT_F2H0BhC99iuMWDxvuGTYvyydtqE3w=h400-rw)
![](https://lh3.googleusercontent.com/5bJo7sEfgkI5KX2rhvOwugq42B5lPY3FcNWIHschi-ek5Rqo_p6_ajxxAiiJ6cXI2cU=h400-rw)
![](https://lh3.googleusercontent.com/pKI7tibPnU4HS92l-ubpphJVS_7wCJdteg6td6gnAnbx8sqiVFJZEjTiQe4hBiPU5W0=h400-rw)

## Motivation

This app is to demonstrate usage of **kotlin** on android using [Dagger2](https://github.com/google/dagger) and [RxJava2](https://github.com/ReactiveX/RxJava).

This app also shows how to write android unit tests using [mockito](https://github.com/mockito/mockito) and [robolectric](https://github.com/robolectric/robolectric) using kotlin.

## App features:

- A homescreen widget that shows your stock portolio in a resizable grid.
- This app fetches stock quotes from this API: https://www.robindahood.com. The API is open sourced [here](https://github.com/premnirmal/robindahood.com).
- Stocks can be sorted by dragging and dropping the list.
- Only performs automatic fetching of stocks during trading hours and weekdays.

### Importing and exporting
- You can import a list of tickers by selecting **import tickers** from the settings menu. All you need is a textfile with your tickers in *comma-separated* format.
- You can also export your tickers to a file by selecting **export tickers**.

## License

MIT

### Author
[Prem Nirmal](http://premnirmal.me/)
