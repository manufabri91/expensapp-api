package com.manuelfabri.expenses.repository;

import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.manuelfabri.expenses.model.RecurrentTransaction;

public interface RecurrentTransactionRepository extends BaseEntityRepository<RecurrentTransaction> {
  @Query("select r from #{#entityName} r where r.deleted = false and r.status = 'ACTIVE' "
      + "and r.startDate <= :asOf and (r.endDate is null or r.endDate >= :asOf)")
  List<RecurrentTransaction> findDueForGeneration(@Param("asOf") OffsetDateTime asOf);

  @Query("select r from #{#entityName} r where r.deleted = false and r.status != 'CANCELLED' "
      + "and (r.owner.id = ?#{ principal?.id } or r.owner.id = 'system')")
  List<RecurrentTransaction> findActiveVisible();
}
