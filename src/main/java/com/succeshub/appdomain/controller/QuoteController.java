package com.succeshub.appdomain.controller;

import com.succeshub.appdomain.dto.QuoteDto;
import com.succeshub.appdomain.service.QuoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/quote")
@RequiredArgsConstructor
public class QuoteController {

    private final QuoteService service;

    @GetMapping("/daily")
    public ResponseEntity<QuoteDto> getDailyQuote() {
        return ResponseEntity.ok(service.getDailyQuote());
    }
}
