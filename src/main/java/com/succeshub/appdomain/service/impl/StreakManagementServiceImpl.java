package com.succeshub.appdomain.service.impl;

import com.succeshub.appdomain.dto.gamification.GamificationDto.StreakActionResultDto;
import com.succeshub.appdomain.dto.gamification.GamificationDto.StreakCalendarDto;
import com.succeshub.appdomain.dto.gamification.GamificationDto.StreakDayDto;
import com.succeshub.appdomain.dto.gamification.GamificationDto.UseItemResultDto;
import com.succeshub.appdomain.model.RewardDefinition;
import com.succeshub.appdomain.model.StreakDayOverride;
import com.succeshub.appdomain.model.UserInventory;
import com.succeshub.appdomain.model.UserProfile;
import com.succeshub.appdomain.repository.StreakDayOverrideRepository;
import com.succeshub.appdomain.repository.TaskRepository;
import com.succeshub.appdomain.repository.UserInventoryRepository;
import com.succeshub.appdomain.repository.UserProfileRepository;
import com.succeshub.appdomain.service.StreakManagementService;
import com.succeshub.appdomain.service.StreakService;
import com.succeshub.appdomain.service.UserProfileService;
import com.succeshub.appdomain.service.gamification.GamificationTimeUtil;
import com.succeshub.config.GamificationProperties;
import com.succeshub.coreinfra.exception_handler.domain.ValidationException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Default {@link StreakManagementService}. Streak state lives on {@link UserProfile};
 * per-day shield protection is recorded in {@code streak_day_override}.
 *
 * <p>Shield spend/availability delegates to {@link StreakService} so auto-miss protection and
 * calendar shield spends share one accounting model.
 *
 * <p>V1 uses server timezone for day boundaries (see {@link GamificationTimeUtil}).
 */
@Service
@RequiredArgsConstructor
public class StreakManagementServiceImpl implements StreakManagementService {

    private static final String DOUBLE_STRIKE_KEY = "DOUBLE_STRIKE";
    private static final String SURGE_TOKEN_KEY = "SURGE_TOKEN";
    private static final int DEFAULT_BOOST_XP = 25;

    private final UserProfileService userProfileService;
    private final UserProfileRepository profileRepository;
    private final TaskRepository taskRepository;
    private final StreakDayOverrideRepository overrideRepository;
    private final UserInventoryRepository inventoryRepository;
    private final StreakService streakService;
    private final GamificationProperties properties;
    private final GamificationTimeUtil timeUtil;

    @Override
    @Transactional
    public StreakCalendarDto getCalendar(String userId, YearMonth month) {
        UserProfile profile = userProfileService.requireProfile(userId);
        LocalDate first = month.atDay(1);
        LocalDate last = month.atEndOfMonth();
        LocalDate today = timeUtil.today();
        int minTasks = properties.getMinTasksForQualifyingDay();

        Map<LocalDate, Integer> completions = countCompletionsByDay(userId, first, last);
        Set<LocalDate> shielded = overrideRepository.findByUserIdAndDayBetween(userId, first, last).stream()
                .map(StreakDayOverride::getDay)
                .collect(Collectors.toSet());
        LocalDate joined = profile.getCreatedAt().atZone(timeUtil.zone()).toLocalDate();

        List<StreakDayDto> days = first.datesUntil(last.plusDays(1))
                .map(day -> {
                    int completed = completions.getOrDefault(day, 0);
                    String status = classify(day, today, joined, completed, minTasks, shielded.contains(day));
                    return new StreakDayDto(day, status, completed);
                })
                .toList();

        return new StreakCalendarDto(
                first,
                profile.getCurrentStreak(),
                tierName(profile),
                profile.getStreakShields(),
                streakService.shieldsAvailable(userId, profile),
                minTasks,
                days
        );
    }

    @Override
    @Transactional
    public StreakActionResultDto adjustStreak(String userId, int delta) {
        UserProfile profile = userProfileService.requireProfile(userId);
        applyStreakValue(profile, profile.getCurrentStreak() + delta);
        pinManualStreakValue(profile);
        profileRepository.save(profile);
        return result(userId, profile, "Streak set to " + profile.getCurrentStreak() + " day(s).");
    }

    @Override
    @Transactional
    public StreakActionResultDto setStreak(String userId, int value) {
        UserProfile profile = userProfileService.requireProfile(userId);
        applyStreakValue(profile, value);
        pinManualStreakValue(profile);
        profileRepository.save(profile);
        return result(userId, profile, "Streak set to " + profile.getCurrentStreak() + " day(s).");
    }

    @Override
    @Transactional
    public StreakActionResultDto resetStreak(String userId) {
        UserProfile profile = userProfileService.requireProfile(userId);
        profile.setCurrentStreak(0);
        profile.setStreakTier(UserProfile.StreakTier.NONE);
        profile.setLastProcessedDay(timeUtil.today().minusDays(1));
        profileRepository.save(profile);
        return result(userId, profile, "Streak reset.");
    }

