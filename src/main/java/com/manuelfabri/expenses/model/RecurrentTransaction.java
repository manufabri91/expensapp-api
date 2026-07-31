package com.manuelfabri.expenses.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

@Entity(name = "recurrenttransactions")
public class RecurrentTransaction extends BaseEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, name = "transactiontype")
  private TransactionTypeEnum type;

  @Column(nullable = false)
  private BigDecimal amount;

  @Column(nullable = false, length = 80)
  private String description;

  @ManyToOne
  @JoinColumn(name = "accountid")
  private Account account;

  @ManyToOne
  @JoinColumn(name = "category")
  private Category category;

  @ManyToOne
  @JoinColumn(name = "subcategory")
  private Subcategory subcategory;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private RecurrenceFrequencyEnum frequency;

  @Column(name = "intervaldays")
  private Integer intervalDays;

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(name = "recurrenttransaction_days_of_month",
      joinColumns = @JoinColumn(name = "recurrenttransactionid"))
  @Column(name = "day_of_month", nullable = false)
  @Fetch(FetchMode.SUBSELECT)
  private Set<Integer> daysOfMonth = new HashSet<>();

  @Column(nullable = false, name = "startdate")
  private OffsetDateTime startDate;

  @Column(name = "enddate")
  private OffsetDateTime endDate;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private RecurrenceStatusEnum status = RecurrenceStatusEnum.ACTIVE;

  @Column(name = "lastgenerateddate")
  private OffsetDateTime lastGeneratedDate;

  @Column(nullable = false)
  private boolean excludeFromTotals = false;

  // GETTERS AND SETTERS
  @Override
  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public TransactionTypeEnum getType() {
    return type;
  }

  public void setType(TransactionTypeEnum type) {
    this.type = type;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public void setAmount(BigDecimal amount) {
    this.amount = amount;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
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

  public RecurrenceFrequencyEnum getFrequency() {
    return frequency;
  }

  public void setFrequency(RecurrenceFrequencyEnum frequency) {
    this.frequency = frequency;
  }

  public Integer getIntervalDays() {
    return intervalDays;
  }

  public void setIntervalDays(Integer intervalDays) {
    this.intervalDays = intervalDays;
  }

  public Set<Integer> getDaysOfMonth() {
    return daysOfMonth;
  }

  public void setDaysOfMonth(Set<Integer> daysOfMonth) {
    this.daysOfMonth = daysOfMonth;
  }

  public OffsetDateTime getStartDate() {
    return startDate;
  }

  public void setStartDate(OffsetDateTime startDate) {
    this.startDate = startDate;
  }

  public OffsetDateTime getEndDate() {
    return endDate;
  }

  public void setEndDate(OffsetDateTime endDate) {
    this.endDate = endDate;
  }

  public RecurrenceStatusEnum getStatus() {
    return status;
  }

  public void setStatus(RecurrenceStatusEnum status) {
    this.status = status;
  }

  public OffsetDateTime getLastGeneratedDate() {
    return lastGeneratedDate;
  }

  public void setLastGeneratedDate(OffsetDateTime lastGeneratedDate) {
    this.lastGeneratedDate = lastGeneratedDate;
  }

  public boolean getExcludeFromTotals() {
    return excludeFromTotals;
  }

  public void setExcludeFromTotals(boolean excludeFromTotals) {
    this.excludeFromTotals = excludeFromTotals;
  }
}
