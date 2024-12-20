package com.manuelfabri.expenses.controller;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.manuelfabri.expenses.constants.Urls;
import com.manuelfabri.expenses.dto.CategoryDto;
import com.manuelfabri.expenses.dto.SubcategoryRequestDto;
import com.manuelfabri.expenses.dto.SubcategoryDto;
import com.manuelfabri.expenses.dto.TransactionDto;
import com.manuelfabri.expenses.service.SubcategoryService;
import com.manuelfabri.expenses.service.TransactionService;

@RestController
@RequestMapping(Urls.SUBCATEGORY)
public class SubcategoryController {

  private SubcategoryService subcategoryService;
  private TransactionService transactionService;

  public SubcategoryController(SubcategoryService subcategoryService, TransactionService transactionService) {
    this.subcategoryService = subcategoryService;
    this.transactionService = transactionService;
  }

  @GetMapping
  public ResponseEntity<List<SubcategoryDto>> getAllCategories() {
    return new ResponseEntity<>(subcategoryService.getAllSubcategories(), HttpStatus.OK);
  }

  @GetMapping("/{id}")
  public ResponseEntity<SubcategoryDto> getById(@PathVariable Long id) {
    return new ResponseEntity<>(subcategoryService.getById(id), HttpStatus.OK);
  }

  @GetMapping("/{id}/transactions")
  public ResponseEntity<List<TransactionDto>> getTransactionsByCategoryId(@PathVariable Long id) {
    return new ResponseEntity<>(transactionService.getTransactionsBySubcategoryId(id), HttpStatus.OK);
  }

  @PutMapping("/{id}")
  public ResponseEntity<SubcategoryDto> updateCategory(@PathVariable Long id,
      @RequestBody SubcategoryRequestDto subcategoryDto) {
    return new ResponseEntity<>(subcategoryService.updateSubcategory(id, subcategoryDto), HttpStatus.OK);
  }

  @PostMapping
  public ResponseEntity<SubcategoryDto> createCategory(@RequestBody SubcategoryRequestDto subcategoryDto) {
    return new ResponseEntity<>(subcategoryService.createSubcategory(subcategoryDto), HttpStatus.CREATED);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<CategoryDto> deleteCategory(@PathVariable Long id) {
    subcategoryService.deleteSubcategory(id);
    return new ResponseEntity<>(HttpStatus.OK);
  }
}
