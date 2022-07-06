package uk.gov.ons.service;

import java.util.ArrayList;
import java.util.List;

import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryParameterValue;
import com.google.cloud.bigquery.TableResult;

import lombok.extern.slf4j.Slf4j;
import uk.gov.ons.component.PubSubComponent.PubsubOutboundGateway;
import uk.gov.ons.entities.QueryResult;

@Slf4j
@Service
public class JobService {
	
	@Autowired
	private BigQuery bigQuery;
	
	@Autowired
	private Scheduler scheduler;
	
	@Autowired
	private PubsubOutboundGateway messagingGateway;
	
	private String QUERY_DATA_SET_TABLE = "SELECT row_count FROM bulk_status.__TABLES__ WHERE table_id = @tableId";

	public void execute(String jobId, int expectedRows, JobKey key) {
		
		QueryResult queryResult = null;
		
		try {
			// Should only have one result
			queryResult = runQuery(jobId).get(0);		
			
			if (queryResult != null && (queryResult.getRowCount() == expectedRows)) {
				// Data in BigQuery table is exportable 
				// Create new pub sub message 
				// Terminate the job
				log.debug(String.format("Table: %s is now exportable. Job: ", jobId));
				messagingGateway.sendToPubsub(new ObjectMapper().createObjectNode().put("jobId", jobId).toString());
				scheduler.deleteJob(key);
			}
			
		} catch (InterruptedException e) {
			log.error(String.format("Problem querying BigQuery: %s", e.getMessage()));
		} catch (SchedulerException e) {
			log.error(String.format("Problem scheduling: %s", e.getMessage()));
		}
	}
	
	private List<QueryResult> runQuery(String jobId) throws InterruptedException {

		List<QueryResult> qr = new ArrayList<QueryResult>();

		QueryJobConfiguration queryConfig = QueryJobConfiguration.newBuilder(QUERY_DATA_SET_TABLE)
				.addNamedParameter("tableId", QueryParameterValue.string(String.format("results_", jobId))).build();

		TableResult results = bigQuery.query(queryConfig);
		results.iterateAll().forEach(row -> {
			qr.add(new QueryResult(row.get("row_count").getLongValue()));
		});

		return qr;
	}
}
