package com.manuelfabri.expenses.repository;

import java.util.List;
import com.manuelfabri.expenses.model.Account;
import com.manuelfabri.expenses.model.User;

public interface AccountRepository extends BaseEntityRepository<Account> {
  List<Account> findByOwnerAndDeletedFalse(User owner);

  List<Account> findByOwner(User owner);
}
