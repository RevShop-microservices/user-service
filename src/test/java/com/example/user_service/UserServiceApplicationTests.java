package com.example.user_service;

import com.example.user_service.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest(properties = {
		"spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,org.springframework.cloud.netflix.eureka.EurekaClientAutoConfiguration",
		"spring.cloud.config.enabled=false",
		"eureka.client.enabled=false",
		"spring.cloud.discovery.enabled=false",
		"jwt.secret=mySuperSecretKeyForJwtSigningMustBeAtLeast256BitsLong!!",
		"jwt.expiration=3600000"
})
class UserServiceApplicationTests {

	@MockBean
	private UserRepository userRepository;

	@Test
	void contextLoads() {
	}

}
