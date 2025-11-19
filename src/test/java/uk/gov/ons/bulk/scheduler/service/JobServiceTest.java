package uk.gov.ons.bulk.scheduler.service;

import com.google.cloud.bigquery.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.Trigger;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.ons.bulk.scheduler.component.PubSubComponent.PubsubOutboundGateway;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class JobServiceTest {

    @InjectMocks
    private JobService jobService;

    @Mock
    private BigQuery bigQuery;

    @Mock
    private Scheduler scheduler;

    @Mock
    private PubsubOutboundGateway messagingGateway;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        ReflectionTestUtils.setField(jobService, "TABLE_ID", "results_");
        ReflectionTestUtils.setField(jobService, "IDS_TABLE_ID", "ids_results_");
    }

    @Test
    void testExecute_SuccessfulExportable() throws Exception {
        String jobId = "123";
        String idsJobId = "";
        int expectedRows = 1;
        JobKey key = new JobKey("job", "group");

        TableResult rowCountResult = mock(TableResult.class);
        TableResult countResult = mock(TableResult.class);

        FieldValueList rowCountList = mock(FieldValueList.class);
        when(rowCountList.get("row_count")).thenReturn(FieldValue.of(FieldValue.Attribute.PRIMITIVE, "1"));
        when(rowCountResult.iterateAll()).thenReturn(List.of(rowCountList));

        FieldValueList countList = mock(FieldValueList.class);
        when(countList.get("count")).thenReturn(FieldValue.of(FieldValue.Attribute.PRIMITIVE, "1"));
        when(countResult.iterateAll()).thenReturn(List.of(countList));

        when(bigQuery.query(any(QueryJobConfiguration.class)))
                .thenReturn(rowCountResult)
                .thenReturn(countResult);

        jobService.execute(jobId, idsJobId, expectedRows, key);

        verify(messagingGateway, times(1)).sendToPubsub(any(String.class));
        verify(scheduler, times(1)).deleteJob(key);
    }

    @Test
    void testDeleteJob() throws Exception {
        when(scheduler.deleteJob(any(JobKey.class))).thenReturn(true);
        boolean result = jobService.deleteJob("testJob");
        assert(result);
    }

    @Test
    void testGetJobs() throws Exception {
        JobKey jobKey = new JobKey("job1", "SCHEDULER_GROUP");
        when(scheduler.getJobKeys(any())).thenReturn(Collections.singleton(jobKey));
        JobDetail jobDetail = mock(JobDetail.class);
        when(jobDetail.getKey()).thenReturn(jobKey);
        when(jobDetail.getDescription()).thenReturn("desc");
        when(scheduler.getJobDetail(jobKey)).thenReturn(jobDetail);
        Trigger trigger = mock(Trigger.class);
        // Explicit cast to match method signature if needed
        when(scheduler.getTriggersOfJob(jobKey)).thenReturn((List) List.of(trigger));
        when(trigger.getDescription()).thenReturn("triggerDesc");
        when(trigger.getNextFireTime()).thenReturn(null);
        when(trigger.getPreviousFireTime()).thenReturn(null);

        var jobs = jobService.getJobs();
        assert(jobs.size() == 1);
    }

    @Test
    void testExecute_WhenRowCountAndCountDoNotMatch_ShouldNotSendMessageOrDeleteJob() throws Exception {
        String jobId = "123";
        String idsJobId = "";
        int expectedRows = 1;
        JobKey key = new JobKey("job", "group");

        TableResult rowCountResult = mock(TableResult.class);
        TableResult countResult = mock(TableResult.class);

        FieldValueList rowCountList = mock(FieldValueList.class);
        when(rowCountList.get("row_count")).thenReturn(FieldValue.of(FieldValue.Attribute.PRIMITIVE, "2"));
        when(rowCountResult.iterateAll()).thenReturn(List.of(rowCountList));

        FieldValueList countList = mock(FieldValueList.class);
        when(countList.get("count")).thenReturn(FieldValue.of(FieldValue.Attribute.PRIMITIVE, "1"));
        when(countResult.iterateAll()).thenReturn(List.of(countList));

        when(bigQuery.query(any(QueryJobConfiguration.class)))
                .thenReturn(rowCountResult)
                .thenReturn(countResult);

        jobService.execute(jobId, idsJobId, expectedRows, key);

        verify(messagingGateway, never()).sendToPubsub(any(String.class));
        verify(scheduler, never()).deleteJob(key);
    }

    @Test
    void testExecute_WhenBigQueryThrowsInterruptedException_ShouldLogError() throws Exception {
        String jobId = "123";
        String idsJobId = "";
        int expectedRows = 1;
        JobKey key = new JobKey("job", "group");

        when(bigQuery.query(any(QueryJobConfiguration.class))).thenThrow(new InterruptedException("interrupted"));

        jobService.execute(jobId, idsJobId, expectedRows, key);

        verify(messagingGateway, never()).sendToPubsub(any(String.class));
        verify(scheduler, never()).deleteJob(key);
    }

    @Test
    void testExecute_WhenSchedulerThrowsSchedulerException_ShouldLogError() throws Exception {
        String jobId = "123";
        String idsJobId = "";
        int expectedRows = 1;
        JobKey key = new JobKey("job", "group");

        TableResult rowCountResult = mock(TableResult.class);
        TableResult countResult = mock(TableResult.class);

        FieldValueList rowCountList = mock(FieldValueList.class);
        when(rowCountList.get("row_count")).thenReturn(FieldValue.of(FieldValue.Attribute.PRIMITIVE, "1"));
        when(rowCountResult.iterateAll()).thenReturn(List.of(rowCountList));

        FieldValueList countList = mock(FieldValueList.class);
        when(countList.get("count")).thenReturn(FieldValue.of(FieldValue.Attribute.PRIMITIVE, "1"));
        when(countResult.iterateAll()).thenReturn(List.of(countList));

        when(bigQuery.query(any(QueryJobConfiguration.class)))
                .thenReturn(rowCountResult)
                .thenReturn(countResult);

        doThrow(new org.quartz.SchedulerException("scheduler error")).when(scheduler).deleteJob(key);

        jobService.execute(jobId, idsJobId, expectedRows, key);

        verify(messagingGateway, times(1)).sendToPubsub(any(String.class));
        verify(scheduler, times(1)).deleteJob(key);
    }

    @Test
    void testExecute_WithIdsJob_ShouldSendMessageAndDeleteJob() throws Exception {
        String jobId = "123";
        String idsJobId = "ids456";
        int expectedRows = 1;
        JobKey key = new JobKey("job", "group");

        TableResult rowCountResult = mock(TableResult.class);
        TableResult countResult = mock(TableResult.class);

        FieldValueList rowCountList = mock(FieldValueList.class);
        when(rowCountList.get("row_count")).thenReturn(FieldValue.of(FieldValue.Attribute.PRIMITIVE, "1"));
        when(rowCountResult.iterateAll()).thenReturn(List.of(rowCountList));

        FieldValueList countList = mock(FieldValueList.class);
        when(countList.get("count")).thenReturn(FieldValue.of(FieldValue.Attribute.PRIMITIVE, "1"));
        when(countResult.iterateAll()).thenReturn(List.of(countList));

        when(bigQuery.query(any(QueryJobConfiguration.class)))
                .thenReturn(rowCountResult)
                .thenReturn(countResult);

        jobService.execute(jobId, idsJobId, expectedRows, key);

        verify(messagingGateway, times(1)).sendToPubsub(any(String.class));
        verify(scheduler, times(1)).deleteJob(key);
    }
}