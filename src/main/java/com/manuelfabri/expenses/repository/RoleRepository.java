package com.manuelfabri.expenses.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.manuelfabri.expenses.model.Role;

public interface RoleRepository extends JpaRepository<Role, Long> {

  Optional<Role> findByName(String name);
}
