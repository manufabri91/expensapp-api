package com.manuelfabri.expenses.repository;

import java.time.OffsetDateTime;
import java.util.List;
import com.manuelfabri.expenses.model.Account;
import com.manuelfabri.expenses.model.Category;
import com.manuelfabri.expenses.model.Subcategory;
import com.manuelfabri.expenses.model.Transaction;
import com.manuelfabri.expenses.model.User;

public interface TransactionRepository extends BaseEntityRepository<Transaction> {
  List<Transaction> findByOwnerAndAccountAndDeletedFalse(User user, Account account);

  List<Transaction> findByOwnerAndCategoryAndDeletedFalse(User user, Category category);

  List<Transaction> findByOwnerAndSubcategoryAndDeletedFalse(User user, Subcategory subcategory);

  List<Transaction> findByOwnerAndEventDateBetweenAndDeletedFalse(User user, OffsetDateTime dateStart,
      OffsetDateTime dateEnd);
}
