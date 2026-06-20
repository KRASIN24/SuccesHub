package com.succeshub.appdomain.service.impl;

import com.succeshub.appdomain.model.AchievementDefinition;
import com.succeshub.appdomain.repository.AchievementDefinitionRepository;
import com.succeshub.appdomain.repository.GoalRepository;
import com.succeshub.appdomain.repository.UserAchievementRepository;
import com.succeshub.appdomain.model.UserProfile;
import com.succeshub.config.GamificationProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AchievementEvaluatorImplTest {

    @Mock
    private AchievementDefinitionRepository definitionRepository;
    @Mock
    private UserAchievementRepository userAchievementRepository;
    @Mock
    private GoalRepository goalRepository;

    private GamificationProperties properties;

    private AchievementEvaluatorImpl evaluator;

    @BeforeEach
    void setUp() {
        properties = new GamificationProperties();
        properties.setSpeedsterTaskThreshold(10);
        properties.setMinTasksForQualifyingDay(1);
        evaluator = new AchievementEvaluatorImpl(definitionRepository, userAchievementRepository, goalRepository, properties);
    }

    @Test
    void speedster_requiresThreshold_notMinTasksForQualifyingDay() {
        AchievementDefinition speedster = def("SPEEDSTER");
        when(definitionRepository.findAll()).thenReturn(List.of(speedster));
        when(userAchievementRepository.findByUserId("user")).thenReturn(List.of());
        when(goalRepository.countByUserIdAndStatus(any(), any())).thenReturn(0L);

        UserProfile profile = new UserProfile();
        profile.setKeycloakId("user");

        assertEquals(0, evaluator.evaluateAndUnlock("user", profile, 9).size());
        verify(userAchievementRepository, never()).save(any());

        assertEquals(1, evaluator.evaluateAndUnlock("user", profile, 10).size());
    }

    private AchievementDefinition def(String key) {
        AchievementDefinition d = new AchievementDefinition();
        d.setId(UUID.randomUUID());
        d.setKey(key);
        d.setLabel(key);
        d.setIcon("star");
        d.setDescription("test");
        return d;
    }
}
