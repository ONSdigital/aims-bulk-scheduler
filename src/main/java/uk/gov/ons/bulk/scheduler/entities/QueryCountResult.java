package uk.gov.ons.bulk.scheduler.entities;

import lombok.Data;

public @Data class QueryCountResult {
	
	private Long count;

	public QueryCountResult(Long count) {
		super();
		this.count = count;
	}
}