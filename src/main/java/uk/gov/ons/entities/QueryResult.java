package uk.gov.ons.entities;

import lombok.Data;

public @Data class QueryResult {
	
	private Long rowCount;

	public QueryResult(Long rowCount) {
		super();
		this.rowCount = rowCount;
	}
}
