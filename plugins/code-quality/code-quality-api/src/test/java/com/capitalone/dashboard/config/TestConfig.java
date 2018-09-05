package com.capitalone.dashboard.config;

import com.capitalone.dashboard.service.*;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/**
 * Spring context configuration for Testing purposes
 */
@Configuration
public class TestConfig {

	@Bean
	public CodeQualityService codeQualityService() {
		return Mockito.mock(CodeQualityService.class);
	}

}
