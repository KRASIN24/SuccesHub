package com.successhub.appdomain.dto.gamification;

public record WeeklyInsightDto(
        int tasksThisWeek,
        int tasksLastWeek,
        int hardTasksThisWeek,
        int hardTasksLastWeek,
        int xpThisWeek,
        int xpLastWeek,
        double hardTaskDeltaPercent,
        String summaryText
) {}
