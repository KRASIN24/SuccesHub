package com.successhub.appdomain.service.impl;

import com.successhub.appdomain.dto.gamification.XpPreviewDto;
import com.successhub.appdomain.entity.UserProfile;
import com.successhub.appdomain.service.GamificationService;
import com.successhub.appdomain.service.UserProfileService;
import com.successhub.appdomain.service.gamification.GamificationTimeUtil;
import com.successhub.appdomain.service.gamification.XpCalculator;
import com.successhub.appdomain.config.GamificationProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GamificationServiceImpl implements GamificationService {

    private final UserProfileService userProfileService;
    private final XpCalculator xpCalculator;
    private final GamificationProperties properties;
    private final GamificationTimeUtil timeUtil;

    @Override
    @Transactional(readOnly = true)
    public XpPreviewDto previewXp(String userId, int difficulty, int durationMinutes, int priority, boolean weeklyChallenge) {
        UserProfile profile = userProfileService.requireProfile(userId);
        userProfileService.resetDailyCountersIfNeeded(profile, timeUtil.today());

        int baseXp = xpCalculator.computeBaseXp(difficulty, durationMinutes, priority);
        int streakBonus = xpCalculator.computeStreakBonus(baseXp, profile.getCurrentStreak());
        int firstTaskBonus = profile.isFirstTaskCompletedToday() ? 0 : properties.getFirstTaskBonusXp();
        int challengeBonus = xpCalculator.computeChallengeBonus(baseXp, weeklyChallenge);
        int maxVariable = (int) Math.round(baseXp * properties.getVariableBonusMax());
        int estimated = xpCalculator.previewTotal(baseXp, streakBonus, firstTaskBonus, challengeBonus, profile.getDailyXpEarned());
        int remaining = Math.max(0, properties.getDailyXpCap() - profile.getDailyXpEarned());

        return new XpPreviewDto(baseXp, streakBonus, firstTaskBonus, maxVariable, challengeBonus, estimated, remaining);
    }
}
