package com.manuelfabri.expenses.repository;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.transaction.annotation.Transactional;
import com.manuelfabri.expenses.model.BaseEntity;


@NoRepositoryBean
public interface BaseEntityRepository<T extends BaseEntity> extends JpaRepository<T, Long> {

  @Query("select e from #{#entityName} e where e.deleted = false and (e.owner.id = ?#{ principal?.id } or e.owner.id = 'system')")
  List<T> findActive();

  @Query("select e from #{#entityName} e where e.deleted = true and (e.owner.id = ?#{ principal?.id } or e.owner.id = 'system')")
  List<T> findInactive();

  @Query("select e from #{#entityName} e where e.deleted = false and (e.owner.id = ?#{ principal?.id } or e.owner.id = 'system')")
  Page<T> findActivePaged(Pageable pageable);

  @Query("select e from #{#entityName} e where e.deleted = true and (e.owner.id = ?#{ principal?.id } or e.owner.id = 'system')")
  Page<T> findInactivePaged(Pageable pageable);

  @Query("select e from #{#entityName} e where e.id = ?1 and e.deleted = false and (e.owner.id = ?#{ principal?.id } or e.owner.id = 'system')")
  Optional<T> findActiveById(Long id);

  @Query("select e from #{#entityName} e where e.id = ?1 and e.deleted = true and (e.owner.id = ?#{ principal?.id } or e.owner.id = 'system')")
  Optional<T> findInactiveById(Long id);

  @Query("select count(e) from #{#entityName} e where e.deleted = false and (e.owner.id = ?#{ principal?.id } or e.owner.id = 'system')")
  long countActive();

  @Query("select count(e) from #{#entityName} e where e.deleted = true and (e.owner.id = ?#{ principal?.id } or e.owner.id = 'system')")
  long countInactive();


  @Query("update #{#entityName} e " + "set e.deleted=true, " + "e.deletedAt = ?2, " + "e.deletedBy = ?3, "
      + "e.updatedAt = ?2, " + "e.updatedBy = ?3 "
      + "where e.id = ?1 and (e.owner.id = ?#{ principal?.id } or e.owner.id = 'system')")
  @Transactional
  @Modifying
  void softDelete(Long id, OffsetDateTime deletedAt, String deletedBy);

  default void softDelete(T entity) {
    softDelete(entity.getId(), Instant.now().atOffset(ZoneOffset.UTC), BaseEntity.getAuditor());
  }

  default void softDeleteById(Long id) {
    softDelete(id, Instant.now().atOffset(ZoneOffset.UTC), BaseEntity.getAuditor());
  }

  @Query("update #{#entityName} e " + "set e.deleted=false, " + "e.deletedAt = null, " + "e.deletedBy = null, "
      + "e.updatedAt = ?2, " + "e.updatedBy = ?3 " + "where e.id = ?1 and e.owner = ?#{ principal?.id }")
  @Transactional
  @Modifying
  void undoSoftDelete(Long id, OffsetDateTime updatedAt, String updatedBy);

  default void undoSoftDelete(T entity) {
    undoSoftDelete(entity.getId(), Instant.now().atOffset(ZoneOffset.UTC), BaseEntity.getAuditor());
  }
}
