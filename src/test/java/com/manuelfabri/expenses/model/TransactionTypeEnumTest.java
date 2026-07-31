package com.manuelfabri.expenses.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class TransactionTypeEnumTest {

  @Test
  void applySign_returnsPositiveAmount_forIncome() {
    BigDecimal rawAmount = new BigDecimal("150.00");

    BigDecimal signedAmount = TransactionTypeEnum.INCOME.applySign(rawAmount);

    assertThat(signedAmount).isEqualByComparingTo("150.00");
  }

  @Test
  void applySign_returnsNegativeAmount_forExpense() {
    BigDecimal rawAmount = new BigDecimal("150.00");

    BigDecimal signedAmount = TransactionTypeEnum.EXPENSE.applySign(rawAmount);

    assertThat(signedAmount).isEqualByComparingTo("-150.00");
  }

  @Test
  void applySign_returnsNegativeAmount_forTransfer() {
    BigDecimal rawAmount = new BigDecimal("150.00");

    BigDecimal signedAmount = TransactionTypeEnum.TRANSFER.applySign(rawAmount);

    assertThat(signedAmount).isEqualByComparingTo("-150.00");
  }

  @Test
  void applySign_alwaysNormalizesToPositiveMagnitudeFirst_regardlessOfInputSign() {
    BigDecimal alreadyNegativeAmount = new BigDecimal("-75.50");

    BigDecimal incomeSignedAmount = TransactionTypeEnum.INCOME.applySign(alreadyNegativeAmount);
    BigDecimal expenseSignedAmount = TransactionTypeEnum.EXPENSE.applySign(alreadyNegativeAmount);

    assertThat(incomeSignedAmount).isEqualByComparingTo("75.50");
    assertThat(expenseSignedAmount).isEqualByComparingTo("-75.50");
  }
}
