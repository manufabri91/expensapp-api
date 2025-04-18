package com.manuelfabri.expenses.service;

import java.util.List;
import com.manuelfabri.expenses.dto.SubcategoryRequestDto;
import com.manuelfabri.expenses.dto.SubcategoryDto;

public interface SubcategoryService {
  List<SubcategoryDto> getAllSubcategories();

  SubcategoryDto createSubcategory(SubcategoryRequestDto createRequest);

  SubcategoryDto updateSubcategory(Long id, SubcategoryRequestDto createRequest);

  void deleteSubcategory(Long id);

  SubcategoryDto getById(Long id);

  List<SubcategoryDto> getByParentId(Long id);
}
