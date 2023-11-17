package com.manuelfabri.expenses.repository;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import com.manuelfabri.expenses.model.Account;

public interface AccountRepository extends BaseEntityRepository<Account>,  JpaSpecificationExecutor<Account>  {
  
}
