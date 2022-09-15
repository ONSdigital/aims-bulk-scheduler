package uk.gov.ons.service;

import java.util.ArrayList;
import java.util.List;

import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryParameterValue;
import com.google.cloud.bigquery.TableResult;

import lombok.extern.slf4j.Slf4j;
import uk.gov.ons.component.PubSubComponent.PubsubOutboundGateway;
import uk.gov.ons.entities.Exportable;
import uk.gov.ons.entities.QueryCountResult;
import uk.gov.ons.entities.QueryRowCountResult;

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
		
		QueryRowCountResult queryRowCountResult = null;
		QueryCountResult queryCountResult = null;
		
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
			queryRowCountResult = runRowCountQuery(jobId, idsJob).get(0);
			queryCountResult = runCountQuery(jobId, idsJob).get(0);
			
			if (queryRowCountResult != null && queryCountResult != null && (queryRowCountResult.getRowCount() == queryCountResult.getCount())) {
				
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
