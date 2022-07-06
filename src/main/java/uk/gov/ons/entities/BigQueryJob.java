package uk.gov.ons.entities;

import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import uk.gov.ons.service.JobService;

@Slf4j
@Component
public class BigQueryJob implements Job {
	
	@Autowired
	private JobService jobService;

	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {
		
		// Query BigQuery to get row count for the result table
		log.info("Job ** {} ** fired @ {}", context.getJobDetail().getKey().getName(), context.getFireTime());
		
		JobDataMap jobDataMap = context.getJobDetail().getJobDataMap();
        jobService.execute(jobDataMap.getString("jobId"), jobDataMap.getInt("expectedRows"), context.getJobDetail().getKey());

        log.info("Next job scheduled @ {}", context.getNextFireTime());
	}
}
