package com.manuelfabri.specification;

import com.manuelfabri.expenses.model.Transaction;
import com.manuelfabri.expenses.model.TransactionTypeEnum;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public class TransactionSpecifications {

  public static Specification<Transaction> hasType(TransactionTypeEnum type) {
    return (root, query, cb) -> type == null ? null : cb.equal(root.get("type"), type);
  }

  public static Specification<Transaction> hasCategoryIds(List<Long> categoryIds) {
    return (root, query, cb) -> (categoryIds == null || categoryIds.isEmpty()) ? null
        : root.get("category").get("id").in(categoryIds);
  }

  public static Specification<Transaction> hasSubcategoryIds(List<Long> subcategoryIds) {
    return (root, query, cb) -> (subcategoryIds == null || subcategoryIds.isEmpty()) ? null
        : root.get("subcategory").get("id").in(subcategoryIds);
  }

  public static Specification<Transaction> hasAccountIds(List<Long> accountIds) {
    return (root, query, cb) -> (accountIds == null || accountIds.isEmpty()) ? null
        : root.get("account").get("id").in(accountIds);
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
