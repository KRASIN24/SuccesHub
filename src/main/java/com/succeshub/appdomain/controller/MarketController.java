package com.succeshub.appdomain.controller;

import com.succeshub.appdomain.dto.MarketDto;
import com.succeshub.appdomain.service.MarketService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/market")
@RequiredArgsConstructor
public class MarketController {

    private final MarketService service;

    @GetMapping("/prices")
    public ResponseEntity<MarketDto> getPrices() {
        return ResponseEntity.ok(service.getPrices());
    }
}
