package com.bizscore;

import com.bizscore.client.MlServiceClient;
import com.bizscore.config.JwtAuthenticationFilter;
import com.bizscore.config.RateLimitFilter;
import com.bizscore.service.CustomUserDetailsService;
import com.bizscore.service.JwtService;
import com.bizscore.service.RateLimitService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
class BizscoreServiceApplicationTests {

	@MockBean
	private MlServiceClient mlServiceClient;

	@MockBean
	private JwtAuthenticationFilter jwtAuthenticationFilter;

	@MockBean
	private RateLimitFilter rateLimitFilter;

	@MockBean
	private CustomUserDetailsService customUserDetailsService;

	@MockBean
	private JwtService jwtService;

	@MockBean
	private RateLimitService rateLimitService;

	@Test
	void contextLoads() {
		// Тест проверяет, что ApplicationContext загружается успешно
		// Если есть проблемы с зависимостями, они будут обнаружены здесь
	}

}
