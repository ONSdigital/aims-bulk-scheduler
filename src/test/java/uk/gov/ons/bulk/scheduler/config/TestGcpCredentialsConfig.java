package uk.gov.ons.bulk.scheduler.config;

import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.NoCredentialsProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

@Configuration(proxyBeanMethods = false)
@Profile("test")
public class TestGcpCredentialsConfig {

    @Bean
    @Primary
    CredentialsProvider credentialsProvider() {
        return NoCredentialsProvider.create();
    }
}

