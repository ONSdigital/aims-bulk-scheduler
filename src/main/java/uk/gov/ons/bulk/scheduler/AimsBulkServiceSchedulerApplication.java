package uk.gov.ons.bulk.scheduler;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@SpringBootApplication
public class AimsBulkServiceSchedulerApplication {

	public static void main(String[] args) {
		SpringApplication.run(AimsBulkServiceSchedulerApplication.class, args);
	}

	@Bean
	public OpenAPI customOpenAPI(@Value("${application-description}") String appDesciption,
			@Value("${application-version}") String appVersion) {
		return new OpenAPI()
				.info(new Info().title("Bulk Scheduler API").version(appVersion).description(appDesciption));
	}
}