    @Override
    @Transactional
    public StreakActionResultDto shieldDay(String userId, LocalDate date) {
        UserProfile profile = userProfileService.requireProfile(userId);
        LocalDate today = timeUtil.today();

        if (date.isAfter(today)) {
            throw new ValidationException("Cannot shield a day in the future.");
        }
        LocalDate joined = profile.getCreatedAt().atZone(timeUtil.zone()).toLocalDate();
        if (date.isBefore(joined)) {
            throw new ValidationException("Cannot shield a day before your account existed.");
        }
        if (overrideRepository.existsByUserIdAndDay(userId, date)) {
            throw new ValidationException("That day is already protected.");
        }
        long completed = taskRepository.countCompletedInRange(
                userId, timeUtil.startOfDay(date), timeUtil.endOfDay(date));
        if (completed >= properties.getMinTasksForQualifyingDay()) {
            throw new ValidationException("That day already qualifies — no shield needed.");
        }
        if (!streakService.consumeShield(userId, profile)) {
            throw new ValidationException("You have no streak shields to spend.");
        }

        StreakDayOverride override = new StreakDayOverride();
        override.setUserId(userId);
        override.setDay(date);
        override.setKind(StreakDayOverride.Kind.SHIELDED);
        overrideRepository.save(override);

        // Only repair the streak here for days the daily ritual has already settled. Days still
        // inside the unprocessed window are counted by the ritual via the override (see
        // DailyRitualServiceImpl#isQualifyingDay), which avoids double-counting.
        LocalDate lastProcessed = profile.getLastProcessedDay();
        boolean alreadyProcessed = lastProcessed != null && !date.isAfter(lastProcessed);
        if (alreadyProcessed) {
            applyStreakValue(profile, profile.getCurrentStreak() + 1);
            if (profile.getLastActiveDate() == null || date.isAfter(profile.getLastActiveDate())) {
                profile.setLastActiveDate(date);
            }
        }
        profileRepository.save(profile);

        return result(userId, profile, "Shield applied to " + date + ". Streak protected.");
    }

    @Override
    @Transactional
    public UseItemResultDto useInventoryItem(String userId, UUID inventoryId) {
        UserInventory item = inventoryRepository.findById(inventoryId)
                .filter(i -> i.getUserId().equals(userId))
                .orElseThrow(() -> new EntityNotFoundException("Inventory item not found"));
        if (item.getQuantity() <= 0) {
            throw new ValidationException("That item is out of stock.");
        }

        RewardDefinition reward = item.getRewardDefinition();
        UserProfile profile = userProfileService.requireProfile(userId);
        String message;

        switch (reward.getType()) {
            case SHIELD -> {
                profile.setStreakShields(profile.getStreakShields() + 1);
                message = "Streak shield banked. It will absorb your next missed day, "
                        + "or spend it on a specific day from the calendar.";
            }
            case XP_BOOST -> {
                if (DOUBLE_STRIKE_KEY.equals(reward.getKey())) {
                    profile.setPendingDoubleStrike(true);
                    message = "Double Strike armed — your next task completion awards 2× XP.";
                } else if (SURGE_TOKEN_KEY.equals(reward.getKey())) {
                    profile.setPendingSurgeToken(true);
                    message = "Surge Token armed — your next task gains +50 XP (ignores daily cap).";
                } else {
                    profile.addXp(DEFAULT_BOOST_XP);
                    message = "Used " + reward.getLabel() + ": +" + DEFAULT_BOOST_XP + " XP.";
                }
            }
            default -> throw new ValidationException(
                    reward.getLabel() + " is a cosmetic — equip it from your collection instead of using it.");
        }

        int remaining = decrementStack(item);
        profileRepository.save(profile);
        return new UseItemResultDto(reward.getKey(), message, remaining, userProfileService.mapToDto(profile));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Map<LocalDate, Integer> countCompletionsByDay(String userId, LocalDate first, LocalDate last) {
        Instant start = timeUtil.startOfDay(first);
        Instant end = timeUtil.endOfDay(last);
        Map<LocalDate, Integer> counts = new HashMap<>();
        for (Instant completedAt : taskRepository.findCompletedAtInRange(userId, start, end)) {
            if (completedAt == null) {
                continue;
            }
            LocalDate day = completedAt.atZone(timeUtil.zone()).toLocalDate();
            counts.merge(day, 1, Integer::sum);
        }
        return counts;
    }

    private String classify(
            LocalDate day,
            LocalDate today,
            LocalDate joined,
            int completed,
            int minTasks,
            boolean shielded
    ) {
        if (day.isAfter(today)) {
            return "FUTURE";
        }
        // Days before the profile existed are not part of the streak window.
        if (day.isBefore(joined)) {
            return "EMPTY";
        }
        if (completed >= minTasks) {
            return "COMPLETED";
        }
        if (shielded) {
            return "SHIELDED";
        }
        if (day.equals(today)) {
            return "TODAY";
        }
        return "MISSED";
    }

    private void applyStreakValue(UserProfile profile, int value) {
        int clamped = Math.max(0, value);
        profile.setCurrentStreak(clamped);
        profile.setStreakTier(UserProfile.tierForStreak(clamped));
    }

    private void pinManualStreakValue(UserProfile profile) {
        // Keep lazy daily settlement from replaying historical days and replacing
        // the value that a developer just set through the gated testing endpoint.
        profile.setLastProcessedDay(timeUtil.today().minusDays(1));
        profile.setLastActiveDate(timeUtil.today());
    }

    /** Decrements a stack, deleting the row when it hits zero. Returns remaining quantity. */
    private int decrementStack(UserInventory item) {
        int remaining = Math.max(0, item.getQuantity() - 1);
        if (remaining == 0) {
            inventoryRepository.delete(item);
        } else {
            item.setQuantity(remaining);
            inventoryRepository.save(item);
        }
        return remaining;
    }

    private StreakActionResultDto result(String userId, UserProfile profile, String message) {
        return new StreakActionResultDto(
                profile.getCurrentStreak(),
                tierName(profile),
                profile.getStreakShields(),
                streakService.shieldsAvailable(userId, profile),
                message
        );
    }

    private String tierName(UserProfile profile) {
        return profile.getStreakTier() != null
                ? profile.getStreakTier().name()
                : UserProfile.StreakTier.NONE.name();
    }
}
