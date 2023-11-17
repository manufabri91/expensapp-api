package com.manuelfabri.expenses.model;

import java.util.List;
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

@Entity(name = "subcategories")
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"parentCategory", "name", "owner"}))
public class Subcategory extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  @Column(nullable = false, length = 100)
  private String name;
  @ManyToOne
  @JoinColumn(name = "parentCategory")
  private Category parentCategory;
  @OneToMany(mappedBy = "subcategory", cascade = CascadeType.ALL)
  private List<Transaction> transactions;
  @ManyToOne
  @JoinColumn(name = "owner")
  private User owner;

  public Subcategory(Long id, String name, Category parentCategory, List<Transaction> transactions, User owner) {
    this.id = id;
    this.name = name;
    this.parentCategory = parentCategory;
    this.transactions = transactions;
    this.owner = owner;
  }

  public Subcategory() {}

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

  public Category getParentCategory() {
    return parentCategory;
  }

  public void setParentCategory(Category parentCategory) {
    this.parentCategory = parentCategory;
  }

  public List<Transaction> getTransactions() {
    return transactions;
  }

  public void setTransactions(List<Transaction> transactions) {
    this.transactions = transactions;
  }

  public User getOwner() {
    return owner;
  }

  public void setOwner(User owner) {
    this.owner = owner;
  }



}
