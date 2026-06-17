package com.succeshub.appdomain.service;

import com.succeshub.appdomain.dto.MarketDto;

/**
 * Application service that proxies cryptocurrency market prices for the dashboard ticker.
 */
public interface MarketService {

    /**
     * Returns cached BTC and ETH spot prices with 24-hour change from CoinGecko.
     *
     * @return price entries; falls back to placeholder values when the upstream API fails
     */
    MarketDto getPrices();
}
