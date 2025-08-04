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
      throw new IllegalArgumentException("CANNOT_CREATE_TRANSFER_CATEGORY");
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

    if (category.getReadOnly()) {
      throw new IllegalArgumentException("READ_ONLY_CATEGORY");
    }

    this.categoryRepository.delete(category);
  }

  @Override
  public CategoryDto createTransferCategory(User user) {
    // Initialize the user with TRANSFER categories and subcategories
    Category transferCategory = new Category();
    transferCategory.setType(TransactionTypeEnum.TRANSFER);
    transferCategory.setName(DefaultCategories.TRANSFER);
    transferCategory.setIconName(DefaultCategories.TRANSFER_ICON_NAME);
    transferCategory.setColor(DefaultCategories.TRANSFER_COLOR);
    transferCategory.setOwner(user);
    transferCategory.setReadOnly(true);
    Category savedCategory = categoryRepository.save(transferCategory);
    // Create subcategories for the transfer category
    Subcategory transferInSubcategory = new Subcategory();
    transferInSubcategory.setName(DefaultCategories.TRANSFER_IN);
    transferInSubcategory.setOwner(user);
    transferInSubcategory.setParentCategory(savedCategory);
    transferInSubcategory.setReadOnly(true);
    subCategoryRepository.save(transferInSubcategory);
    Subcategory transferOutSubcategory = new Subcategory();
    transferOutSubcategory.setName(DefaultCategories.TRANSFER_OUT);
    transferOutSubcategory.setOwner(user);
    transferOutSubcategory.setParentCategory(savedCategory);
    transferOutSubcategory.setReadOnly(true);
    subCategoryRepository.save(transferOutSubcategory);

    return mapper.map(savedCategory, CategoryDto.class);
  }
}
