package com.manuelfabri.expenses.service;

import java.util.List;
import com.manuelfabri.expenses.dto.CategoryDto;
import com.manuelfabri.expenses.dto.CategoryRequestDto;
import com.manuelfabri.expenses.model.Category;
import com.manuelfabri.expenses.model.User;


public interface CategoryService {
  List<CategoryDto> getAllCategories();

  CategoryDto createCategory(CategoryRequestDto createRequest, Boolean createSubcategory);

  CategoryDto createCategory(Category category);

  CategoryDto createTransferCategory(User user);

  CategoryDto updateCategory(Long id, CategoryRequestDto categoryDto);

  void deleteCategory(Long id);

  CategoryDto getById(Long id);
}
