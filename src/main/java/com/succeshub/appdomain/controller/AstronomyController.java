package com.succeshub.appdomain.controller;

import com.succeshub.appdomain.dto.LunarDto;
import com.succeshub.appdomain.service.LunarService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST API for the dashboard lunar phase widget.
 */
@Tag(name = "Astronomy", description = "Lunar phase computation")
@RestController
@RequestMapping("/api/astronomy")
@RequiredArgsConstructor
public class AstronomyController {

    private final LunarService service;

    /**
     * Returns the current lunar phase, illumination percentage, and approximate rise/set times.
     */
    @Operation(summary = "Get lunar phase", description = "Computed server-side from the current UTC date; result is cached.")
    @ApiResponse(responseCode = "200", description = "Lunar data returned")
    @GetMapping("/lunar")
    public ResponseEntity<LunarDto> getLunarPhase() {
        return ResponseEntity.ok(service.getLunarPhase());
    }
}
