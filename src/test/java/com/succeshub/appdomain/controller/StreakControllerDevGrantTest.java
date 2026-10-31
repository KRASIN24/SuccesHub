package com.succeshub.appdomain.controller;

import com.succeshub.appdomain.dto.gamification.GamificationDto.AdjustStreakRequest;
import com.succeshub.appdomain.dto.gamification.GamificationDto.SetStreakRequest;
import com.succeshub.appdomain.service.StreakManagementService;
import com.succeshub.config.LootProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class StreakControllerDevGrantTest {

    @Mock StreakManagementService streakManagementService;
    @Mock LootProperties lootProperties;
    @Mock OidcUser principal;

    @InjectMocks StreakController controller;

    @Test
    void manualStreakMutationsAreForbiddenWhenDevGrantsAreDisabled() {
        assertEquals(
                HttpStatus.FORBIDDEN,
                controller.adjustStreak(principal, new AdjustStreakRequest(1)).getStatusCode());
        assertEquals(
                HttpStatus.FORBIDDEN,
                controller.setStreak(principal, new SetStreakRequest(30)).getStatusCode());
        assertEquals(
                HttpStatus.FORBIDDEN,
                controller.resetStreak(principal).getStatusCode());

        verifyNoInteractions(streakManagementService);
    }
}
