package com.manuelfabri.specification;

import com.manuelfabri.expenses.model.BaseEntity;
import com.manuelfabri.expenses.model.User;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class BaseEntitySpecifications {

  public static <T extends BaseEntity> Specification<T> activeForCurrentUser() {
    return (root, query, cb) -> {
      Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

      String principalId = authentication != null && authentication.getPrincipal() != null
          ? ((User) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getId()
          : null;

      return cb.and(cb.isFalse(root.get("deleted")),
          cb.or(cb.equal(root.get("owner").get("id"), principalId), cb.equal(root.get("owner").get("id"), "system")));
    };
  }

  public static <T extends BaseEntity> Specification<T> inactiveForCurrentUser() {
    return (root, query, cb) -> {
      Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
      String principalId = authentication != null && authentication.getPrincipal() != null
          ? ((User) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getId()
          : null;

      return cb.and(cb.isTrue(root.get("deleted")),
          cb.or(cb.equal(root.get("owner").get("id"), principalId), cb.equal(root.get("owner").get("id"), "system")));
    };
  }
}
