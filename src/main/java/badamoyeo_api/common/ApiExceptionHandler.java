package badamoyeo_api.common;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiExceptionHandler {
	private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);


	@ExceptionHandler(ResponseStatusException.class)
	public ResponseEntity<Map<String, String>> handleResponseStatus(ResponseStatusException exception) {
		String message = exception.getReason() == null ? "request failed" : exception.getReason();
		return ResponseEntity.status(exception.getStatusCode()).body(Map.of("message", message));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException exception) {
		String message = exception.getBindingResult().getFieldErrors().stream()
			.findFirst()
			.map(error -> error.getField() + ": " + error.getDefaultMessage())
			.orElse("invalid request");
		return ResponseEntity.badRequest().body(Map.of("message", message));
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<Map<String, String>> handleUnreadableMessage(HttpMessageNotReadableException exception) {
		return ResponseEntity.badRequest().body(Map.of("message", "invalid request body"));
	}

	@ExceptionHandler(MultipartException.class)
	public ResponseEntity<Map<String, String>> handleMultipart(MultipartException exception) {
		return ResponseEntity.badRequest().body(Map.of("message", "invalid multipart request"));
	}

	@ExceptionHandler(MaxUploadSizeExceededException.class)
	public ResponseEntity<Map<String, String>> handleMaxUploadSize(MaxUploadSizeExceededException exception) {
		return ResponseEntity.badRequest().body(Map.of("message", "image file is too large"));
	}

	@ExceptionHandler(NoResourceFoundException.class)
	public ResponseEntity<Map<String, String>> handleNoResourceFound(NoResourceFoundException exception) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "not found"));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<Map<String, String>> handleException(Exception exception) {
		log.error("Unhandled exception", exception);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
			.body(Map.of("message", "internal server error"));
	}
}
