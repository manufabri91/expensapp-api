package com.manuelfabri.expenses.model;

import java.math.BigDecimal;
import java.util.List;
import org.hibernate.annotations.Formula;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity(name = "accounts")
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"owner", "name"}))
public class Account extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  @Column(nullable = false, length = 100)
  private String name;
  @Column(nullable = false)
  private CurrencyEnum currency;
  @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<Transaction> transactions;
  @ManyToOne
  @JoinColumn(name = "owner")
  private User owner;
  @Column(nullable = false, precision = 10, scale = 2)
  private BigDecimal initialBalance = BigDecimal.ZERO;

  @Formula("(SELECT COALESCE(SUM(t.amount), 0) FROM transactions t WHERE t.accountid = id) + initialbalance")
  private BigDecimal accountBalance;

  public Account(Long id, String name, CurrencyEnum currency, User owner) {
    this.id = id;
    this.name = name;
    this.currency = currency;
    this.owner = owner;
  }

  public Account() {}

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public CurrencyEnum getCurrency() {
    return currency;
  }

  public void setCurrency(CurrencyEnum currency) {
    this.currency = currency;
  }

  public User getOwner() {
    return owner;
  }

  public void setOwner(User owner) {
    this.owner = owner;
  }

  public List<Transaction> getTransactions() {
    return transactions;
  }

  public void setTransactions(List<Transaction> transactions) {
    this.transactions = transactions;
  }

  public BigDecimal getInitialBalance() {
    return initialBalance;
  }

  public void setInitialBalance(BigDecimal initialBalance) {
    this.initialBalance = initialBalance;
  }

  public BigDecimal getAccountBalance() {
    return accountBalance;
  }

  public void setAccountBalance(BigDecimal accountBalance) {
    this.accountBalance = accountBalance;
  }


}
