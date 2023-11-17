package com.manuelfabri.expenses.model;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.springframework.security.core.context.SecurityContextHolder;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

@MappedSuperclass
public abstract class BaseEntity {

  @Column(updatable = false)
  private OffsetDateTime createdAt;

  private String createdBy;

  private OffsetDateTime updatedAt;

  private String updatedBy;

  private boolean deleted;

  private String deletedBy;

  private OffsetDateTime deletedAt;

  public abstract Long getId();

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public String getCreatedBy() {
    return createdBy;
  }

  public OffsetDateTime getUpdatedAt() {
    return updatedAt;
  }

  public String getUpdatedBy() {
    return updatedBy;
  }

  public boolean isDeleted() {
    return deleted;
  }

  public String getDeletedBy() {
    return deletedBy;
  }

  public OffsetDateTime getDeletedAt() {
    return deletedAt;
  }

  @PrePersist
  public void onPrePersist() {
    if (this.createdAt == null) {
      this.createdAt = Instant.now().atOffset(ZoneOffset.UTC);
      this.createdBy = getAuditor();
    }
    this.updatedAt = Instant.now().atOffset(ZoneOffset.UTC);
    this.updatedBy = getAuditor();
  }

  @PreUpdate
  public void onPreUpdate() {
    this.updatedAt = Instant.now().atOffset(ZoneOffset.UTC);
    this.updatedBy = getAuditor();
  }

  public static String getAuditor() {
    User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    return user.getId();
  }

}
