package com.manuelfabri.expenses.service.implementation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.modelmapper.config.Configuration.AccessLevel;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import com.manuelfabri.expenses.dto.TransactionDto;
import com.manuelfabri.expenses.dto.TransactionRequestDto;
import com.manuelfabri.expenses.model.Account;
import com.manuelfabri.expenses.model.Category;
import com.manuelfabri.expenses.model.CurrencyEnum;
import com.manuelfabri.expenses.model.Subcategory;
import com.manuelfabri.expenses.model.Transaction;
import com.manuelfabri.expenses.model.TransactionTypeEnum;
import com.manuelfabri.expenses.model.User;
import com.manuelfabri.expenses.repository.AccountRepository;
import com.manuelfabri.expenses.repository.CategoryRepository;
import com.manuelfabri.expenses.repository.SubcategoryRepository;
import com.manuelfabri.expenses.repository.TransactionRepository;

/**
 * Focused on the sign-application call sites touched while extracting {@code TransactionTypeEnum.applySign} — not a
 * full test of every existing behavior in this service (transfers, paging/specifications, monthly summaries), which
 * remain part of the broader test-coverage gap tracked separately.
 */
@ExtendWith(MockitoExtension.class)
class TransactionServiceImplementationTest {

  @Mock
  private TransactionRepository transactionRepository;
  @Mock
  private CategoryRepository categoryRepository;
  @Mock
  private AccountRepository accountRepository;
  @Mock
  private SubcategoryRepository subcategoryRepository;

  private TransactionServiceImplementation service;
  private User currentUser;
  private Account account;
  private Category category;
  private Subcategory subcategory;

  @BeforeEach
  void setUp() {
    ModelMapper mapper = new ModelMapper();
    mapper.getConfiguration().setFieldMatchingEnabled(true).setFieldAccessLevel(AccessLevel.PRIVATE)
        .setSkipNullEnabled(true);

    service =
        new TransactionServiceImplementation(transactionRepository, categoryRepository, subcategoryRepository,
            accountRepository, mapper);

    currentUser = new User("owner-1", "owner@example.com", "owner", "Owner", "One", List.of());
    account = new Account(10L, "Checking", CurrencyEnum.USD, currentUser);
    category = new Category(20L, "Groceries", "icon", "#fff", currentUser, List.of());
    subcategory = new Subcategory(30L, "Supermarket", category, List.of(), currentUser);

    SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
    securityContext.setAuthentication(new UsernamePasswordAuthenticationToken(currentUser, null));
    SecurityContextHolder.setContext(securityContext);
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private TransactionRequestDto requestDto(TransactionTypeEnum type, BigDecimal amount) {
    TransactionRequestDto requestDto = new TransactionRequestDto();
    requestDto.setType(type);
    requestDto.setAmount(amount);
    requestDto.setDescription("Weekly groceries");
    requestDto.setAccountId(account.getId());
    requestDto.setCategoryId(category.getId());
    requestDto.setSubcategoryId(subcategory.getId());
    requestDto.setEventDate(OffsetDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC));
    return requestDto;
  }

  private void stubRelatedEntities() {
    when(categoryRepository.findActiveById(category.getId())).thenReturn(Optional.of(category));
    when(subcategoryRepository.findActiveById(subcategory.getId())).thenReturn(Optional.of(subcategory));
    when(accountRepository.findActiveById(account.getId())).thenReturn(Optional.of(account));
  }

  @Test
  void createTransaction_keepsAPositiveAmount_forIncome() {
    TransactionRequestDto requestDto = requestDto(TransactionTypeEnum.INCOME, new BigDecimal("120.00"));
    stubRelatedEntities();
    when(transactionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    TransactionDto result = service.createTransaction(requestDto);

    assertThat(result.getAmount()).isEqualByComparingTo("120.00");
    assertThat(result.getType()).isEqualTo(TransactionTypeEnum.INCOME);
  }

  @Test
  void createTransaction_negatesTheAmount_forExpense() {
    TransactionRequestDto requestDto = requestDto(TransactionTypeEnum.EXPENSE, new BigDecimal("45.50"));
    stubRelatedEntities();
    when(transactionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    TransactionDto result = service.createTransaction(requestDto);

    assertThat(result.getAmount()).isEqualByComparingTo("-45.50");
  }

  @Test
  void createTransaction_ownsTheTransactionAsTheCurrentUser_andSetsRelatedEntities() {
    TransactionRequestDto requestDto = requestDto(TransactionTypeEnum.EXPENSE, new BigDecimal("45.50"));
    stubRelatedEntities();
    when(transactionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    TransactionDto result = service.createTransaction(requestDto);

    assertThat(result.getAccountId()).isEqualTo(account.getId());
    assertThat(result.getCategory().getId()).isEqualTo(category.getId());
    assertThat(result.getSubcategory().getId()).isEqualTo(subcategory.getId());
  }

  @Test
  void updateTransaction_reappliesTheSignConvention_whenTypeChangesFromIncomeToExpense() {
    Transaction existingTransaction = new Transaction();
    existingTransaction.setId(1L);
    existingTransaction.setType(TransactionTypeEnum.INCOME);
    existingTransaction.setAmount(new BigDecimal("100.00"));
    when(transactionRepository.findActiveById(1L)).thenReturn(Optional.of(existingTransaction));
    TransactionRequestDto requestDto = requestDto(TransactionTypeEnum.EXPENSE, new BigDecimal("100.00"));
    stubRelatedEntities();
    when(transactionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    TransactionDto result = service.updateTransaction(1L, requestDto);

    assertThat(result.getAmount()).isEqualByComparingTo("-100.00");
    assertThat(result.getType()).isEqualTo(TransactionTypeEnum.EXPENSE);
  }
}
