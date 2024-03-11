package uk.gov.ons.bulk.scheduler.controller;

import java.util.List;

import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.constraints.NotBlank;
import lombok.extern.slf4j.Slf4j;
import uk.gov.ons.bulk.scheduler.entities.BulkSchedulerJob;
import uk.gov.ons.bulk.scheduler.service.JobService;

@Slf4j
@Validated
@RestController
public class BulkSchedulerController {
	
	@Autowired
	private JobService jobService;
	
	@Operation(summary = "Get a list of scheduled jobs on the system")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Scheduled Jobs list returned OK (can be empty)",
					content = { @Content(mediaType = "application/json",
							schema = @Schema(implementation = BulkSchedulerJob.class)) }),
			@ApiResponse(responseCode = "500", description = "Problem with Scheduler",
					content = @Content) })
	@GetMapping(value = "/jobs", produces = "application/json")
	public ResponseEntity<String> getScheduledJobs() {

		List<BulkSchedulerJob> jobs;
		try {
			jobs = jobService.getJobs();
			
			ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule())
					.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
					.setSerializationInclusion(Include.NON_NULL);
			
			return ResponseEntity.ok(objectMapper.createObjectNode().set("jobs", objectMapper.valueToTree(jobs)).toString());
		} catch (SchedulerException e) {
			String response = String.format("/jobs error: %s", e.getMessage());
			log.error(response);
			return ResponseEntity.internalServerError()
					.body(new ObjectMapper().createObjectNode().put("error", response).toString());
		}
	}
	
	@Operation(summary = "Delete a Scheduled Job")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Scheduled Job successfully removed from system",
					content = @Content),
			@ApiResponse(responseCode = "400", description = "Scheduled Job not found on the system",
					content = @Content),
			@ApiResponse(responseCode = "500", description = "Problem with Scheduler",
					content = @Content) })
	@DeleteMapping(value = "/job/{jobname}", produces = "application/json")
	public ResponseEntity<String> deleteScheduledJob(
			@PathVariable(required = true, name = "jobname") @NotBlank(message = "{jobname.val.message}") String jobName) {
		
		try {		
			boolean deleteResult = jobService.deleteJob(jobName);
			
			if (deleteResult) {
				String happyResponse = String.format("Scheduled Job %s removed from the system", jobName);
				log.info(happyResponse);
				return ResponseEntity.ok()
						.body(new ObjectMapper().createObjectNode().put("success", happyResponse).toString());

			} else {
				String unhappyResponse = String.format("Scheduled Job %s not found on the system", jobName);
				log.info(unhappyResponse);
				return ResponseEntity.badRequest()
						.body(new ObjectMapper().createObjectNode().put("error", unhappyResponse).toString());
			}
		} catch (SchedulerException e) {
			String response = String.format("/job/%s error: %s", jobName, e.getMessage());
			log.error(response);
			return ResponseEntity.internalServerError()
					.body(new ObjectMapper().createObjectNode().put("error", response).toString());
		}
	}
}
