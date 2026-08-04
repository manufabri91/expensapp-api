package com.manuelfabri.expenses.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.WebRequest;
import com.manuelfabri.expenses.dto.ErrorResponseDto;

class GlobalExceptionHandlerTest {

  private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

  @Test
  void handleUnexpectedException_returnsInternalServerErrorWithGenericMessage() {
    ResponseEntity<Object> response =
        handler.handleUnexpectedException(new IllegalStateException("boom"), mock(WebRequest.class));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    assertThat(((ErrorResponseDto) response.getBody()).getMessage()).isEqualTo("An unexpected error occurred.");
  }
}
