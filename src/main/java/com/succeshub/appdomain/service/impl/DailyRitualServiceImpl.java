package com.succeshub.appdomain.service.impl;

import com.succeshub.appdomain.dto.AchievementDto;
import com.succeshub.appdomain.dto.TaskDto;
import com.succeshub.appdomain.dto.gamification.GamificationDto.CloseDayResultDto;
import com.succeshub.appdomain.dto.gamification.GamificationDto.DailyStatusDto;
import com.succeshub.appdomain.dto.gamification.GamificationDto.ForecastDto;
import com.succeshub.appdomain.model.Task;
import com.succeshub.appdomain.model.UserProfile;
import com.succeshub.appdomain.repository.TaskRepository;
import com.succeshub.appdomain.repository.UserProfileRepository;
import com.succeshub.appdomain.service.AchievementEvaluator;
import com.succeshub.appdomain.service.DailyRitualService;
import com.succeshub.appdomain.service.LootBoxService;
import com.succeshub.appdomain.service.StreakService;
import com.succeshub.appdomain.service.UserProfileService;
import com.succeshub.appdomain.service.gamification.GamificationTimeUtil;
import com.succeshub.config.GamificationProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DailyRitualServiceImpl implements DailyRitualService {

    private final UserProfileService userProfileService;
    private final UserProfileRepository profileRepository;
    private final TaskRepository taskRepository;
    private final StreakService streakService;
    private final LootBoxService lootBoxService;
    private final AchievementEvaluator achievementEvaluator;
    private final GamificationProperties properties;
    private final GamificationTimeUtil timeUtil;
    private final TaskMapper taskMapper;

    @Override
    @Transactional
    public CloseDayResultDto closePendingDays(String userId) {
        UserProfile profile = userProfileService.requireProfile(userId);
        userProfileService.resetDailyCountersIfNeeded(profile, timeUtil.today());

        int streakBefore = profile.getCurrentStreak();
        List<UUID> lootBoxes = new ArrayList<>();
        List<AchievementDto> achievements = new ArrayList<>();

        LocalDate today = timeUtil.today();
        LocalDate yesterday = today.minusDays(1);
        LocalDate cursor = profile.getLastProcessedDay() == null ? yesterday : profile.getLastProcessedDay().plusDays(1);

        if (cursor.isAfter(yesterday)) {
            return new CloseDayResultDto(streakBefore, profile.getCurrentStreak(),
                    tierName(profile), List.of(), List.of(), true);
        }

        while (!cursor.isAfter(yesterday)) {
            boolean qualifying = isQualifyingDay(userId, cursor);
            if (!qualifying && cursor.equals(yesterday) && isGraceActive(userId, today)) {
                qualifying = true;
            }
            streakService.applyDayOutcome(profile, cursor, qualifying);
            UUID loot = lootBoxService.checkStreakMilestone(userId, profile.getCurrentStreak());
            if (loot != null) {
                lootBoxes.add(loot);
            }
            profile.setLastProcessedDay(cursor);
            cursor = cursor.plusDays(1);
        }

        long tasksToday = taskRepository.countCompletedInRange(
                userId, timeUtil.startOfDay(today), timeUtil.endOfDay(today));
        achievements.addAll(achievementEvaluator.evaluateAndUnlock(userId, profile, tasksToday));
        profileRepository.save(profile);

        return new CloseDayResultDto(streakBefore, profile.getCurrentStreak(), tierName(profile), achievements, lootBoxes, false);
    }

    @Override
    @Transactional
    public DailyStatusDto getDailyStatus(String userId) {
        closePendingDays(userId);
        UserProfile profile = userProfileService.requireProfile(userId);
        LocalDate today = timeUtil.today();

        List<Task> scheduled = taskRepository.findByUserIdAndScheduledDateAndStatusOrderByCreatedAtDesc(
                userId, today, Task.Status.TODO);
        List<Task> weekly = taskRepository.findByUserIdAndWeeklyChallengeTrueAndWeeklyChallengeWeek(
                userId, timeUtil.mondayOfWeek(today));

        long completedToday = taskRepository.countCompletedInRange(
                userId, timeUtil.startOfDay(today), timeUtil.endOfDay(today));
        int remaining = Math.max(0, properties.getDailyXpCap() - profile.getDailyXpEarned());
        boolean pendingCelebrations = completedToday >= properties.getMinTasksForQualifyingDay();

        return new DailyStatusDto(
                scheduled.size(),
                profile.getDailyXpEarned(),
                remaining,
                profile.getCurrentStreak(),
                tierName(profile),
                profile.getStreakShields(),
                pendingCelebrations,
                weekly.stream().map(taskMapper::toDto).toList(),
                scheduled.stream().map(taskMapper::toDto).toList()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public ForecastDto getForecast(String userId) {
        UserProfile profile = userProfileService.requireProfile(userId);
        int streak = profile.getCurrentStreak();
        int nextMilestone = streak < 7 ? 7 : streak < 30 ? 30 : streak < 100 ? 100 : 100;
        double nextRate = properties.streakMultiplierRate(nextMilestone);
        return new ForecastDto(
                nextMilestone,
                nextRate,
                properties.getWeeklyChallengeMinDifficulty(),
                properties.getWeeklyChallengeCount(),
                profile.getStreakShields()
        );
    }

    private boolean isQualifyingDay(String userId, LocalDate day) {
        long count = taskRepository.countCompletedInRange(
                userId, timeUtil.startOfDay(day), timeUtil.endOfDay(day));
        return count >= properties.getMinTasksForQualifyingDay();
    }

    private boolean isGraceActive(String userId, LocalDate today) {
        LocalTime now = LocalTime.now(timeUtil.zone());
        if (now.getHour() >= properties.getGraceHourLocal()) {
            return false;
        }
        long todayCount = taskRepository.countCompletedInRange(
                userId, timeUtil.startOfDay(today), timeUtil.endOfDay(today));
        return todayCount >= properties.getMinTasksForQualifyingDay();
    }

    private String tierName(UserProfile profile) {
        return profile.getStreakTier() != null ? profile.getStreakTier().name() : UserProfile.StreakTier.NONE.name();
    }
}
