package com.manuelfabri.expenses.constants;

public final class Urls {

  private Urls() {
    // No need to instantiate the class, we can hide its constructor
  }

  public static final String ACCOUNT = "/account";
  public static final String AUTH = "/auth";
  public static final String TRANSACTION = "/transaction";
  public static final String CATEGORY = "/category";
  public static final String SUBCATEGORY = "/subcategory";
  public static final String SUMMARY = "/summary";
  public static final String RECURRENT_TRANSACTION = "/recurrent-transaction";
  public static final String USER_SETTINGS = "/user-settings";

  // springdoc-openapi's default paths
  public static final String API_DOCS = "/v3/api-docs";
  public static final String SWAGGER_UI = "/swagger-ui";
}
