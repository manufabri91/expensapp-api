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
  @Column(nullable = false, name = "transactiontype")
  private TransactionTypeEnum type;

  @ManyToOne
  @JoinColumn(name = "linkedtransactionid")
  private Transaction linkedTransaction;

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
  @JoinColumn(name = "category")
  private Category category;
  @ManyToOne
  @JoinColumn(name = "subcategory")
  private Subcategory subcategory;
  @Column(nullable = false)
  private boolean excludeFromTotals = false;

  // CONSTRUCTORS
  public Transaction() {}

  public Transaction(Long id, OffsetDateTime eventDate, String description, BigDecimal amount,
      CurrencyEnum currencyCode, User owner, Category category, Subcategory subcategory, TransactionTypeEnum type,
      Long linkedTransactionId) {
    this.id = id;
    this.eventDate = eventDate;
    this.description = description;
    this.amount = amount;
    this.setOwner(owner);
    this.category = category;
    this.subcategory = subcategory;
    this.type = type;
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
    return type;
  }

  public void setType(TransactionTypeEnum type) {
    this.type = type;
  }

  public Transaction getLinkedTransaction() {
    return linkedTransaction;
  }

  public void setLinkedTransaction(Transaction linkedTransaction) {
    this.linkedTransaction = linkedTransaction;
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

  public boolean getExcludeFromTotals() {
    return excludeFromTotals;
  }

  public void setExcludeFromTotals(boolean excludeFromTotals) {
    this.excludeFromTotals = excludeFromTotals;
  }
}
