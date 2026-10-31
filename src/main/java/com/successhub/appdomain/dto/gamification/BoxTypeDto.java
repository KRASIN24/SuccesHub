package com.successhub.appdomain.dto.gamification;

/** A selectable box type with its display metadata and rarity odds (percentages). */
public record BoxTypeDto(
        String id,
        String name,
        String source,
        String feel,
        String blurb,
        String icon,
        int commonWeight,
        int rareWeight,
        int legendaryWeight
) {}
