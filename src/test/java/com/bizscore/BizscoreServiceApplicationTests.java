package com.bizscore;

import com.bizscore.client.MlServiceClient;
import com.bizscore.config.TestSecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
class BizscoreServiceApplicationTests {

	@MockBean
	private MlServiceClient mlServiceClient;

	@Test
	void contextLoads() {
		// Тест проверяет, что ApplicationContext загружается успешно
		// TestSecurityConfig упрощает Security для тестов
	}

}
