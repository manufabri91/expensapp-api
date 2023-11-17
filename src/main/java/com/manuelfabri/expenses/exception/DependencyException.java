package com.manuelfabri.expenses.exception;

public class DependencyException extends RuntimeException {

  private static final long serialVersionUID = -8434239951000337756L;

  public DependencyException() {
    super("Failed to execute dependency");
  }

  public DependencyException(String dependencyName) {
    super(String.format("Failed to execute dependency: %s", dependencyName));
  }

  public DependencyException(String dependencyName, String description) {
    super(String.format("Failed to execute dependency: %s. Error message: %s", dependencyName, description));
  }
}
