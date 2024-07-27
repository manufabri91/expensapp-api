package com.manuelfabri.expenses.dto;

import java.math.BigDecimal;
import com.manuelfabri.expenses.model.CurrencyEnum;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AccountDto {

  @NotNull
  private long id;
  @NotBlank(message = "Name cannot be blank or null")
  private String name;
  @NotNull
  private CurrencyEnum currency;
  @NotNull
  private BigDecimal accountBalance;
}
