package com.manuelfabri.expenses.repository;

import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

  @Query("SELECT t.account.currency, SUM(t.amount) FROM #{#entityName} t WHERE EXTRACT(YEAR FROM t.eventDate) = :year AND t.amount > 0 and t.owner.id = ?#{ principal?.id } GROUP BY t.account.currency")
  List<Object[]> getTransactionsTotalIncomes(@Param("year") int year);

  @Query("SELECT t.account.currency, SUM(t.amount) FROM #{#entityName} t WHERE EXTRACT(MONTH FROM t.eventDate) = :month AND EXTRACT(YEAR FROM t.eventDate) = :year AND t.amount > 0 and t.owner.id = ?#{ principal?.id } GROUP BY t.account.currency")
  List<Object[]> getTransactionsTotalIncomes(@Param("month") int month, @Param("year") int year);

  @Query("SELECT t.account.currency, SUM(t.amount) FROM #{#entityName} t WHERE t.amount > 0 and t.owner.id = ?#{ principal?.id } GROUP BY t.account.currency")
  List<Object[]> getTransactionsTotalIncomes();


  @Query("SELECT t.account.currency, SUM(t.amount) FROM #{#entityName} t WHERE EXTRACT(MONTH FROM t.eventDate) = :month AND EXTRACT(YEAR FROM t.eventDate) = :year AND t.amount < 0 and t.owner.id = ?#{ principal?.id } GROUP BY t.account.currency")
  List<Object[]> getTransactionsTotalExpenses(@Param("month") int month, @Param("year") int year);

  @Query("SELECT t.account.currency, SUM(t.amount) FROM #{#entityName} t WHERE EXTRACT(YEAR FROM t.eventDate) = :year AND t.amount < 0 and t.owner.id = ?#{ principal?.id } GROUP BY t.account.currency")
  List<Object[]> getTransactionsTotalExpenses(@Param("year") int year);

  @Query("SELECT t.account.currency, SUM(t.amount) FROM #{#entityName} t WHERE t.amount < 0 and t.owner.id = ?#{ principal?.id } GROUP BY t.account.currency")
  List<Object[]> getTransactionsTotalExpenses();

  @Query("SELECT t.account.currency, SUM(t.amount) FROM #{#entityName} t WHERE EXTRACT(MONTH FROM t.eventDate) = :month AND EXTRACT(YEAR FROM t.eventDate) = :year AND t.owner.id = ?#{ principal?.id } GROUP BY t.account.currency")
  List<Object[]> getBalancesByCurrency(@Param("month") int month, @Param("year") int year);

  @Query("SELECT t.account.currency, SUM(t.amount) FROM #{#entityName} t WHERE EXTRACT(YEAR FROM t.eventDate) = :year AND t.owner.id = ?#{ principal?.id } GROUP BY t.account.currency")
  List<Object[]> getBalancesByCurrency(@Param("year") int year);

  @Query("SELECT t.account.currency, SUM(t.amount) FROM #{#entityName} t WHERE t.owner.id = ?#{ principal?.id } GROUP BY t.account.currency")
  List<Object[]> getBalancesByCurrency();
}
