# Stock Tracker Wear

A standalone Wear OS stock tracking app for Samsung Galaxy Watch 7 (and Wear OS 3+). View a watchlist of symbols with delayed quotes, add/remove symbols, and refresh on demand. Quotes are cached and refreshed in the background.

## Requirements

- Android Studio Ladybug (2024.2.1) or newer
- JDK 17
- Wear OS 3+ device or emulator (e.g. Wear OS 6 / API 35 for Galaxy Watch 7)

## Setup

### 1. Clone and open

Open the project in Android Studio.

### 2. Gradle wrapper (if needed)

If the project has no `gradlew`, open it in Android Studio and use **File → Sync Project with Gradle Files** (or the prompt to create the wrapper).

### 3. API key (Alpha Vantage)

The app uses [Alpha Vantage](https://www.alphavantage.co/support/#api-key) for stock quotes. Free tier allows about 25 requests per day.

1. Sign up at [Alpha Vantage](https://www.alphavantage.co/support/#api-key) and get an API key.
2. In the project root, create or edit `local.properties` (this file is git-ignored). You can copy `local.properties.example` and rename it.
3. Add:

```properties
STOCK_API_KEY=your_alpha_vantage_api_key_here
```

Without a key, the app runs but will show "Configure API key" and cannot fetch quotes.

### 4. Build and run

1. Create a Wear OS virtual device: **Device Manager** → **Create Device** → **Wear** → pick a Wear OS image (e.g. API 35).
2. Select the `wear` run configuration and run on the emulator or a physical Wear OS device.

## Features

- **Watchlist**: List of symbols with last price and change (with 15-minute cache).
- **Refresh**: Manual refresh from the watchlist screen.
- **Add symbol**: Choose from a short list of popular symbols (e.g. AAPL, MSFT).
- **Remove symbol**: Not exposed in UI in v1; can be added via long-press or secondary action.
- **Detail**: Tap a symbol to see price, change, open/high/low, and "as of" time.
- **Background refresh**: WorkManager refreshes quotes about every 45 minutes when the device is connected (respects API limits).

## Architecture

- **UI**: Jetpack Compose for Wear OS (Material 3), `ScalingLazyColumn`, swipe-to-dismiss navigation.
- **Data**: Room for watchlist and cached quotes; Retrofit for Alpha Vantage GLOBAL_QUOTE API.
- **DI**: Hilt.
- **Background**: WorkManager + HiltWorkerFactory for periodic refresh.

## Project structure

```
wear/
  src/main/java/com/stocktracker/wear/
    data/          # API, Room, Repository
    domain/        # StockQuote, WatchlistItem
    di/            # Hilt modules
    ui/            # Screens, ViewModels, theme, navigation
    worker/        # RefreshQuotesWorker, scheduler
```

## Notes

- Data is **delayed** (free tier). Show "as of" time so users know it’s not real-time.
- Keep the watchlist small (e.g. under 10 symbols) to stay within free API limits.
- For production, move the API key to a secure mechanism (e.g. backend proxy) and do not ship it in the app.
