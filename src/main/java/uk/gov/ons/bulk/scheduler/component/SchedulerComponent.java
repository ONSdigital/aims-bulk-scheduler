package uk.gov.ons.bulk.scheduler.component;

import static uk.gov.ons.bulk.scheduler.util.SchedulerConstants.SCHEDULER_GROUP;
import static uk.gov.ons.bulk.scheduler.util.SchedulerConstants.TRIGGER_GROUP;

import java.time.ZonedDateTime;
import java.util.Date;

import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import uk.gov.ons.bulk.scheduler.entities.BigQueryJob;

@Component
public class SchedulerComponent {
	
	@Value("${aims.scheduler.frequency-minutes}")
	private int frequencyInMinutes;
	
	public JobDetail createJobDetail(String jobName, String jobId, String idsJobId, int expectedRows) {
		
		JobDataMap jobDataMap = new JobDataMap();
		jobDataMap.put("jobId", jobId);
		jobDataMap.put("idsJobId", idsJobId);
		jobDataMap.put("expectedRows", expectedRows);
		
        return JobBuilder.newJob(BigQueryJob.class)
                .withIdentity(jobName, SCHEDULER_GROUP)
                .withDescription("Query BigQuery status of result table")
                .usingJobData(jobDataMap)
                .build();
	}
	
    public Trigger createTrigger(JobDetail jobDetail) {
        return TriggerBuilder.newTrigger()
                .forJob(jobDetail)
                .withIdentity(jobDetail.getKey().getName(), TRIGGER_GROUP)
                .startAt(Date.from(ZonedDateTime.now().plusMinutes(frequencyInMinutes).toInstant()))
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                		.withIntervalInMinutes(frequencyInMinutes)
                		.withMisfireHandlingInstructionFireNow()
                		.repeatForever())
                .build();
    }
}
