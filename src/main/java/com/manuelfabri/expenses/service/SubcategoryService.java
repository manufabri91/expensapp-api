package com.manuelfabri.expenses.service;

import java.util.List;
import com.manuelfabri.expenses.dto.CreateSubcategoryDto;
import com.manuelfabri.expenses.dto.SubcategoryDto;


public interface SubcategoryService {
  List<SubcategoryDto> getAllSubcategories();

  SubcategoryDto createSubcategory(CreateSubcategoryDto createRequest);

  void deleteSubcategory(Long id);

  SubcategoryDto getById(Long id);

}
