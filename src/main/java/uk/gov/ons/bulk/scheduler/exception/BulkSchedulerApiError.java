package uk.gov.ons.bulk.scheduler.exception;

import java.util.Arrays;
import java.util.List;

import org.springframework.http.HttpStatus;

import lombok.Data;
import lombok.ToString;

@ToString
public @Data class BulkSchedulerApiError {

    private HttpStatus status;
    private String message;
    private List<String> errors;

    public BulkSchedulerApiError(HttpStatus status, String message, List<String> errors) {
        super();
        this.status = status;
        this.message = message;
        this.errors = errors;
    }

    public BulkSchedulerApiError(HttpStatus status, String message, String error) {
        super();
        this.status = status;
        this.message = message;
        errors = Arrays.asList(error);
    }
}