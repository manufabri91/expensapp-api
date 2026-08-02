package com.manuelfabri.expenses.model;

import java.math.BigDecimal;

/**
 * The {@code TransactionTypeEnum} handles constants for the types of transactions.
 */
public enum TransactionTypeEnum {
  INCOME, EXPENSE, TRANSFER;

  /**
   * Applies this type's sign convention to a positive magnitude: negative for EXPENSE/TRANSFER, positive for INCOME.
   */
  public BigDecimal applySign(BigDecimal amount) {
    BigDecimal absolute = amount.abs();
    return (this == EXPENSE || this == TRANSFER) ? absolute.negate() : absolute;
  }
}
