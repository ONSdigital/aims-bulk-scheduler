package uk.gov.ons.component;

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

import uk.gov.ons.entities.BigQueryJob;

@Component
public class SchedulerComponent {
	
	@Value("${aims.scheduler.frequency-minutes}")
	private int frequencyInMinutes;
	
	public JobDetail createJobDetail(String jobName, String tableId, int expectedRows) {
		
		JobDataMap jobDataMap = new JobDataMap();
		jobDataMap.put("tableId", tableId);
		jobDataMap.put("expectedRows", expectedRows);
		
        return JobBuilder.newJob(BigQueryJob.class)
                .withIdentity(jobName, "bulk-query-jobs")
                .withDescription("Query BigQuery status of result table")
                .usingJobData(jobDataMap)
                .build();
	}
	
    public Trigger createTrigger(JobDetail jobDetail) {
        return TriggerBuilder.newTrigger()
                .forJob(jobDetail)
                .withIdentity(jobDetail.getKey().getName(), "bulk-query-triggers")
                .withDescription("Query BigQuery status of result table Trigger")
                .startAt(Date.from(ZonedDateTime.now().plusMinutes(frequencyInMinutes).toInstant()))
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                		.withIntervalInMinutes(frequencyInMinutes)
                		.withMisfireHandlingInstructionFireNow()
                		.repeatForever())
                .build();
    }
}
