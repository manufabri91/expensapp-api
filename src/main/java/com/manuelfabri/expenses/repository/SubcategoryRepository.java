package com.manuelfabri.expenses.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import com.manuelfabri.expenses.model.Subcategory;

public interface SubcategoryRepository extends BaseEntityRepository<Subcategory> {
  @Query("select e from #{#entityName} e where e.name = ?1 and e.deleted = false and e.owner.id = 'system'")
  Optional<Subcategory> findActiveByNameAndIsSystem(String string);
}
