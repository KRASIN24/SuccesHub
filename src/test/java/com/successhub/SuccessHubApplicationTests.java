package com.successhub;

import com.successhub.appdomain.repository.AchievementDefinitionRepository;
import com.successhub.appdomain.repository.GoalCategorySubscriptionRepository;
import com.successhub.appdomain.repository.GoalRepository;
import com.successhub.appdomain.repository.LootBoxContentRepository;
import com.successhub.appdomain.repository.RewardDefinitionRepository;
import com.successhub.appdomain.repository.StreakDayOverrideRepository;
import com.successhub.appdomain.repository.TaskCategoryRepository;
import com.successhub.appdomain.repository.TaskRepository;
import com.successhub.appdomain.repository.UserAchievementRepository;
import com.successhub.appdomain.repository.UserInventoryRepository;
import com.successhub.appdomain.repository.UserLootBoxRepository;
import com.successhub.appdomain.repository.UserProfileRepository;
import com.successhub.appdomain.repository.XpEventRepository;
import com.successhub.appdomain.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
		"spring.autoconfigure.exclude=" +
				"org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration," +
				"org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration," +
				"org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration",
		"spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost:9999/fake-jwks"
})
@ActiveProfiles("test")
@AutoConfigureMockMvc
class SuccessHubApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private UserProfileRepository userProfileRepository;
	@MockitoBean
	private TaskCategoryRepository taskCategoryRepository;
	@MockitoBean
	private TaskRepository taskRepository;
	@MockitoBean
	private GoalRepository goalRepository;
	@MockitoBean
	private GoalCategorySubscriptionRepository goalCategorySubscriptionRepository;
	@MockitoBean
	private AchievementDefinitionRepository achievementDefinitionRepository;
	@MockitoBean
	private UserAchievementRepository userAchievementRepository;
	@MockitoBean
	private UserInventoryRepository userInventoryRepository;
	@MockitoBean
	private RewardDefinitionRepository rewardDefinitionRepository;
	@MockitoBean
	private XpEventRepository xpEventRepository;
	@MockitoBean
	private UserLootBoxRepository userLootBoxRepository;
	@MockitoBean
	private LootBoxContentRepository lootBoxContentRepository;
	@MockitoBean
	private StreakDayOverrideRepository streakDayOverrideRepository;
	@MockitoBean
	private NotificationRepository notificationRepository;

	@Test
	void contextLoads() {
	}

	@Test
	void swaggerUiIsPublic() throws Exception {
		// Act & Assert
		mockMvc.perform(get("/swagger-ui/index.html"))
				.andExpect(status().isOk());
	}

	@Test
	void openApiDocsArePublicAndCorrect() throws Exception {
		// Act & Assert
		mockMvc.perform(get("/v3/api-docs"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.openapi").exists())
				.andExpect(jsonPath("$.info.title").value("SuccessHub API"));
	}

	@Test
	void apiProfileRequiresAuthentication() throws Exception {
		// Act & Assert
		mockMvc.perform(get("/api/profile"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void apiTasksRequiresAuthentication() throws Exception {
		// Act & Assert
		mockMvc.perform(get("/api/tasks"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void apiGamificationDailyRequiresAuthentication() throws Exception {
		// Act & Assert
		mockMvc.perform(get("/api/gamification/daily"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void apiNotificationsRequiresAuthentication() throws Exception {
		// Act & Assert
		mockMvc.perform(get("/api/notifications"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void csrfCookieIsIssuedOnApiGet() throws Exception {
		// Act & Assert
		mockMvc.perform(get("/api/profile"))
				.andExpect(status().isUnauthorized())
				.andExpect(cookie().exists("XSRF-TOKEN"));
	}
}
