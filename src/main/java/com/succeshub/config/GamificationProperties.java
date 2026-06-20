package com.succeshub.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Tunable gamification constants. All services inject this bean; avoid magic numbers in service code.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "succeshub.gamification")
public class GamificationProperties {

    private int dailyXpCap = 300;
    private int minTasksForQualifyingDay = 1;
    /**
     * Hour of day (0–23) for grace-period cutoff. V1 uses {@link java.time.ZoneId#systemDefault() server timezone};
     * user timezone support is deferred to V2.
     */
    private int graceHourLocal = 12;
    private int speedsterTaskThreshold = 10;
    private int firstTaskBonusXp = 10;
    private double variableBonusChance = 0.15;
    private double variableBonusMin = 0.10;
    private double variableBonusMax = 0.50;
    private double weeklyChallengeBonusRate = 0.50;
    private int weeklyChallengeCount = 3;
    private int weeklyChallengeMinDifficulty = 4;
    private int bossDamageFactor = 2;
    private int weeklyLootMinQualifyingDays = 3;
    private Map<Integer, Double> streakMultipliers = defaultStreakMultipliers();

    private static Map<Integer, Double> defaultStreakMultipliers() {
        Map<Integer, Double> map = new LinkedHashMap<>();
        map.put(7, 0.10);
        map.put(30, 0.20);
        map.put(100, 0.35);
        return map;
    }

    /**
     * Returns the highest streak multiplier rate for the given streak length.
     *
     * @param streak current consecutive-day streak
     * @return additive rate applied to base XP (e.g. 0.10 for +10%)
     */
    public double streakMultiplierRate(int streak) {
        double rate = 0.0;
        for (Map.Entry<Integer, Double> entry : streakMultipliers.entrySet()) {
            if (streak >= entry.getKey()) {
                rate = entry.getValue();
            }
        }
        return rate;
    }
}
