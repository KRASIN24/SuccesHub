package com.succeshub.appdomain.service.gamification;

import com.succeshub.appdomain.model.Task;
import com.succeshub.appdomain.model.UserProfile;
import com.succeshub.config.GamificationProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class XpCalculatorTest {

    private XpCalculator calculator;
    private GamificationProperties properties;

    @BeforeEach
    void setUp() {
        properties = new GamificationProperties();
        calculator = new XpCalculator(properties);
    }

    @Test
    void computeBaseXp_usesFormula() {
        assertEquals(60, calculator.computeBaseXp(3, 30, 2));
        assertEquals(120, calculator.computeBaseXp(3, 60, 2));
    }

    @Test
    void streakBonus_appliesAtSevenDays() {
        int base = 100;
        assertEquals(0, calculator.computeStreakBonus(base, 6));
        assertEquals(10, calculator.computeStreakBonus(base, 7));
        assertEquals(20, calculator.computeStreakBonus(base, 30));
    }

    @Test
    void dailyCap_truncatesTotal() {
        assertEquals(50, calculator.applyDailyCap(200, 250));
        assertEquals(0, calculator.applyDailyCap(100, 300));
    }

    @Test
    void calculateForCompletion_includesFirstTaskBonus() {
        UserProfile profile = new UserProfile();
        profile.setCurrentStreak(7);
        profile.setDailyXpEarned(0);
        profile.setFirstTaskCompletedToday(false);

        Task task = new Task();
        task.setDifficulty(3);
        task.setDurationMinutes(30);
        task.setPriority(2);
        task.setWeeklyChallenge(false);

        XpCalculator.XpResult result = calculator.calculateForCompletion(task, profile, false);
        assertEquals(10, result.firstTaskBonus());
        assertTrue(result.totalXp() >= 70);
    }
}
