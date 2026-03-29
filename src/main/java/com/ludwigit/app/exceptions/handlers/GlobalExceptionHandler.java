package com.ludwigit.app.exceptions.handlers;

import com.ludwigit.app.exceptions.AppException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(AppException.class)
	public ResponseEntity<Object> handleAppExceptions(AppException exception) {
		Map<String, Object> body = new LinkedHashMap<>();

		body.put("message", exception.getMessage());
		body.put("code", exception.code);
		body.put("timestamp", exception.timestamp);

		return ResponseEntity.status(exception.code).body(body);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<Object> handleRequestParamsNotMetException(MethodArgumentNotValidException exception) {
		Map<String, Object> body = new LinkedHashMap<>();
		Map<String, String> fieldErrors = new LinkedHashMap<>();

		exception.getBindingResult().getFieldErrors().forEach(error -> {
			fieldErrors.put(error.getField(), error.getDefaultMessage());
		});
		body.put("message", "Body parameters did not meet the validation requirements. Please check the errors and try again.");
		body.put("code", HttpStatus.BAD_REQUEST);
		body.put("timestamp", LocalDateTime.now());
		body.put("errors", fieldErrors);

		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
	}

	@ExceptionHandler(NoResourceFoundException.class)
	public ResponseEntity<Object> handleNoResourceFoundException(NoResourceFoundException exception) {
		Map<String, Object> body = new LinkedHashMap<>();

		body.put("message", "The requested resource was not found. Please check the URL and try again.");
		body.put("code", HttpStatus.NOT_FOUND);
		body.put("timestamp", LocalDateTime.now());

		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<Object> handleGlobalException(Exception exception) {
		System.out.println(exception);

		Map<String, Object> body = new LinkedHashMap<>();

		body.put("message", "An unexpected error occurred. Please try again later.");
		body.put("code", HttpStatus.INTERNAL_SERVER_ERROR);
		body.put("timestamp", LocalDateTime.now());

		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
	}

}
