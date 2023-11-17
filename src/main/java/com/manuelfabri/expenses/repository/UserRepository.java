package com.manuelfabri.expenses.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

import com.manuelfabri.expenses.model.User;

public interface UserRepository extends JpaRepository<User, String> {

  public Optional<User> findByUsername(String username);

  public Optional<User> findByEmail(String email);

  public Optional<User> findByUsernameOrEmail(String username, String email);
}
