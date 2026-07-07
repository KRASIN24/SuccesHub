package com.succeshub.appdomain.service.impl;

import com.succeshub.appdomain.dto.gamification.GamificationDto.WeeklyInsightDto;
import com.succeshub.appdomain.model.XpEvent;
import com.succeshub.appdomain.repository.XpEventRepository;
import com.succeshub.appdomain.service.InsightService;
import com.succeshub.appdomain.service.gamification.GamificationTimeUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InsightServiceImpl implements InsightService {

    private final XpEventRepository xpEventRepository;
    private final GamificationTimeUtil timeUtil;

    @Override
    @Transactional(readOnly = true)
    public WeeklyInsightDto getWeeklyInsights(String userId) {
        LocalDate today = timeUtil.today();
        LocalDate thisWeekStart = timeUtil.mondayOfWeek(today);
        LocalDate lastWeekStart = thisWeekStart.minusWeeks(1);

        Instant thisStart = timeUtil.startOfDay(thisWeekStart);
        Instant thisEnd = timeUtil.endOfDay(today);
        Instant lastStart = timeUtil.startOfDay(lastWeekStart);
        Instant lastEnd = timeUtil.startOfDay(thisWeekStart);

        List<XpEvent> thisWeek = xpEventRepository.findByUserIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtAsc(
                userId, thisStart, thisEnd);
        List<XpEvent> lastWeek = xpEventRepository.findByUserIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtAsc(
                userId, lastStart, lastEnd);

        int tasksThisWeek = thisWeek.size();
        int tasksLastWeek = lastWeek.size();
        int hardThis = (int) thisWeek.stream().filter(e -> e.getDifficulty() >= 4).count();
        int hardLast = (int) lastWeek.stream().filter(e -> e.getDifficulty() >= 4).count();
        int xpThis = thisWeek.stream().mapToInt(XpEvent::getTotalXp).sum();
        int xpLast = lastWeek.stream().mapToInt(XpEvent::getTotalXp).sum();

        double hardDelta = hardLast == 0 ? (hardThis > 0 ? 100.0 : 0.0) : ((hardThis - hardLast) / (double) hardLast) * 100.0;
        String summary = hardDelta >= 0
                ? String.format("You completed %.0f%% more Hard tasks than last week.", hardDelta)
                : String.format("Hard task completions are down %.0f%% from last week — room to push.", Math.abs(hardDelta));

        return new WeeklyInsightDto(tasksThisWeek, tasksLastWeek, hardThis, hardLast, xpThis, xpLast, hardDelta, summary);
    }
}
