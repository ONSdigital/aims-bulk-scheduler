package uk.gov.ons.bulk.scheduler.service;

import static uk.gov.ons.bulk.scheduler.util.SchedulerConstants.SCHEDULER_GROUP;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryParameterValue;
import com.google.cloud.bigquery.TableResult;

import lombok.extern.slf4j.Slf4j;
import uk.gov.ons.bulk.scheduler.component.PubSubComponent.PubsubOutboundGateway;
import uk.gov.ons.bulk.scheduler.entities.BulkSchedulerJob;
import uk.gov.ons.bulk.scheduler.entities.BulkSchedulerTrigger;
import uk.gov.ons.bulk.scheduler.entities.Exportable;
import uk.gov.ons.bulk.scheduler.entities.QueryCountResult;
import uk.gov.ons.bulk.scheduler.entities.QueryRowCountResult;

@Slf4j
@Service
public class JobService {
	
	@Autowired
	private BigQuery bigQuery;
	
	@Autowired
	private Scheduler scheduler;
	
	@Autowired
	private PubsubOutboundGateway messagingGateway;
	
	private String QUERY_ROW_COUNT_DATA_SET_TABLE = "SELECT row_count FROM bulk_status.__TABLES__ WHERE table_id = @tableId";
	private String QUERY_ROW_COUNT_IDS_DATA_SET_TABLE = "SELECT row_count FROM ids_results.__TABLES__ WHERE table_id = @tableId";
	private String QUERY_COUNT_DATA_SET_TABLE = "SELECT COUNT(1) AS count FROM bulk_status.%s%s";
	private String QUERY_COUNT_IDS_DATA_SET_TABLE = "SELECT COUNT(1) AS count FROM ids_results.%s%s";
	private String TABLE_ID = "results_";
	private String IDS_TABLE_ID = "ids_results_";

	public void execute(String jobId, String idsJobId, int expectedRows, JobKey key) {
		
		try {
			// Is this an idsJob?
			boolean idsJob;

			if (idsJobId != null && idsJobId.length() > 0) {
				idsJob = true;
			} else {
				idsJob = false;
				idsJobId = "";
			}
		
			// Should only have one result
			List<QueryRowCountResult> queryRowCountResultList = runRowCountQuery(jobId, idsJob);
			List<QueryCountResult> queryCountResultList = runCountQuery(jobId, idsJob);
			
			if (queryRowCountResultList.size() > 0 && queryCountResultList.size() > 0 
					&& (queryRowCountResultList.get(0).getRowCount().equals(queryCountResultList.get(0).getCount()))) {
				
				log.debug(String.format("queryRowCountResult: %d", queryRowCountResultList.get(0).getRowCount()));
				log.debug(String.format("queryCountResult: %d", queryCountResultList.get(0).getCount()));
				
				// Data in BigQuery table is exportable
				// Cloud Function will update bulk-status-db to results-ready in required table
				// It will not export an IDS table to GCS.
				// Create new pub sub message 
				// Terminate the job
				String tableId = idsJob ? IDS_TABLE_ID : TABLE_ID;
				
				log.debug(String.format("Table: %s%s is now exportable.", tableId, jobId));
				messagingGateway.sendToPubsub(new ObjectMapper().writeValueAsString(new Exportable(jobId, idsJobId)));
				scheduler.deleteJob(key);
			}
			
		} catch (InterruptedException e) {
			log.error(String.format("Problem querying BigQuery: %s", e.getMessage()));
		} catch (SchedulerException e) {
			log.error(String.format("Problem scheduling: %s", e.getMessage()));
		} catch (JsonProcessingException e) {
			log.error(String.format("Problem creating Exportable object: %s", e.getMessage()));
		}
	}
	
	public boolean deleteJob(String jobName) throws SchedulerException {
		return scheduler.deleteJob(new JobKey(jobName, SCHEDULER_GROUP));
	}
	
	public List<BulkSchedulerJob> getJobs() throws SchedulerException {

		List<BulkSchedulerJob> jobs = new ArrayList<BulkSchedulerJob>();
		
		for(JobKey key : scheduler.getJobKeys(GroupMatcher.groupEquals(SCHEDULER_GROUP))) {			
			JobDetail detail = scheduler.getJobDetail(key);
			List<BulkSchedulerTrigger> triggers = new ArrayList<BulkSchedulerTrigger>();

			for (Trigger trigger : scheduler.getTriggersOfJob(key)) {
				
				LocalDateTime nextFireTime = trigger.getNextFireTime() != null ? 
						trigger.getNextFireTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime() : null;
				LocalDateTime previousFireTime = trigger.getPreviousFireTime() != null ? 
						trigger.getPreviousFireTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime() : null;
				
				triggers.add(new BulkSchedulerTrigger(trigger.getDescription(),	nextFireTime, previousFireTime));
			}
			
			jobs.add(new BulkSchedulerJob(detail.getKey().getName(), 
					detail.getKey().getGroup(), 
					detail.getDescription(), 
					triggers));
		}
		
		return jobs;
	}
	
	private List<QueryRowCountResult> runRowCountQuery(String jobId, boolean idsJob) throws InterruptedException {

		List<QueryRowCountResult> qr = new ArrayList<QueryRowCountResult>();
		
		String datasetTable = idsJob ? QUERY_ROW_COUNT_IDS_DATA_SET_TABLE : QUERY_ROW_COUNT_DATA_SET_TABLE;
		String tableId = idsJob ? IDS_TABLE_ID : TABLE_ID;
		
		QueryJobConfiguration queryConfig = QueryJobConfiguration.newBuilder(datasetTable)
				.addNamedParameter("tableId", QueryParameterValue.string(String.format("%s%s", tableId, jobId))).build();

		TableResult results = bigQuery.query(queryConfig);
		results.iterateAll().forEach(row -> {
			qr.add(new QueryRowCountResult(row.get("row_count").getLongValue()));
		});

		return qr;
	}
	
	private List<QueryCountResult> runCountQuery(String jobId, boolean idsJob) throws InterruptedException {

		List<QueryCountResult> qr = new ArrayList<QueryCountResult>();
		
		String tableId = idsJob ? IDS_TABLE_ID : TABLE_ID;
		String query = idsJob ? String.format(QUERY_COUNT_IDS_DATA_SET_TABLE, tableId, jobId) : String.format(QUERY_COUNT_DATA_SET_TABLE, tableId, jobId);
		
		QueryJobConfiguration queryConfig = QueryJobConfiguration.newBuilder(query).build();

		TableResult results = bigQuery.query(queryConfig);
		results.iterateAll().forEach(row -> {
			qr.add(new QueryCountResult(row.get("count").getLongValue()));
		});

		return qr;
	}
}