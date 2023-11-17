package com.manuelfabri.expenses.service;

import java.util.List;
import com.manuelfabri.expenses.dto.CategoryDto;
import com.manuelfabri.expenses.dto.CreateCategoryDto;


public interface CategoryService {
  List<CategoryDto> getAllCategories();

  CategoryDto createCategory(CreateCategoryDto createRequest);

  void deleteCategory(Long id);

  CategoryDto getById(Long id);

}
