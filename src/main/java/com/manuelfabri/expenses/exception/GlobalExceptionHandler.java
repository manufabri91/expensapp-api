package com.manuelfabri.expenses.exception;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import com.manuelfabri.expenses.dto.ErrorResponseDto;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(ResourceNotFoundException.class)
  @ResponseStatus(value = HttpStatus.NOT_FOUND)
  public ResponseEntity<Object> handleResourceNotFound(ResourceNotFoundException ex, WebRequest request) {
    return buildErrorResponse(ex, HttpStatus.NOT_FOUND, request);
  }

  @ExceptionHandler(DependencyException.class)
  @ResponseStatus(value = HttpStatus.FAILED_DEPENDENCY)
  public ResponseEntity<Object> handleFailedDependencyException(DependencyException ex, WebRequest request) {
    return buildErrorResponse(ex, HttpStatus.FAILED_DEPENDENCY, request);
  }

  @ExceptionHandler(InvalidLoginException.class)
  @ResponseStatus(value = HttpStatus.UNAUTHORIZED)
  public ResponseEntity<Object> handleInvalidLoginException(InvalidLoginException ex, WebRequest request) {
    return buildErrorResponse(ex, HttpStatus.UNAUTHORIZED, request);
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  @ResponseStatus(value = HttpStatus.BAD_REQUEST)
  public ResponseEntity<Object> handleDataIntegrityViolationExceptionn(DataIntegrityViolationException ex,
      WebRequest request) {
    return buildErrorResponse(ex, "DATA_INTEGRITY", HttpStatus.BAD_REQUEST, request, null);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  @ResponseStatus(value = HttpStatus.BAD_REQUEST)
  public ResponseEntity<Object> handleIllegalArgumentException(IllegalArgumentException ex, WebRequest request) {
    return buildErrorResponse(ex, HttpStatus.BAD_REQUEST, request);
  }

  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers,
      HttpStatusCode status, WebRequest request) {
    return buildErrorResponse(ex, "Validation error. Check 'errors' field for details.",
        HttpStatus.UNPROCESSABLE_ENTITY, request, ex.getBindingResult().getFieldErrors());
  }

  /**
   * Catch-all for any exception type not handled above. Without this, an unmapped exception falls
   * through to Spring's default BasicErrorController - a different response shape than every other
   * error in this API, and never logged.
   */
  @ExceptionHandler(Exception.class)
  @ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
  public ResponseEntity<Object> handleUnexpectedException(Exception ex, WebRequest request) {
    LOGGER.error("Unhandled exception", ex);
    return buildErrorResponse(ex, "An unexpected error occurred.", HttpStatus.INTERNAL_SERVER_ERROR, request, null);
  }

  /**
   * Create ResponseEntity ErrorResponseDto from a given exception. Overloaded to use the exception message.
   */
  private ResponseEntity<Object> buildErrorResponse(Exception exception, HttpStatus httpStatus, WebRequest request) {
    return buildErrorResponse(exception, exception.getMessage(), httpStatus, request, null);
  }

  /**
   * Create ResponseEntity ErrorResponseDto from a given exception.
   */
  private ResponseEntity<Object> buildErrorResponse(Exception exception, String message, HttpStatus httpStatus,
      WebRequest request, List<FieldError> fieldErrors) {
    ErrorResponseDto errorResponse = new ErrorResponseDto(message);

    if (fieldErrors != null && !fieldErrors.isEmpty()) {
      for (FieldError fieldError : fieldErrors) {
        errorResponse.addValidationError(fieldError.getField(), fieldError.getDefaultMessage());
      }
    }

    return new ResponseEntity<Object>(errorResponse, httpStatus);
  }


}
