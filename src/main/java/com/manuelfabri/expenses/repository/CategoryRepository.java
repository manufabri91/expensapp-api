package com.manuelfabri.expenses.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import com.manuelfabri.expenses.model.Category;

public interface CategoryRepository extends BaseEntityRepository<Category> {
  @Query("select e from #{#entityName} e where e.name = ?1 and e.deleted = false and e.owner.id = 'system'")
  Optional<Category> findActiveByNameAndIsSystem(String string);
}
