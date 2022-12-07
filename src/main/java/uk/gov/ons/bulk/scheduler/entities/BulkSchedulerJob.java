package uk.gov.ons.bulk.scheduler.entities;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
public @Data class BulkSchedulerJob {
	
	private String name;
	private String group;
	private String description;
	private List<BulkSchedulerTrigger> triggers;
}
