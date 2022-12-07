package uk.gov.ons.bulk.scheduler.entities;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonDeserialize(using = LocalDateDeserializer.class)
@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
@AllArgsConstructor
@NoArgsConstructor
public @Data class BulkSchedulerTrigger {
	private String description;
	@JsonProperty("next_fire_time")
	private LocalDateTime nextFireTime; 
	@JsonProperty("previous_fire_time")
	private LocalDateTime previousFireTime; 
}
