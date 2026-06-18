package com.succeshub.appdomain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record LunarDto(
        String phaseName,
        @JsonProperty("illumination") int illuminationPercent,
        String riseTime,
        String setTime
) {}
