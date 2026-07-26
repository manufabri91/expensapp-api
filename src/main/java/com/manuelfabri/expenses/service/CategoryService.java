package com.manuelfabri.expenses.service;

import java.util.List;
import com.manuelfabri.expenses.dto.CategoryDto;
import com.manuelfabri.expenses.dto.CategoryRequestDto;
import com.manuelfabri.expenses.model.Category;


public interface CategoryService {
  List<CategoryDto> getAllCategories();

  CategoryDto createCategory(CategoryRequestDto createRequest, Boolean createSubcategory);

  CategoryDto createCategory(Category category);

  CategoryDto updateCategory(Long id, CategoryRequestDto categoryDto);

  void deleteCategory(Long id);

  CategoryDto getById(Long id);
}
