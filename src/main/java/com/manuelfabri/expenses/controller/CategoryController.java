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
import com.manuelfabri.expenses.dto.CategoryRequestDto;
import com.manuelfabri.expenses.dto.SubcategoryDto;
import com.manuelfabri.expenses.dto.TransactionDto;
import com.manuelfabri.expenses.service.CategoryService;
import com.manuelfabri.expenses.service.SubcategoryService;
import com.manuelfabri.expenses.service.TransactionService;
import jakarta.validation.Valid;

@RestController
@RequestMapping(Urls.CATEGORY)
public class CategoryController {

  private CategoryService categoryService;
  private TransactionService transactionService;
  private SubcategoryService subcategoryService;

  public CategoryController(CategoryService categoryService, TransactionService transactionService,
      SubcategoryService subcategoryService) {
    this.categoryService = categoryService;
    this.transactionService = transactionService;
    this.subcategoryService = subcategoryService;
  }

  @GetMapping
  public ResponseEntity<List<CategoryDto>> getAllCategories() {
    return new ResponseEntity<>(categoryService.getAllCategories(), HttpStatus.OK);
  }

  @PostMapping
  public ResponseEntity<CategoryDto> createCategory(@RequestBody @Valid CategoryRequestDto categoryDto) {
    return new ResponseEntity<>(categoryService.createCategory(categoryDto, true), HttpStatus.CREATED);
  }

  @PutMapping("/{id}")
  public ResponseEntity<CategoryDto> updateCategory(@PathVariable Long id,
      @RequestBody @Valid CategoryRequestDto categoryDto) {
    return new ResponseEntity<>(categoryService.updateCategory(id, categoryDto), HttpStatus.OK);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<CategoryDto> deleteCategory(@PathVariable Long id) {
    categoryService.deleteCategory(id);
    return new ResponseEntity<>(HttpStatus.OK);
  }

  @GetMapping("/{id}")
  public ResponseEntity<CategoryDto> getById(@PathVariable Long id) {
    return new ResponseEntity<>(categoryService.getById(id), HttpStatus.OK);
  }

  @GetMapping("/{id}/subcategories")
  public ResponseEntity<List<SubcategoryDto>> getSubcategoriesByCategoryId(@PathVariable Long id) {
    return new ResponseEntity<>(subcategoryService.getByParentId(id), HttpStatus.OK);
  }

  @GetMapping("/{id}/transactions")
  public ResponseEntity<List<TransactionDto>> getTransactionsByCategoryId(@PathVariable Long id) {
    return new ResponseEntity<>(transactionService.getTransactionsByCategoryId(id), HttpStatus.OK);
  }
}
