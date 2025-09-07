package com.manuelfabri.specification;

import com.manuelfabri.expenses.model.Transaction;
import com.manuelfabri.expenses.model.TransactionTypeEnum;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public class TransactionSpecifications {

  public static Specification<Transaction> hasType(TransactionTypeEnum type) {
    return (root, query, cb) -> type == null ? null : cb.equal(root.get("type"), type);
  }

  public static Specification<Transaction> hasCategoryId(Long categoryId) {
    return (root, query, cb) -> categoryId == null ? null : cb.equal(root.get("category").get("id"), categoryId);
  }

  public static Specification<Transaction> hasMinAmount(BigDecimal minAmount) {
    return (root, query, cb) -> minAmount == null ? null : cb.greaterThanOrEqualTo(root.get("amount"), minAmount);
  }

  public static Specification<Transaction> hasMaxAmount(BigDecimal maxAmount) {
    return (root, query, cb) -> maxAmount == null ? null : cb.lessThanOrEqualTo(root.get("amount"), maxAmount);
  }

  public static Specification<Transaction> occurredAfter(OffsetDateTime from) {
    return (root, query, cb) -> from == null ? null : cb.greaterThanOrEqualTo(root.get("eventDate"), from);
  }

  public static Specification<Transaction> occurredBefore(OffsetDateTime to) {
    return (root, query, cb) -> to == null ? null : cb.lessThanOrEqualTo(root.get("eventDate"), to);
  }
}
