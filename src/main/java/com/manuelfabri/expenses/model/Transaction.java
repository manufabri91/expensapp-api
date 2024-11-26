package com.manuelfabri.expenses.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity(name = "transactions")
public class Transaction extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  @Column(nullable = false)
  private OffsetDateTime eventDate;
  @Column(nullable = false, length = 80)
  private String description;
  @Column(nullable = false)
  private BigDecimal amount;
  @ManyToOne
  @JoinColumn(name = "accountid")
  private Account account;
  @ManyToOne
  @JoinColumn(name = "owner")
  private User owner;
  @ManyToOne
  @JoinColumn(name = "category")
  private Category category;
  @ManyToOne
  @JoinColumn(name = "subcategory")
  private Subcategory subcategory;


  // CONSTRUCTORS
  public Transaction() {}

  public Transaction(Long id, OffsetDateTime eventDate, String description, BigDecimal amount,
      CurrencyEnum currencyCode, User owner, Category category, Subcategory subcategory) {
    this.id = id;
    this.eventDate = eventDate;
    this.description = description;
    this.amount = amount;
    this.owner = owner;
    this.category = category;
    this.subcategory = subcategory;
  }

  // GETTERS AND SETTERS
  @Override
  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public OffsetDateTime getEventDate() {
    return eventDate;
  }

  public void setEventDate(OffsetDateTime eventDate) {
    this.eventDate = eventDate;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public void setAmount(BigDecimal amount) {
    this.amount = amount;
  }

  public TransactionTypeEnum getType() {
    return this.amount.signum() > 0 ? TransactionTypeEnum.INCOME : TransactionTypeEnum.EXPENSE;
  }

  public CurrencyEnum getCurrencyCode() {
    return this.account.getCurrency();
  }

  public Account getAccount() {
    return account;
  }

  public void setAccount(Account account) {
    this.account = account;
  }

  public User getOwner() {
    return owner;
  }

  public void setOwner(User owner) {
    this.owner = owner;
  }

  public Category getCategory() {
    return category;
  }

  public void setCategory(Category category) {
    this.category = category;
  }

  public Subcategory getSubcategory() {
    return subcategory;
  }

  public void setSubcategory(Subcategory subcategory) {
    this.subcategory = subcategory;
  }


}
