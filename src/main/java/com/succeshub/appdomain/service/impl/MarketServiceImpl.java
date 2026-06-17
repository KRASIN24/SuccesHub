package com.succeshub.appdomain.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.succeshub.appdomain.dto.MarketDto;
import com.succeshub.appdomain.service.MarketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MarketServiceImpl implements MarketService {

    private static final String COINGECKO_URL =
            "https://api.coingecko.com/api/v3/simple/price?ids=bitcoin,ethereum,nasdaq100&vs_currencies=usd&include_24hr_change=true";

    private final RestClient restClient;

    @Override
    @Cacheable("market-prices")
    public MarketDto getPrices() {
        try {
            JsonNode root = restClient.get()
                    .uri(COINGECKO_URL)
                    .retrieve()
                    .body(JsonNode.class);

            List<MarketDto.PriceEntry> entries = new ArrayList<>();

            if (root != null) {
                entries.add(buildEntry(root, "bitcoin", "BTC/USD"));
                entries.add(buildEntry(root, "ethereum", "ETH/USD"));
            }

            return new MarketDto(entries);
        } catch (Exception ex) {
            log.warn("Failed to fetch market data from CoinGecko: {}", ex.getMessage());
            return new MarketDto(List.of(
                    new MarketDto.PriceEntry("BTC/USD", "N/A", "N/A"),
                    new MarketDto.PriceEntry("ETH/USD", "N/A", "N/A")
            ));
        }
    }

    private MarketDto.PriceEntry buildEntry(JsonNode root, String coinId, String pair) {
        JsonNode node = root.path(coinId);
        if (node.isMissingNode()) {
            return new MarketDto.PriceEntry(pair, "N/A", "N/A");
        }
        double price = node.path("usd").asDouble(0);
        double change = node.path("usd_24h_change").asDouble(0);
        String priceStr = String.format("%,.2f", price);
        String changeStr = String.format("%+.2f%%", change);
        return new MarketDto.PriceEntry(pair, priceStr, changeStr);
    }
}
