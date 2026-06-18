package com.succeshub.appdomain.dto;

import java.util.List;

public record MarketDto(List<PriceEntry> prices) {
    public record PriceEntry(String pair, String price, String change) {}
}
