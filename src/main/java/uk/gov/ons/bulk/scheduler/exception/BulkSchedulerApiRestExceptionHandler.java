package uk.gov.ons.bulk.scheduler.exception;

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;

/**
 * Custom Exception Handler for REST argument and constraint violations.
 * Any violations result in a BAD_REQUEST being returned to the user with the 
 * following format:
 * 
 * {
 *   "status": "BAD_REQUEST",
 *   "message": "runBulkRequest.excludenorthernireland: excludenorthernireland must be true or false",
 *   "errors": [
 *     "uk.gov.ons.bulk.controllers.BulkAddressController runBulkRequest.excludenorthernireland: excludenorthernireland must be true or false"
 *   ]
 * }
 * 
 * Violations are also logged
 *
 */
@Slf4j
@ControllerAdvice
public class BulkSchedulerApiRestExceptionHandler extends ResponseEntityExceptionHandler {

	@ExceptionHandler({ ConstraintViolationException.class })
	public ResponseEntity<Object> handleConstraintViolation(ConstraintViolationException ex, WebRequest request) {
		
		List<String> errors = new ArrayList<String>();

		ex.getConstraintViolations().forEach(violation -> {	
			errors.add(String.format("%s %s: %s", violation.getRootBeanClass().getName(), 
					violation.getPropertyPath(), violation.getMessage()));
		});

		BulkSchedulerApiError apiError = new BulkSchedulerApiError(HttpStatus.BAD_REQUEST, ex.getLocalizedMessage(), errors);
		
		// Log the parameter validation errors - may be useful
		log.error(apiError.toString());
		
		return new ResponseEntity<Object>(apiError, new HttpHeaders(), apiError.getStatus());
	}
}
