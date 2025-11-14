package uk.gov.ons.bulk.scheduler.component;

import static org.junit.jupiter.api.Assertions.*;

import java.time.ZonedDateTime;
import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.quartz.JobDetail;
import org.quartz.Trigger;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.ons.bulk.scheduler.util.SchedulerConstants;

class SchedulerComponentTest {

    private SchedulerComponent schedulerComponent;

    @BeforeEach
    void setUp() {
        schedulerComponent = new SchedulerComponent();
        ReflectionTestUtils.setField(schedulerComponent, "frequencyInMinutes", 10);
    }

    @Test
    void testCreateJobDetail() {
        String jobName = "testJob";
        String jobId = "jobId123";
        String idsJobId = "idsJobId456";
        int expectedRows = 5;

        JobDetail jobDetail = schedulerComponent.createJobDetail(jobName, jobId, idsJobId, expectedRows);

        assertNotNull(jobDetail);
        assertEquals(jobName, jobDetail.getKey().getName());
        assertEquals(SchedulerConstants.SCHEDULER_GROUP, jobDetail.getKey().getGroup());
        assertEquals("Query BigQuery status of result table", jobDetail.getDescription());
        assertEquals(jobId, jobDetail.getJobDataMap().getString("jobId"));
        assertEquals(idsJobId, jobDetail.getJobDataMap().getString("idsJobId"));
        assertEquals(expectedRows, jobDetail.getJobDataMap().getInt("expectedRows"));
    }

    @Test
    void testCreateTrigger() {
        JobDetail jobDetail = schedulerComponent.createJobDetail("testJob", "jobId", "idsJobId", 1);
        Trigger trigger = schedulerComponent.createTrigger(jobDetail);

        assertNotNull(trigger);
        assertEquals("testJob", trigger.getKey().getName());
        assertEquals(SchedulerConstants.TRIGGER_GROUP, trigger.getKey().getGroup());
        assertEquals(jobDetail.getKey(), trigger.getJobKey());

        Date now = Date.from(ZonedDateTime.now().toInstant());
        assertTrue(trigger.getStartTime().after(now) || trigger.getStartTime().equals(now));
    }
}