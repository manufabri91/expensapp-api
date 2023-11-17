package com.manuelfabri.expenses;

import org.modelmapper.ModelMapper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.data.repository.query.SecurityEvaluationContextExtension;

@SpringBootApplication
public class ExpensesApplication {

  @Bean
  ModelMapper modelMapper() {
    return new ModelMapper();
  }

  @Bean
  public SecurityEvaluationContextExtension securityEvaluationContextExtension() {
    return new SecurityEvaluationContextExtension();
  }

  public static void main(String[] args) {
    SpringApplication.run(ExpensesApplication.class, args);
  }

}
