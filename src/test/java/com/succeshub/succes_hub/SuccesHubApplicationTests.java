package com.succeshub.succes_hub;

import com.succeshub.appdomain.repository.AchievementDefinitionRepository;
import com.succeshub.appdomain.repository.GoalRepository;
import com.succeshub.appdomain.repository.TaskCategoryRepository;
import com.succeshub.appdomain.repository.TaskRepository;
import com.succeshub.appdomain.repository.UserAchievementRepository;
import com.succeshub.appdomain.repository.UserProfileRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
class SuccesHubApplicationTests {

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
	private AchievementDefinitionRepository achievementDefinitionRepository;
	@MockitoBean
	private UserAchievementRepository userAchievementRepository;

	@Test
	void contextLoads() {
	}

	@Test
	void swaggerUiIsPublic() throws Exception {
		mockMvc.perform(get("/swagger-ui/index.html"))
				.andExpect(status().isOk());
	}

	@Test
	void openApiDocsArePublicAndCorrect() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.openapi").exists())
				.andExpect(jsonPath("$.info.title").value("SuccessHub API"));
	}
}
