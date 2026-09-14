# Dashboard auxiliary widgets

The Dashboard's right rail adds lightweight context without exposing third-party APIs to the browser:

- **Sovereign Index** — BTC/USD and ETH/USD spot prices plus 24-hour change.
- **Lunar Phase** — a server-calculated phase, illumination, and approximate rise time.
- **Motivational Quote** — a backend-owned quote payload.

Angular loads these widgets alongside profile, daily ritual, task, forecast, and achievement data. The services are intentionally simple, but their cache and failure behaviors matter in development and production.

## Request flow

[`HomeComponent`](../frontend/src/app/features/home/home.component.ts) starts eight requests with `forkJoin`:

1. profile
2. daily ritual status
3. forecast
4. open tasks
5. achievements
6. market prices
7. lunar data
8. quote

The three auxiliary requests pass through Angular's shared [`ApiService`](../frontend/src/app/core/services/api.service.ts) to Spring controllers and services:

```text
HomeComponent
  ├─ MarketService    → GET /api/market/prices    → CoinGecko
  ├─ AstronomyService → GET /api/astronomy/lunar → local calculation
  └─ QuoteService     → GET /api/quote/daily      → static V1 value
```

All `/api/**` routes require an authenticated BFF session, even though these controllers do not use the user identity. The Angular auth interceptor enables credentials so the browser can attach its `JSESSIONID` cookie. During local development, [`proxy.conf.json`](../frontend/proxy.conf.json) forwards `/api` to the backend on port 8082.

## API contracts

### `GET /api/market/prices`

Returns display-ready strings rather than numeric values:

```json
{
  "prices": [
    { "pair": "BTC/USD", "price": "67,123.45", "change": "+1.23%" },
    { "pair": "ETH/USD", "price": "3,456.78", "change": "-0.42%" }
  ]
}
```

[`MarketServiceImpl`](../src/main/java/com/succeshub/appdomain/service/impl/MarketServiceImpl.java) calls a fixed CoinGecko URL. It requests Bitcoin, Ethereum, and Nasdaq 100 data, but only maps Bitcoin and Ethereum into the response.

### `GET /api/astronomy/lunar`

```json
{
  "phaseName": "Waxing Gibbous",
  "illumination": 90,
  "riseTime": "09:34",
  "setTime": "21:34"
}
```

[`LunarServiceImpl`](../src/main/java/com/succeshub/appdomain/service/impl/LunarServiceImpl.java) calculates the result from the current UTC date. Rise and set values are phase-based approximations; they do not use user location, coordinates, or timezone. The current Dashboard renders `riseTime` but not `setTime`.

### `GET /api/quote/daily`

```json
{
  "text": "The void is not an empty space, but a canvas awaiting the curation of your will.",
  "author": "The Archivist"
}
```

The V1 [`QuoteServiceImpl`](../src/main/java/com/succeshub/appdomain/service/impl/QuoteServiceImpl.java) returns this same value on every request. It does not select by date or read from a database.

## Caching and failure behavior

[`WebClientConfig`](../src/main/java/com/succeshub/config/WebClientConfig.java) uses an in-memory `ConcurrentMapCacheManager`.

| Cache | Population | Expiry |
|-------|------------|--------|
| `market-prices` | First market request in a backend process | None |
| `lunar-phase` | First lunar request in a backend process | None |

Consequences:

- Cached values remain until the backend restarts. Each backend instance has its own cache.
- Lunar data can become stale after the UTC date changes.
- A CoinGecko exception is converted to HTTP 200 with `N/A` entries. That fallback response is also cached until restart.
- A successful HTTP response with a missing coin produces `N/A` for that coin. A null response body produces an empty `prices` list.
- The market client has no application-level timeout, retry, API-key, or configurable base URL.
- Price and percentage values are formatted using the backend JVM's default locale, then sent as strings.

The Dashboard treats its eight startup requests as one operation. If any request errors, `forkJoin` discards the combined result and shows the page-level retry state. CoinGecko failures normally do not trigger that state because the backend converts them to a successful fallback response.

Scheduling or completing a task and celebrating a day each reload all eight Dashboard requests. Market and lunar reloads read the backend cache after the first method invocation.

## Troubleshooting

### Market widget stays on `N/A`

1. Search backend logs for `Failed to fetch market data from CoinGecko`.
2. Confirm the backend host can reach `https://api.coingecko.com`.
3. After the upstream or network issue is resolved, restart the backend to clear the cached fallback.

There is no endpoint for targeted cache eviction.

### Lunar widget shows yesterday's phase

Restart the backend after the UTC date boundary. The current cache has no midnight eviction.

### The whole Dashboard shows an error

Use the browser network panel to identify which of the eight requests failed. A missing or expired BFF session returns 401 from all `/api/**` calls. Retrying the page does not clear backend caches.

## Change checklist

When changing a widget:

1. Keep the Java DTO and Angular model field names aligned.
2. Decide whether the response should remain display-ready strings or expose numeric values.
3. Define cache expiry and failure-caching behavior explicitly.
4. Preserve authenticated same-origin `/api` access, or update the security contract deliberately.
5. Test the widget independently and test Dashboard behavior when it fails; there are currently no dedicated backend or Angular tests for these three services.
