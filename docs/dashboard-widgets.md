# Dashboard auxiliary widgets

The Dashboard's right rail adds lightweight context without exposing third-party APIs to the browser:

- **Sovereign Index** — BTC/USD and ETH/USD spot prices plus 24-hour change.
- **Lunar Phase** — the API calculates phase, illumination, and approximate rise/set times; the Dashboard displays phase, illumination, and rise time.
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
  → Angular widget service
  → ApiService (/api)
  → auth interceptor
  → development or production reverse proxy
  → Spring controller
  → service implementation
  → CoinGecko, local calculation, or static result
```

All `/api/**` routes require an authenticated BFF session, even though these controllers do not use the user identity. The Angular auth interceptor enables credentials on every HTTP request; the browser attaches the matching HttpOnly `JSESSIONID` cookie.

During local development, [`proxy.conf.json`](../frontend/proxy.conf.json) forwards `/api`, `/oauth2`, `/login`, and `/logout` to the backend on port 8082. Production also uses the relative `/api` base URL, so its hosting layer must provide equivalent routing. Direct cross-origin browser access is not a production fallback: the current Spring CORS configuration permits only `http://localhost:4200`.

## API contracts

### `GET /api/market/prices`

Returns display-ready strings rather than numeric values. This example assumes an `en-US` JVM locale:

```json
{
  "prices": [
    { "pair": "BTC/USD", "price": "67,123.45", "change": "+1.23%" },
    { "pair": "ETH/USD", "price": "3,456.78", "change": "-0.42%" }
  ]
}
```

[`MarketServiceImpl`](../src/main/java/com/succeshub/appdomain/service/impl/MarketServiceImpl.java) calls a fixed CoinGecko URL. The request includes the CoinGecko ID `nasdaq100`, but response mapping ignores it and returns only Bitcoin and Ethereum.

### `GET /api/astronomy/lunar`

Example for 2026-01-01 UTC:

```json
{
  "phaseName": "Waxing Gibbous",
  "illumination": 96,
  "riseTime": "10:27",
  "setTime": "22:27"
}
```

[`LunarServiceImpl`](../src/main/java/com/succeshub/appdomain/service/impl/LunarServiceImpl.java) calculates the result from the current UTC date. It uses a fixed 29.53-day cycle, divides that cycle into eight equal phase bins, maps cycle age linearly onto a 24-hour clock, and always sets moonset 12 hours after moonrise. These values are illustrative rather than ephemeris-derived; they do not use user location, coordinates, or timezone. The current Dashboard renders `riseTime` but not `setTime`.

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
| `market-prices` | First completed cache miss; cold population is unsynchronized | None |
| `lunar-phase` | First completed cache miss; cold population is unsynchronized | None |
| Quote | Every request; not cached | Not applicable |

Consequences:

- Cached values remain until the backend restarts. Each backend instance has its own cache.
- Concurrent cold-cache requests can all execute; a later completion can replace an earlier cached value.
- Lunar data can become stale after the UTC date changes.
- A CoinGecko exception is converted to HTTP 200 with `N/A` entries. That fallback response is also cached until restart.
- A successful response with an absent coin property produces `N/A` without a warning log. An explicit null coin value produces zero-formatted fields, and a null response body produces an empty `prices` list. Missing or non-numeric price fields also become zero-formatted strings using the JVM locale.
- The market client has no application-level timeout, retry, API-key, or configurable base URL.
- Price and percentage values are formatted using the backend JVM's default locale, then sent as strings.

The Dashboard treats its eight startup requests as one operation. Initial load shows one page-wide skeleton until all requests succeed or one errors; widgets have no independent loading, error, or retry state. If any request errors, `forkJoin` discards the combined result and shows the page-level retry state. A silent refresh keeps the existing page visible while requests are pending, but an error still replaces it with the page-wide error. CoinGecko failures normally do not trigger that state because the backend converts exceptions to a successful fallback response.

Scheduling a task, completing a task, or celebrating a day reloads all eight Dashboard requests. Market and lunar reloads normally read the backend cache after initial population.

## Troubleshooting

### Market widget stays on `N/A`

1. Search backend logs for `Failed to fetch market data from CoinGecko`.
2. Confirm the backend host can reach `https://api.coingecko.com`.
3. After the upstream or network issue is resolved, restart the backend to clear the cached fallback.

The warning identifies exception fallbacks, but its absence does not rule out malformed upstream data that produced `N/A`, an empty list, or zero values. There is no endpoint for targeted cache eviction.

### Dashboard remains on the loading skeleton

Inspect `/api/market/prices` first. The backend configures no explicit connect/read timeout or retry, so a stalled upstream call can keep `forkJoin` pending until a lower network layer times out.

### Lunar widget shows stale data

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
