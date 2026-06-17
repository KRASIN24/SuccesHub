package com.succeshub.appdomain.controller;

import com.succeshub.appdomain.dto.LunarDto;
import com.succeshub.appdomain.service.LunarService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/astronomy")
@RequiredArgsConstructor
public class AstronomyController {

    private final LunarService service;

    @GetMapping("/lunar")
    public ResponseEntity<LunarDto> getLunarPhase() {
        return ResponseEntity.ok(service.getLunarPhase());
    }
}
