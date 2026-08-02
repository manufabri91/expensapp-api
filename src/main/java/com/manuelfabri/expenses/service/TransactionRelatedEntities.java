package com.manuelfabri.expenses.service;

import com.manuelfabri.expenses.model.Account;
import com.manuelfabri.expenses.model.Category;
import com.manuelfabri.expenses.model.Subcategory;

/**
 * The account/category/subcategory a transaction (or recurring transaction definition) is resolved against.
 */
public record TransactionRelatedEntities(Account account, Category category, Subcategory subcategory) {
}
