package uk.gov.ons.bulk.scheduler.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@JsonIgnoreProperties(ignoreUnknown = true)
public @Data class Payload {
	
	private String jobId;
	private String idsJobId;
	private int expectedRows;
}
