package com.manuelfabri.expenses.exception;

public class InvalidLoginException extends RuntimeException {
  private static final long serialVersionUID = -2207939911182264685L;

  public InvalidLoginException() {
    super("Invalid username or password");
  }
}
