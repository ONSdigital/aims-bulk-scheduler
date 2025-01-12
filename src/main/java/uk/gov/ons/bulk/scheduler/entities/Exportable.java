package uk.gov.ons.bulk.scheduler.entities;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;

@AllArgsConstructor
public @Data class Exportable {
	@NonNull
	private String jobId;
	@NonNull
	private String idsJobId;
}
