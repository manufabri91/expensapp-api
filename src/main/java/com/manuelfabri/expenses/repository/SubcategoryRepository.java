package com.manuelfabri.expenses.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import com.manuelfabri.expenses.model.Subcategory;
import com.manuelfabri.expenses.model.User;

public interface SubcategoryRepository extends BaseEntityRepository<Subcategory> {
  @Query("select e from #{#entityName} e where e.name = ?1 and e.deleted = false and e.owner.id = 'system'")
  Optional<Subcategory> findActiveByNameAndIsSystem(String string);

  List<Subcategory> findByOwnerAndDeletedFalse(User owner);

  List<Subcategory> findByOwner(User owner);
}
