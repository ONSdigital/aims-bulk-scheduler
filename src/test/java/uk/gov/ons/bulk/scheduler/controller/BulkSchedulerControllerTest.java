package uk.gov.ons.bulk.scheduler.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.hamcrest.core.Is;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import uk.gov.ons.bulk.scheduler.entities.BulkSchedulerJob;
import uk.gov.ons.bulk.scheduler.entities.BulkSchedulerTrigger;
import uk.gov.ons.bulk.scheduler.service.JobService;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(SpringExtension.class)
@TestInstance(Lifecycle.PER_CLASS)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext
class BulkSchedulerControllerTest {
	
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private JobService jobService;
    
    private LocalDateTime now;
    private LocalDateTime then;

    @BeforeAll
    public void setUp() throws Exception {

       now = LocalDateTime.now();  
       then = LocalDateTime.now().minusMinutes(5);
    }
    
    
	@Test
	public void testSwagger() throws Exception {

		mockMvc.perform(MockMvcRequestBuilders.get("/v3/api-docs")
						.contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(jsonPath("$.openapi", Is.is("3.0.1")))
				.andExpect(content().contentType(MediaType.APPLICATION_JSON));
	}
	
	@Test
	public void jobsRequest() throws Exception {
		
		BulkSchedulerTrigger bst = new BulkSchedulerTrigger("Trigger description", now, then);
		List<BulkSchedulerTrigger> bstList = List.of(bst);
		
		BulkSchedulerJob bsj1 = new BulkSchedulerJob("Job_1", "A group", "This is Job_1", bstList);
		BulkSchedulerJob bsj2 = new BulkSchedulerJob("Job_2", "A group", "This is Job_2", bstList);
		
		List<BulkSchedulerJob> bsjList = List.of(bsj1, bsj2);

		when(jobService.getJobs()).thenReturn(bsjList);
		
		mockMvc.perform(MockMvcRequestBuilders.get("/jobs")
				.contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(jsonPath("$.jobs").isArray()).andExpect(jsonPath("$.jobs", hasSize(2)))
				.andExpect(jsonPath("$.jobs[0].name", Is.is(bsj1.getName())))
				.andExpect(jsonPath("$.jobs[0].group", Is.is(bsj1.getGroup())))
				.andExpect(jsonPath("$.jobs[0].description", Is.is(bsj1.getDescription())))
				.andExpect(jsonPath("$.jobs[0].triggers").isArray()).andExpect(jsonPath("$.jobs[0].triggers", hasSize(1)))
				.andExpect(jsonPath("$.jobs[0].triggers[0].description", Is.is(bst.getDescription())))
				.andExpect(jsonPath("$.jobs[0].triggers[0].next_fire_time", Is.is(now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))))
				.andExpect(jsonPath("$.jobs[0].triggers[0].previous_fire_time", Is.is(then.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))))
				.andExpect(content().contentType(MediaType.APPLICATION_JSON));
	}
	
	@Test
	public void jobsRequestException() throws Exception {
		
		when(jobService.getJobs()).thenThrow(new SchedulerException("A Scheduler Exception"));
		
		mockMvc.perform(MockMvcRequestBuilders.get("/jobs")
				.contentType(MediaType.APPLICATION_JSON)).andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.error", containsString("A Scheduler Exception")))
				.andExpect(content().contentType(MediaType.APPLICATION_JSON));
	}

	@Test
	public void deleteJobRequestException() throws Exception {
		
		when(jobService.deleteJob(Mockito.any(String.class))).thenThrow(new SchedulerException("A Scheduler Exception"));
		
		mockMvc.perform(MockMvcRequestBuilders.delete("/job/my-job")
				.contentType(MediaType.APPLICATION_JSON)).andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.error", containsString("A Scheduler Exception")))
				.andExpect(content().contentType(MediaType.APPLICATION_JSON));
	}
	
	@Test
	public void deleteJobRequestNoJobName() throws Exception {
		
		when(jobService.deleteJob(Mockito.any(String.class))).thenThrow(new SchedulerException("A Scheduler Exception"));
		
		mockMvc.perform(MockMvcRequestBuilders.delete("/job/ ")
				.contentType(MediaType.APPLICATION_JSON)).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status", Is.is("BAD_REQUEST")))
				.andExpect(jsonPath("$.message", containsString("jobName: jobname is mandatory")))
				.andExpect(jsonPath("$.errors").isArray()).andExpect(jsonPath("$.errors", hasSize(1)))
				.andExpect(jsonPath("$.errors", hasItem(containsString("jobName: jobname is mandatory"))))		
				.andExpect(content().contentType(MediaType.APPLICATION_JSON));
	}
	
	@Test
	public void deleteJobRequestGoodJobName() throws Exception {
		
		when(jobService.deleteJob(Mockito.any(String.class))).thenReturn(true);
		
		mockMvc.perform(MockMvcRequestBuilders.delete("/job/my-job")
				.contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(jsonPath("success", Is.is("Scheduled Job my-job removed from the system")))
				.andExpect(content().contentType(MediaType.APPLICATION_JSON));
	}
	
	@Test
	public void deleteJobRequestBadJobName() throws Exception {
		
		when(jobService.deleteJob(Mockito.any(String.class))).thenReturn(false);
		
		mockMvc.perform(MockMvcRequestBuilders.delete("/job/my-job")
				.contentType(MediaType.APPLICATION_JSON)).andExpect(status().isBadRequest())
				.andExpect(jsonPath("error", Is.is("Scheduled Job my-job not found on the system")))
				.andExpect(content().contentType(MediaType.APPLICATION_JSON));
	}
}
