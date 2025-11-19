package uk.gov.ons.bulk.scheduler;

import org.junit.jupiter.api.Test;
import org.quartz.Scheduler;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.gov.ons.bulk.scheduler.service.JobService;

@SpringBootTest
@ActiveProfiles("test")
class AimsBulkServiceSchedulerApplicationTests {

    @MockitoBean
    private JobService jobService;

    @MockitoBean
    private Scheduler scheduler;

	@Test
	void contextLoads() {
	}

}
