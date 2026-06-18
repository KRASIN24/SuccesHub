package com.succeshub.appdomain.controller;

import com.succeshub.appdomain.dto.MarketDto;
import com.succeshub.appdomain.service.MarketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST API for the Sovereign Index widget: proxied cryptocurrency prices.
 */
@Tag(name = "Market", description = "Cryptocurrency market prices (CoinGecko proxy)")
@RestController
@RequestMapping("/api/market")
@RequiredArgsConstructor
public class MarketController {

    private final MarketService service;

    /**
     * Returns BTC and ETH spot prices with 24-hour change, cached server-side.
     */
    @Operation(summary = "Get market prices", description = "Proxies CoinGecko and returns formatted BTC/USD and ETH/USD entries.")
    @ApiResponse(responseCode = "200", description = "Prices returned (may contain N/A on upstream failure)")
    @GetMapping("/prices")
    public ResponseEntity<MarketDto> getPrices() {
        return ResponseEntity.ok(service.getPrices());
    }
}
