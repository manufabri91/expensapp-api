package com.manuelfabri.expenses.service.implementation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.modelmapper.config.Configuration.AccessLevel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import com.manuelfabri.expenses.dto.TransactionDto;
import com.manuelfabri.expenses.dto.TransactionRequestDto;
import com.manuelfabri.expenses.exception.ResourceNotFoundException;
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
 * Focused on the sign-application call sites touched while extracting {@code TransactionTypeEnum.applySign}, the
 * pending formula (create/update/confirm/list), and that the pending exclusion is wired into the read paths — not a
 * full test of every existing behavior in this service (paging/filtering edge cases beyond pending), which remains
 * part of the broader test-coverage gap tracked separately.
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
  private Account destinationAccount;
  private Category category;
  private Subcategory subcategory;
  private Category transferCategory;
  private Subcategory transferInSubcategory;
  private Subcategory transferOutSubcategory;

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
    destinationAccount = new Account(11L, "Savings", CurrencyEnum.USD, currentUser);
    category = new Category(20L, "Groceries", "icon", "#fff", currentUser, List.of());
    subcategory = new Subcategory(30L, "Supermarket", category, List.of(), currentUser);
    transferCategory = new Category(40L, "Transfer", "icon", "#000", currentUser, List.of());
    transferInSubcategory = new Subcategory(41L, "Transfer In", transferCategory, List.of(), currentUser);
    transferOutSubcategory = new Subcategory(42L, "Transfer Out", transferCategory, List.of(), currentUser);

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

  private TransactionRequestDto requestDto(TransactionTypeEnum type, BigDecimal amount, OffsetDateTime eventDate,
      boolean excludeFromTotals) {
    TransactionRequestDto requestDto = requestDto(type, amount);
    requestDto.setEventDate(eventDate);
    requestDto.setExcludeFromTotals(excludeFromTotals);
    return requestDto;
  }

  private TransactionRequestDto transferRequestDto(OffsetDateTime eventDate) {
    TransactionRequestDto requestDto = new TransactionRequestDto();
    requestDto.setType(TransactionTypeEnum.TRANSFER);
    requestDto.setAmount(new BigDecimal("100.00"));
    requestDto.setDescription("Move money");
    requestDto.setAccountId(account.getId());
    requestDto.setDestinationAccountId(destinationAccount.getId());
    requestDto.setEventDate(eventDate);
    return requestDto;
  }

  private void stubRelatedEntities() {
    when(categoryRepository.findActiveById(category.getId())).thenReturn(Optional.of(category));
    when(subcategoryRepository.findActiveById(subcategory.getId())).thenReturn(Optional.of(subcategory));
    when(accountRepository.findActiveById(account.getId())).thenReturn(Optional.of(account));
  }

  private void stubTransferRelatedEntities() {
    when(categoryRepository.findActiveByNameAndIsSystem("TRANSFER.CATEGORY")).thenReturn(Optional.of(transferCategory));
    when(subcategoryRepository.findActiveByNameAndIsSystem("TRANSFER.IN.SUBCATEGORY"))
        .thenReturn(Optional.of(transferInSubcategory));
    when(subcategoryRepository.findActiveByNameAndIsSystem("TRANSFER.OUT.SUBCATEGORY"))
        .thenReturn(Optional.of(transferOutSubcategory));
    when(accountRepository.findActiveById(account.getId())).thenReturn(Optional.of(account));
    when(accountRepository.findActiveById(destinationAccount.getId())).thenReturn(Optional.of(destinationAccount));
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

  // --- pending formula: createTransaction -------------------------------------------------------------------

  @Test
  void createTransaction_setsPendingTrue_forFutureDatedTransactionExcludedFromTotals() {
    OffsetDateTime futureDate = OffsetDateTime.now(ZoneOffset.UTC).plusDays(10);
    TransactionRequestDto requestDto = requestDto(TransactionTypeEnum.EXPENSE, new BigDecimal("45.50"), futureDate, true);
    stubRelatedEntities();
    when(transactionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    TransactionDto result = service.createTransaction(requestDto);

    assertThat(result.isPending()).isTrue();
  }

  @Test
  void createTransaction_setsPendingFalse_forFutureDatedTransactionNotExcludedFromTotals() {
    OffsetDateTime futureDate = OffsetDateTime.now(ZoneOffset.UTC).plusDays(10);
    TransactionRequestDto requestDto =
        requestDto(TransactionTypeEnum.EXPENSE, new BigDecimal("45.50"), futureDate, false);
    stubRelatedEntities();
    when(transactionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    TransactionDto result = service.createTransaction(requestDto);

    assertThat(result.isPending()).isFalse();
  }

  @Test
  void createTransaction_setsPendingFalse_forPastDatedTransactionExcludedFromTotals() {
    OffsetDateTime pastDate = OffsetDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
    TransactionRequestDto requestDto =
        requestDto(TransactionTypeEnum.EXPENSE, new BigDecimal("45.50"), pastDate, true);
    stubRelatedEntities();
    when(transactionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    TransactionDto result = service.createTransaction(requestDto);

    assertThat(result.isPending()).isFalse();
  }

  @Test
  void createTransaction_setsPendingFalse_forFutureDatedTransfer_regardlessOfExcludeFromTotals() {
    OffsetDateTime futureDate = OffsetDateTime.now(ZoneOffset.UTC).plusDays(10);
    TransactionRequestDto requestDto = transferRequestDto(futureDate);
    stubTransferRelatedEntities();
    when(transactionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    TransactionDto result = service.createTransaction(requestDto);

    assertThat(result.isPending()).isFalse();
    assertThat(result.isExcludeFromTotals()).isTrue();
  }

  // --- pending formula: updateTransaction --------------------------------------------------------------------

  private Transaction existingNonTransferTransaction() {
    Transaction transaction = new Transaction();
    transaction.setId(1L);
    transaction.setType(TransactionTypeEnum.EXPENSE);
    transaction.setAmount(new BigDecimal("-45.50"));
    transaction.setEventDate(OffsetDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC));
    transaction.setAccount(account);
    transaction.setCategory(category);
    transaction.setSubcategory(subcategory);
    return transaction;
  }

  @Test
  void updateTransaction_setsPendingTrue_forFutureDatedTransactionExcludedFromTotals() {
    when(transactionRepository.findActiveById(1L)).thenReturn(Optional.of(existingNonTransferTransaction()));
    OffsetDateTime futureDate = OffsetDateTime.now(ZoneOffset.UTC).plusDays(10);
    TransactionRequestDto requestDto = requestDto(TransactionTypeEnum.EXPENSE, new BigDecimal("45.50"), futureDate, true);
    stubRelatedEntities();
    when(transactionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    TransactionDto result = service.updateTransaction(1L, requestDto);

    assertThat(result.isPending()).isTrue();
  }

  @Test
  void updateTransaction_setsPendingFalse_forFutureDatedTransactionNotExcludedFromTotals() {
    when(transactionRepository.findActiveById(1L)).thenReturn(Optional.of(existingNonTransferTransaction()));
    OffsetDateTime futureDate = OffsetDateTime.now(ZoneOffset.UTC).plusDays(10);
    TransactionRequestDto requestDto =
        requestDto(TransactionTypeEnum.EXPENSE, new BigDecimal("45.50"), futureDate, false);
    stubRelatedEntities();
    when(transactionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    TransactionDto result = service.updateTransaction(1L, requestDto);

    assertThat(result.isPending()).isFalse();
  }

  @Test
  void updateTransaction_setsPendingFalse_forPastDatedTransactionExcludedFromTotals() {
    when(transactionRepository.findActiveById(1L)).thenReturn(Optional.of(existingNonTransferTransaction()));
    OffsetDateTime pastDate = OffsetDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
    TransactionRequestDto requestDto =
        requestDto(TransactionTypeEnum.EXPENSE, new BigDecimal("45.50"), pastDate, true);
    stubRelatedEntities();
    when(transactionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    TransactionDto result = service.updateTransaction(1L, requestDto);

    assertThat(result.isPending()).isFalse();
  }

  @Test
  void updateTransaction_setsPendingFalse_forFutureDatedTransfer() {
    when(transactionRepository.findActiveById(1L)).thenReturn(Optional.of(existingNonTransferTransaction()));
    OffsetDateTime futureDate = OffsetDateTime.now(ZoneOffset.UTC).plusDays(10);
    TransactionRequestDto requestDto = transferRequestDto(futureDate);
    stubTransferRelatedEntities();
    when(transactionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    TransactionDto result = service.updateTransaction(1L, requestDto);

    assertThat(result.isPending()).isFalse();
    assertThat(result.isExcludeFromTotals()).isTrue();
  }

  @Test
  void updateTransaction_flipsPendingToFalse_whenEventDateMovesFromFutureToPast() {
    Transaction existingTransaction = existingNonTransferTransaction();
    existingTransaction.setExcludeFromTotals(true);
    existingTransaction.setPending(true);
    when(transactionRepository.findActiveById(1L)).thenReturn(Optional.of(existingTransaction));
    OffsetDateTime pastDate = OffsetDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
    TransactionRequestDto requestDto =
        requestDto(TransactionTypeEnum.EXPENSE, new BigDecimal("45.50"), pastDate, true);
    stubRelatedEntities();
    when(transactionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    TransactionDto result = service.updateTransaction(1L, requestDto);

    assertThat(result.isPending()).isFalse();
  }

  // --- confirm ------------------------------------------------------------------------------------------------

  @Test
  void confirm_clearsExcludeFromTotalsAndPending_andReturnsTheMappedTransaction() {
    Transaction existingTransaction = existingNonTransferTransaction();
    existingTransaction.setExcludeFromTotals(true);
    existingTransaction.setPending(true);
    when(transactionRepository.findActiveById(1L)).thenReturn(Optional.of(existingTransaction));
    when(transactionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    TransactionDto result = service.confirm(1L);

    assertThat(result.isPending()).isFalse();
    assertThat(result.isExcludeFromTotals()).isFalse();
  }

  @Test
  void confirm_throwsResourceNotFound_whenTransactionMissing() {
    when(transactionRepository.findActiveById(1L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.confirm(1L)).isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void confirm_rejectsATransferLeg_becauseItIsNotPending() {
    Transaction transferLeg = existingNonTransferTransaction();
    transferLeg.setType(TransactionTypeEnum.TRANSFER);
    transferLeg.setExcludeFromTotals(true);
    transferLeg.setPending(false);
    when(transactionRepository.findActiveById(1L)).thenReturn(Optional.of(transferLeg));

    assertThatThrownBy(() -> service.confirm(1L)).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void confirm_rejectsAPermanentlyExcludedTransaction_becauseItIsNotPending() {
    Transaction permanentlyExcluded = existingNonTransferTransaction();
    permanentlyExcluded.setExcludeFromTotals(true);
    permanentlyExcluded.setPending(false);
    when(transactionRepository.findActiveById(1L)).thenReturn(Optional.of(permanentlyExcluded));

    assertThatThrownBy(() -> service.confirm(1L)).isInstanceOf(IllegalArgumentException.class);
  }

  // --- getPendingTransactions ----------------------------------------------------------------------------------

  @Test
  void getPendingTransactions_mapsAllTransactionsReturnedByTheRepository() {
    Transaction firstPending = existingNonTransferTransaction();
    firstPending.setId(1L);
    Transaction secondPending = existingNonTransferTransaction();
    secondPending.setId(2L);
    when(transactionRepository.findAll(any(Specification.class))).thenReturn(List.of(firstPending, secondPending));

    List<TransactionDto> result = service.getPendingTransactions();

    assertThat(result).extracting(TransactionDto::getId).containsExactly(1L, 2L);
  }

  @Test
  void getPendingTransactions_appliesIsPendingSpecification() {
    ArgumentCaptor<Specification<Transaction>> specCaptor = ArgumentCaptor.forClass(Specification.class);
    when(transactionRepository.findAll(specCaptor.capture())).thenReturn(List.of());

    service.getPendingTransactions();

    Root<Transaction> root = mockCriteriaRoot();
    CriteriaBuilder criteriaBuilder = mockCriteriaBuilder();
    specCaptor.getValue().toPredicate(root, mock(CriteriaQuery.class), criteriaBuilder);

    verify(criteriaBuilder).isTrue(root.get("pending"));
  }

  // --- read paths exclude pending -------------------------------------------------------------------------------

  @Test
  void getPagedTransactions_appliesIsNotPendingSpecification() {
    ArgumentCaptor<Specification<Transaction>> specCaptor = ArgumentCaptor.forClass(Specification.class);
    when(transactionRepository.findAll(specCaptor.capture(), any(Pageable.class))).thenReturn(Page.empty());

    service.getPagedTransactions(null, null, null, null, null, null, null, null, Pageable.unpaged());

    Root<Transaction> root = mockCriteriaRoot();
    CriteriaBuilder criteriaBuilder = mockCriteriaBuilder();
    specCaptor.getValue().toPredicate(root, mock(CriteriaQuery.class), criteriaBuilder);

    verify(criteriaBuilder).isFalse(root.get("pending"));
  }

  @Test
  void getFilteredTotals_appliesIsNotPendingSpecification() {
    ArgumentCaptor<Specification<Transaction>> specCaptor = ArgumentCaptor.forClass(Specification.class);
    when(transactionRepository.findAll(specCaptor.capture())).thenReturn(List.of());

    service.getFilteredTotals(null, null, null, null, null, null, null, null);

    Root<Transaction> root = mockCriteriaRoot();
    CriteriaBuilder criteriaBuilder = mockCriteriaBuilder();
    specCaptor.getValue().toPredicate(root, mock(CriteriaQuery.class), criteriaBuilder);

    verify(criteriaBuilder).isFalse(root.get("pending"));
  }

  @Test
  void getMonthlyTransactions_usesThePendingFalseRepositoryMethod() {
    OffsetDateTime startDate = OffsetDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
    OffsetDateTime endDate = startDate.plusMonths(1).minusSeconds(1);
    when(transactionRepository.findByOwnerAndEventDateBetweenAndPendingFalseAndDeletedFalse(currentUser, startDate,
        endDate)).thenReturn(List.of(existingNonTransferTransaction()));

    List<TransactionDto> result = service.getMonthlyTransactions(2024, 1);

    assertThat(result).hasSize(1);
    verify(transactionRepository).findByOwnerAndEventDateBetweenAndPendingFalseAndDeletedFalse(currentUser, startDate,
        endDate);
  }

  // --- criteria API test doubles, used to verify the shape of composed Specifications without a real database ----

  @SuppressWarnings("unchecked")
  private Root<Transaction> mockCriteriaRoot() {
    Map<String, Path<Object>> pathsByField = new HashMap<>();
    Root<Transaction> root = mock(Root.class);
    when(root.get(anyString())).thenAnswer(invocation -> {
      String field = invocation.getArgument(0);
      return pathsByField.computeIfAbsent(field, key -> {
        Path<Object> path = mock(Path.class);
        lenient().when(path.get(anyString())).thenReturn(path);
        return path;
      });
    });
    return root;
  }

  private CriteriaBuilder mockCriteriaBuilder() {
    CriteriaBuilder criteriaBuilder = mock(CriteriaBuilder.class);
    Predicate predicate = mock(Predicate.class);
    lenient().when(criteriaBuilder.isFalse(any())).thenReturn(predicate);
    lenient().when(criteriaBuilder.isTrue(any())).thenReturn(predicate);
    lenient().when(criteriaBuilder.equal(any(), any())).thenReturn(predicate);
    lenient().when(criteriaBuilder.or(any(Predicate.class), any(Predicate.class))).thenReturn(predicate);
    lenient().when(criteriaBuilder.and(any(Predicate.class), any(Predicate.class))).thenReturn(predicate);
    return criteriaBuilder;
  }
}
