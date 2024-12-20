package com.manuelfabri.expenses.service.implementation;

import java.util.List;
import java.util.stream.Collectors;
import org.modelmapper.ModelMapper;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import com.manuelfabri.expenses.dto.CategoryDto;
import com.manuelfabri.expenses.dto.CategoryRequestDto;
import com.manuelfabri.expenses.exception.ResourceNotFoundException;
import com.manuelfabri.expenses.model.Category;
import com.manuelfabri.expenses.model.User;
import com.manuelfabri.expenses.repository.CategoryRepository;
import com.manuelfabri.expenses.service.CategoryService;

@Service
public class CategoryServiceImplementation implements CategoryService {
  private CategoryRepository categoryRepository;
  private ModelMapper mapper;

  public CategoryServiceImplementation(CategoryRepository categoryRepository, ModelMapper mapper) {

    this.categoryRepository = categoryRepository;
    this.mapper = mapper;
  }

  @Override
  public CategoryDto getById(Long id) {
    return this.categoryRepository.findActiveById(id).map((cat) -> mapper.map(cat, CategoryDto.class))
        .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id.toString()));
  }

  @Override
  public List<CategoryDto> getAllCategories() {
    return this.categoryRepository.findActive().stream().map(category -> mapper.map(category, CategoryDto.class))
        .collect(Collectors.toList());
  }

  @Override
  public CategoryDto createCategory(CategoryRequestDto createRequest) {
    User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    Category category = mapper.map(createRequest, Category.class);
    category.setOwner(user);
    Category newCategory = this.categoryRepository.save(category);

    return mapper.map(newCategory, CategoryDto.class);
  }

  @Override
  public CategoryDto updateCategory(Long id, CategoryRequestDto categoryDto) {
    Category category = this.categoryRepository.findActiveById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id.toString()));
    category.setName(categoryDto.getName());
    category.setColor(categoryDto.getColor());
    category.setIconName(categoryDto.getIconName());

    Category updatedCategory = this.categoryRepository.save(category);

    return mapper.map(updatedCategory, CategoryDto.class);
  }

  @Override
  public void deleteCategory(Long id) {
    Category category = this.categoryRepository.findActiveById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id.toString()));
    this.categoryRepository.delete(category);
  }
}
