package com.manuelfabri.expenses.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity(name = "users")
public class User {

  @Id
  private String id; // Not autogenerated since we will use UID coming from Firebase for consistency

  private String username;
  @Column(name = "firstname")
  private String firstName;
  @Column(name = "lastname")
  private String lastName;
  private String email;

  @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<Transaction> transactions;

  @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<Account> accounts;

  @ManyToMany
  @JoinTable(name = "users_roles", joinColumns = @JoinColumn(name = "userid", referencedColumnName = "id"),
      inverseJoinColumns = @JoinColumn(name = "roleid", referencedColumnName = "id"))
  @JsonIgnore
  private Collection<Role> roles;

  public User() {}

  public User(String id, String email, String username, String firstName, String lastName, Collection<Role> roles,
      List<Account> accounts, List<Transaction> transactions) {
    this.id = id;
    this.username = username;
    this.firstName = firstName;
    this.lastName = lastName;
    this.email = email;
    this.roles = roles;
    this.accounts = accounts;
    this.transactions = transactions;
  }

  public User(String id, String email, String username, String firstName, String lastName, Collection<Role> roles) {
    this.id = id;
    this.username = username;
    this.firstName = firstName;
    this.lastName = lastName;
    this.email = email;
    this.roles = roles;
    this.accounts = new ArrayList<Account>();
    this.transactions = new ArrayList<Transaction>();
  }

  public String getId() {
    return id;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getFirstName() {
    return firstName;
  }

  public void setFirstName(String firstName) {
    this.firstName = firstName;
  }

  public String getLastName() {
    return lastName;
  }

  public void setLastName(String lastName) {
    this.lastName = lastName;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public Collection<Role> getRoles() {
    return roles;
  }

  public void setRoles(Collection<Role> roles) {
    this.roles = roles;
  }

  public List<Account> getAccounts() {
    return accounts;
  }

  public void setAccounts(List<Account> accounts) {
    this.accounts = accounts;
  }

  public List<Transaction> getTransactions() {
    return transactions;
  }

  public void setTransactions(List<Transaction> transactions) {
    this.transactions = transactions;
  }

  public Map<String, Object> getClaims() {
    List<String> parsedRoles = roles.stream().map(role -> role.getSpringName()).collect(Collectors.toList());
    Map<String, Object> claims = new HashMap<String, Object>();
    claims.put("roles", parsedRoles);
    return claims;
  }

}
