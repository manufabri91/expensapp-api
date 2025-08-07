package com.manuelfabri.expenses;

import org.modelmapper.ModelMapper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.data.repository.query.SecurityEvaluationContextExtension;
import com.manuelfabri.expenses.dto.LinkedTransactionDTO;
import com.manuelfabri.expenses.model.Transaction;

@SpringBootApplication
public class ExpensesApplication {

  @Bean
  public ModelMapper modelMapper() {
    ModelMapper modelMapper = new ModelMapper();
    modelMapper.getConfiguration().setFieldMatchingEnabled(true)
        .setFieldAccessLevel(org.modelmapper.config.Configuration.AccessLevel.PRIVATE).setSkipNullEnabled(true);

    modelMapper.addConverter(src -> {
      Transaction tx = src.getSource();
      if (tx == null || tx.getLinkedTransaction() == null) {
        return null;
      }
      LinkedTransactionDTO dto = new LinkedTransactionDTO();
      dto.setId(tx.getId());
      dto.setAccountId(tx.getAccount().getId());
      dto.setAccountName(tx.getAccount().getName());
      return dto;
    }, Transaction.class, LinkedTransactionDTO.class);

    return modelMapper;
  }

  @Bean
  public SecurityEvaluationContextExtension securityEvaluationContextExtension() {
    return new SecurityEvaluationContextExtension();
  }

  public static void main(String[] args) {
    SpringApplication.run(ExpensesApplication.class, args);
  }
}
