package uk.gov.ons.bulk.scheduler.entities;

import lombok.Data;

public @Data class QueryRowCountResult {
	
	private Long rowCount;

	public QueryRowCountResult(Long rowCount) {
		super();
		this.rowCount = rowCount;
	}
}
