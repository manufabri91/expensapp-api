package com.manuelfabri.expenses.service.implementation;

import java.util.List;
import java.util.stream.Collectors;
import org.modelmapper.ModelMapper;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import com.manuelfabri.expenses.dto.CategoryDto;
import com.manuelfabri.expenses.dto.CreateCategoryDto;
import com.manuelfabri.expenses.dto.CreateSubcategoryDto;
import com.manuelfabri.expenses.dto.SubcategoryDto;
import com.manuelfabri.expenses.exception.ResourceNotFoundException;
import com.manuelfabri.expenses.model.Category;
import com.manuelfabri.expenses.model.Subcategory;
import com.manuelfabri.expenses.model.User;
import com.manuelfabri.expenses.repository.CategoryRepository;
import com.manuelfabri.expenses.repository.SubcategoryRepository;
import com.manuelfabri.expenses.service.SubcategoryService;

@Service
public class SubcategoryServiceImplementation implements SubcategoryService {
  private SubcategoryRepository subcategoryRepository;
  private CategoryRepository categoryRepository;
  private ModelMapper mapper;

  public SubcategoryServiceImplementation(SubcategoryRepository subcategoryRepository,
      CategoryRepository categoryRepository, ModelMapper mapper) {
    this.subcategoryRepository = subcategoryRepository;
    this.categoryRepository = categoryRepository;
    this.mapper = mapper;
  }

  @Override
  public SubcategoryDto getById(Long id) {
    return this.subcategoryRepository.findActiveById(id).map((cat) -> mapper.map(cat, SubcategoryDto.class))
        .orElseThrow(() -> new ResourceNotFoundException("Subcategory", "id", id.toString()));
  }

  @Override
  public List<SubcategoryDto> getAllSubcategories() {
    return this.subcategoryRepository.findActive().stream().map(category -> mapper.map(category, SubcategoryDto.class))
        .collect(Collectors.toList());
  }

  @Override
  public SubcategoryDto createSubcategory(CreateSubcategoryDto createRequest) {
    User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    Category parentCategory = this.categoryRepository.findActiveById(createRequest.getParentCategoryId()).orElseThrow(
        () -> new ResourceNotFoundException("Parent category", "id", createRequest.getParentCategoryId().toString()));
    Subcategory subcategory = mapper.map(createRequest, Subcategory.class);
    subcategory.setId(null); // TODO: Revisar como hacer para que el mapper lo deje nulo
    subcategory.setOwner(user);
    subcategory.setParentCategory(parentCategory);
    Subcategory newSubcategory = this.subcategoryRepository.save(subcategory);

    return mapper.map(newSubcategory, SubcategoryDto.class);
  }

  @Override
  public void deleteSubcategory(Long id) {
    Subcategory subcategory = this.subcategoryRepository.findActiveById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Subcategory", "id", id.toString()));
    this.subcategoryRepository.delete(subcategory);
  }
}
