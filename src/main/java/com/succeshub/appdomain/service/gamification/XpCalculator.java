package com.succeshub.appdomain.service.gamification;

import com.succeshub.appdomain.model.Task;
import com.succeshub.appdomain.model.UserProfile;
import com.succeshub.config.GamificationProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Pure XP calculation helper. Stateless aside from injected configuration.
 */
@Component
@RequiredArgsConstructor
public class XpCalculator {

    private final GamificationProperties properties;

    public int durationFactor(int durationMinutes) {
        return Math.max(1, Math.round(durationMinutes / 30.0f));
    }

    public int computeBaseXp(int difficulty, int durationMinutes, int priority) {
        return 10 * difficulty * durationFactor(durationMinutes) * priority;
    }

    public int computeStreakBonus(int baseXp, int currentStreak) {
        return (int) Math.round(baseXp * properties.streakMultiplierRate(currentStreak));
    }

    public int computeChallengeBonus(int baseXp, boolean weeklyChallenge) {
        if (!weeklyChallenge) {
            return 0;
        }
        return (int) Math.round(baseXp * properties.getWeeklyChallengeBonusRate());
    }

    public int computeVariableBonus(int baseXp, boolean roll) {
        if (!roll) {
            return 0;
        }
        double rate = ThreadLocalRandom.current().nextDouble(
                properties.getVariableBonusMin(), properties.getVariableBonusMax());
        return (int) Math.round(baseXp * rate);
    }

    public boolean rollVariableBonus() {
        return ThreadLocalRandom.current().nextDouble() < properties.getVariableBonusChance();
    }

    public int applyDailyCap(int total, int dailyXpEarned) {
        int remaining = properties.getDailyXpCap() - dailyXpEarned;
        return Math.max(0, Math.min(total, remaining));
    }

    public int previewTotal(int baseXp, int streakBonus, int firstTaskBonus, int challengeBonus, int dailyXpEarned) {
        return applyDailyCap(baseXp + streakBonus + firstTaskBonus + challengeBonus, dailyXpEarned);
    }

    public XpResult calculateForCompletion(
            Task task,
            UserProfile profile,
            boolean applyVariableBonus) {
        int baseXp = computeBaseXp(task.getDifficulty(), task.getDurationMinutes(), task.getPriority());
        int streakBonus = computeStreakBonus(baseXp, profile.getCurrentStreak());
        int firstTaskBonus = profile.isFirstTaskCompletedToday() ? 0 : properties.getFirstTaskBonusXp();
        int challengeBonus = computeChallengeBonus(baseXp, task.isWeeklyChallenge());
        boolean variableTriggered = applyVariableBonus && rollVariableBonus();
        int variableBonus = computeVariableBonus(baseXp, variableTriggered);
        int rawTotal = baseXp + streakBonus + firstTaskBonus + variableBonus + challengeBonus;
        int totalXp = applyDailyCap(rawTotal, profile.getDailyXpEarned());
        return new XpResult(baseXp, streakBonus, firstTaskBonus, variableBonus, challengeBonus, totalXp, variableTriggered);
    }

    public record XpResult(
            int baseXp,
            int streakBonus,
            int firstTaskBonus,
            int variableBonus,
            int challengeBonus,
            int totalXp,
            boolean variableBonusTriggered
    ) {}
}
