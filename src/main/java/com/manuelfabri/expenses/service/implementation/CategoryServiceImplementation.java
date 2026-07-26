package com.manuelfabri.expenses.service.implementation;

import java.util.List;
import java.util.stream.Collectors;
import org.modelmapper.ModelMapper;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.manuelfabri.expenses.constants.DefaultCategories;
import com.manuelfabri.expenses.dto.CategoryDto;
import com.manuelfabri.expenses.dto.CategoryRequestDto;
import com.manuelfabri.expenses.exception.ResourceNotFoundException;
import com.manuelfabri.expenses.model.Category;
import com.manuelfabri.expenses.model.Subcategory;
import com.manuelfabri.expenses.model.TransactionTypeEnum;
import com.manuelfabri.expenses.model.User;
import com.manuelfabri.expenses.repository.CategoryRepository;
import com.manuelfabri.expenses.repository.SubcategoryRepository;
import com.manuelfabri.expenses.service.CategoryService;

@Service
public class CategoryServiceImplementation implements CategoryService {
  private CategoryRepository categoryRepository;
  private SubcategoryRepository subCategoryRepository;
  private ModelMapper mapper;

  public CategoryServiceImplementation(CategoryRepository categoryRepository, ModelMapper mapper,
      SubcategoryRepository subCategoryRepository) {

    this.categoryRepository = categoryRepository;
    this.subCategoryRepository = subCategoryRepository;
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

  @Transactional
  @Override
  public CategoryDto createCategory(CategoryRequestDto createRequest, Boolean createSubcategory) {
    if (createRequest.getType() == TransactionTypeEnum.TRANSFER) {
      throw new IllegalArgumentException("FORBIDDEN_CATEGORY_TYPE");
    }
    User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    Category category = mapper.map(createRequest, Category.class);
    category.setOwner(user);
    Category newCategory = this.categoryRepository.save(category);

    if (createSubcategory) {
      var newSubcategory = new Subcategory();
      newSubcategory.setName(DefaultCategories.GENERAL_SUBCATEGORY);
      newSubcategory.setOwner(user);
      newSubcategory.setParentCategory(newCategory);
      this.subCategoryRepository.save(newSubcategory);
    }

    return mapper.map(newCategory, CategoryDto.class);
  }

  @Override
  public CategoryDto createCategory(Category category) {
    User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    category.setOwner(user);
    Category newCategory = this.categoryRepository.save(category);

    return mapper.map(newCategory, CategoryDto.class);
  }

  @Override
  public CategoryDto updateCategory(Long id, CategoryRequestDto categoryDto) {
    Category category = this.categoryRepository.findActiveById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id.toString()));
    if (category.getReadOnly()) {
      throw new IllegalArgumentException("READ_ONLY_CATEGORY");
    }
    if (categoryDto.getType() == TransactionTypeEnum.TRANSFER) {
      throw new IllegalArgumentException("FORBIDDEN_CATEGORY_TYPE");
    }
    category.setName(categoryDto.getName());
    category.setColor(categoryDto.getColor());
    category.setIconName(categoryDto.getIconName());
    category.setType(categoryDto.getType());

    Category updatedCategory = this.categoryRepository.save(category);

    return mapper.map(updatedCategory, CategoryDto.class);
  }

  @Override
  public void deleteCategory(Long id) {
    Category category = this.categoryRepository.findActiveById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id.toString()));

    if (category.getReadOnly()) {
      throw new IllegalArgumentException("READ_ONLY_CATEGORY");
    }

    this.categoryRepository.delete(category);
  }
}
