package com.manuelfabri.expenses.exception;

public class DuplicateEmailException extends RuntimeException {
  public DuplicateEmailException() {
    super("USER_EXISTS");
  }
}
