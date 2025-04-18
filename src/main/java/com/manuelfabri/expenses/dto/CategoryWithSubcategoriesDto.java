package com.manuelfabri.expenses.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Data;

@Data
public class CategoryWithSubcategoriesDto {
  @NotNull
  private long id;
  @NotBlank(message = "Name cannot be blank or null")
  private String name;
  @NotNull
  private String iconName;
  @NotNull
  private String color;
  @NotNull
  private List<SubcategoryDto> subcategories;
}
