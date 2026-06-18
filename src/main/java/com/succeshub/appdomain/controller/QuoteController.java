package com.succeshub.appdomain.controller;

import com.succeshub.appdomain.dto.QuoteDto;
import com.succeshub.appdomain.service.QuoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST API for the dashboard daily motivational quote.
 */
@Tag(name = "Quote", description = "Daily inspirational quote")
@RestController
@RequestMapping("/api/quote")
@RequiredArgsConstructor
public class QuoteController {

    private final QuoteService service;

    /**
     * Returns the quote displayed in the dashboard aside.
     */
    @Operation(summary = "Get daily quote")
    @ApiResponse(responseCode = "200", description = "Quote returned")
    @GetMapping("/daily")
    public ResponseEntity<QuoteDto> getDailyQuote() {
        return ResponseEntity.ok(service.getDailyQuote());
    }
}
