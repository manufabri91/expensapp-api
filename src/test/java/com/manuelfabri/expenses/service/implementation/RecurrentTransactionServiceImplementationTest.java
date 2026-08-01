package com.manuelfabri.expenses.service.implementation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.modelmapper.config.Configuration.AccessLevel;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import com.manuelfabri.expenses.dto.RecurrentTransactionDto;
import com.manuelfabri.expenses.dto.RecurrentTransactionRequestDto;
import com.manuelfabri.expenses.exception.ResourceNotFoundException;
import com.manuelfabri.expenses.model.Account;
import com.manuelfabri.expenses.model.Category;
import com.manuelfabri.expenses.model.CurrencyEnum;
import com.manuelfabri.expenses.model.RecurrenceFrequencyEnum;
import com.manuelfabri.expenses.model.RecurrenceStatusEnum;
import com.manuelfabri.expenses.model.RecurrentTransaction;
import com.manuelfabri.expenses.model.Subcategory;
import com.manuelfabri.expenses.model.TransactionTypeEnum;
import com.manuelfabri.expenses.model.User;
import com.manuelfabri.expenses.repository.AccountRepository;
import com.manuelfabri.expenses.repository.CategoryRepository;
import com.manuelfabri.expenses.repository.RecurrentTransactionRepository;
import com.manuelfabri.expenses.repository.SubcategoryRepository;
import com.manuelfabri.expenses.service.RecurrenceDateCalculator;
import com.manuelfabri.expenses.service.RecurrentTransactionGeneratorService;

@ExtendWith(MockitoExtension.class)
class RecurrentTransactionServiceImplementationTest {

  @Mock
  private RecurrentTransactionRepository recurrentTransactionRepository;
  @Mock
  private AccountRepository accountRepository;
  @Mock
  private CategoryRepository categoryRepository;
  @Mock
  private SubcategoryRepository subcategoryRepository;
  @Mock
  private RecurrenceDateCalculator dateCalculator;
  @Mock
  private RecurrentTransactionGeneratorService generatorService;

  private RecurrentTransactionServiceImplementation service;
  private User currentUser;
  private Account account;
  private Category category;
  private Subcategory subcategory;

  @BeforeEach
  void setUp() {
    ModelMapper mapper = new ModelMapper();
    mapper.getConfiguration().setFieldMatchingEnabled(true).setFieldAccessLevel(AccessLevel.PRIVATE)
        .setSkipNullEnabled(true);

    service = new RecurrentTransactionServiceImplementation(recurrentTransactionRepository, accountRepository,
        categoryRepository, subcategoryRepository, dateCalculator, generatorService, mapper);

    currentUser = new User("owner-1", "owner@example.com", "owner", "Owner", "One", List.of());
    account = new Account(10L, "Checking", CurrencyEnum.USD, currentUser);
    category = new Category(20L, "Subscriptions", "icon", "#fff", currentUser, List.of());
    category.setType(TransactionTypeEnum.EXPENSE);
    subcategory = new Subcategory(30L, "Streaming", category, List.of(), currentUser);

    SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
    securityContext.setAuthentication(new UsernamePasswordAuthenticationToken(currentUser, null));
    SecurityContextHolder.setContext(securityContext);
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private RecurrentTransactionRequestDto intervalRequestDto() {
    RecurrentTransactionRequestDto requestDto = new RecurrentTransactionRequestDto();
    requestDto.setType(TransactionTypeEnum.EXPENSE);
    requestDto.setAmount(new BigDecimal("9.99"));
    requestDto.setDescription("Streaming subscription");
    requestDto.setAccountId(account.getId());
    requestDto.setCategoryId(category.getId());
    requestDto.setSubcategoryId(subcategory.getId());
    requestDto.setFrequency(RecurrenceFrequencyEnum.INTERVAL_DAYS);
    requestDto.setIntervalDays(30);
    requestDto.setStartDate(OffsetDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC));
    return requestDto;
  }

  private void stubRelatedEntities() {
    when(categoryRepository.findActiveById(category.getId())).thenReturn(Optional.of(category));
    when(subcategoryRepository.findActiveById(subcategory.getId())).thenReturn(Optional.of(subcategory));
    when(accountRepository.findActiveById(account.getId())).thenReturn(Optional.of(account));
  }

  @Test
  void create_rejectsTransferType() {
    RecurrentTransactionRequestDto requestDto = intervalRequestDto();
    requestDto.setType(TransactionTypeEnum.TRANSFER);

    assertThatThrownBy(() -> service.create(requestDto)).isInstanceOf(IllegalArgumentException.class)
        .hasMessage("UNSUPPORTED_RECURRENCE_TYPE");
  }

  @Test
  void create_rejectsIntervalFrequency_withoutIntervalDays() {
    RecurrentTransactionRequestDto requestDto = intervalRequestDto();
    requestDto.setIntervalDays(null);

    assertThatThrownBy(() -> service.create(requestDto)).isInstanceOf(IllegalArgumentException.class)
        .hasMessage("MISSING_INTERVAL_DAYS");
  }

  @Test
  void create_rejectsIntervalFrequency_withNonPositiveIntervalDays() {
    RecurrentTransactionRequestDto requestDto = intervalRequestDto();
    requestDto.setIntervalDays(0);

    assertThatThrownBy(() -> service.create(requestDto)).isInstanceOf(IllegalArgumentException.class)
        .hasMessage("MISSING_INTERVAL_DAYS");
  }

  @Test
  void create_rejectsMonthlyFrequency_withoutDaysOfMonth() {
    RecurrentTransactionRequestDto requestDto = intervalRequestDto();
    requestDto.setFrequency(RecurrenceFrequencyEnum.MONTHLY_DAYS);
    requestDto.setDaysOfMonth(Set.of());

    assertThatThrownBy(() -> service.create(requestDto)).isInstanceOf(IllegalArgumentException.class)
        .hasMessage("MISSING_DAYS_OF_MONTH");
  }

  @Test
  void create_rejectsMonthlyFrequency_withDayOutOfRange() {
    RecurrentTransactionRequestDto requestDto = intervalRequestDto();
    requestDto.setFrequency(RecurrenceFrequencyEnum.MONTHLY_DAYS);
    requestDto.setDaysOfMonth(Set.of(32));

    assertThatThrownBy(() -> service.create(requestDto)).isInstanceOf(IllegalArgumentException.class)
        .hasMessage("INVALID_DAY_OF_MONTH");
  }

  @Test
  void create_throwsResourceNotFound_whenCategoryMissing() {
    RecurrentTransactionRequestDto requestDto = intervalRequestDto();
    when(categoryRepository.findActiveById(category.getId())).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.create(requestDto)).isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void create_throwsIllegalArgument_whenSubcategoryBelongsToADifferentCategory() {
    Category otherCategory = new Category(99L, "Other", "icon", "#000", currentUser, List.of());
    Subcategory mismatchedSubcategory = new Subcategory(30L, "Streaming", otherCategory, List.of(), currentUser);
    RecurrentTransactionRequestDto requestDto = intervalRequestDto();
    when(categoryRepository.findActiveById(category.getId())).thenReturn(Optional.of(category));
    when(subcategoryRepository.findActiveById(subcategory.getId())).thenReturn(Optional.of(mismatchedSubcategory));

    assertThatThrownBy(() -> service.create(requestDto)).isInstanceOf(IllegalArgumentException.class)
        .hasMessage("SUBCATEGORY_NOT_BELONG_TO_CATEGORY");
  }

  @Test
  void create_throwsResourceNotFound_whenAccountMissing() {
    RecurrentTransactionRequestDto requestDto = intervalRequestDto();
    when(categoryRepository.findActiveById(category.getId())).thenReturn(Optional.of(category));
    when(subcategoryRepository.findActiveById(subcategory.getId())).thenReturn(Optional.of(subcategory));
    when(accountRepository.findActiveById(account.getId())).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.create(requestDto)).isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void create_savesActiveRecurrenceOwnedByCurrentUser_forIntervalFrequency() {
    RecurrentTransactionRequestDto requestDto = intervalRequestDto();
    stubRelatedEntities();
    when(dateCalculator.nextDueDate(any())).thenReturn(Optional.empty());
    when(recurrentTransactionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    RecurrentTransactionDto result = service.create(requestDto);

    assertThat(result.getStatus()).isEqualTo(RecurrenceStatusEnum.ACTIVE);
    assertThat(result.getAmount()).isEqualByComparingTo("9.99");
    assertThat(result.getFrequency()).isEqualTo(RecurrenceFrequencyEnum.INTERVAL_DAYS);
    assertThat(result.getIntervalDays()).isEqualTo(30);
    assertThat(result.getDaysOfMonth()).isEmpty();
    assertThat(result.getAccountId()).isEqualTo(account.getId());
    assertThat(result.getCategory().getId()).isEqualTo(category.getId());
    assertThat(result.getSubcategory().getId()).isEqualTo(subcategory.getId());
  }

  @Test
  void create_clearsIntervalDaysAndKeepsDaysOfMonth_forMonthlyFrequency() {
    RecurrentTransactionRequestDto requestDto = intervalRequestDto();
    requestDto.setFrequency(RecurrenceFrequencyEnum.MONTHLY_DAYS);
    requestDto.setIntervalDays(null);
    requestDto.setDaysOfMonth(Set.of(1, 15));
    stubRelatedEntities();
    when(dateCalculator.nextDueDate(any())).thenReturn(Optional.empty());
    when(recurrentTransactionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    RecurrentTransactionDto result = service.create(requestDto);

    assertThat(result.getIntervalDays()).isNull();
    assertThat(result.getDaysOfMonth()).containsExactlyInAnyOrder(1, 15);
  }

  @Test
  void create_immediatelyAsksTheGeneratorToBackfillTodaysOccurrence() {
    RecurrentTransactionRequestDto requestDto = intervalRequestDto();
    stubRelatedEntities();
    when(dateCalculator.nextDueDate(any())).thenReturn(Optional.empty());
    when(recurrentTransactionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    service.create(requestDto);

    ArgumentCaptor<RecurrentTransaction> recurrenceCaptor = ArgumentCaptor.forClass(RecurrentTransaction.class);
    verify(generatorService).generateForRecurrence(recurrenceCaptor.capture(), eq(LocalDate.now(ZoneOffset.UTC)));
    assertThat(recurrenceCaptor.getValue().getDescription()).isEqualTo("Streaming subscription");
  }

  @Test
  void update_doesNotAskTheGeneratorToBackfillAnything() {
    RecurrentTransaction existingRecurrence = new RecurrentTransaction();
    existingRecurrence.setId(1L);
    existingRecurrence.setOwner(currentUser);
    existingRecurrence.setStatus(RecurrenceStatusEnum.ACTIVE);
    when(recurrentTransactionRepository.findActiveById(1L)).thenReturn(Optional.of(existingRecurrence));
    RecurrentTransactionRequestDto requestDto = intervalRequestDto();
    stubRelatedEntities();
    when(dateCalculator.nextDueDate(any())).thenReturn(Optional.empty());
    when(recurrentTransactionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    service.update(1L, requestDto);

    verifyNoInteractions(generatorService);
  }

  @Test
  void update_throwsResourceNotFound_whenRecurrenceMissing() {
    RecurrentTransactionRequestDto requestDto = intervalRequestDto();
    when(recurrentTransactionRepository.findActiveById(1L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.update(1L, requestDto)).isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void update_appliesNewFieldsToExistingRecurrence_andKeepsItsStatus() {
    RecurrentTransaction existingRecurrence = new RecurrentTransaction();
    existingRecurrence.setId(1L);
    existingRecurrence.setOwner(currentUser);
    existingRecurrence.setStatus(RecurrenceStatusEnum.PAUSED);
    when(recurrentTransactionRepository.findActiveById(1L)).thenReturn(Optional.of(existingRecurrence));
    RecurrentTransactionRequestDto requestDto = intervalRequestDto();
    requestDto.setDescription("Updated description");
    stubRelatedEntities();
    when(dateCalculator.nextDueDate(any())).thenReturn(Optional.empty());
    when(recurrentTransactionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    RecurrentTransactionDto result = service.update(1L, requestDto);

    assertThat(result.getDescription()).isEqualTo("Updated description");
    assertThat(result.getStatus()).isEqualTo(RecurrenceStatusEnum.PAUSED);
  }

  @Test
  void pause_setsStatusToPaused() {
    RecurrentTransaction existingRecurrence = new RecurrentTransaction();
    existingRecurrence.setId(1L);
    existingRecurrence.setStatus(RecurrenceStatusEnum.ACTIVE);
    when(recurrentTransactionRepository.findActiveById(1L)).thenReturn(Optional.of(existingRecurrence));
    when(dateCalculator.nextDueDate(any())).thenReturn(Optional.empty());
    when(recurrentTransactionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    RecurrentTransactionDto result = service.pause(1L);

    assertThat(result.getStatus()).isEqualTo(RecurrenceStatusEnum.PAUSED);
  }

  @Test
  void resume_setsStatusToActive() {
    RecurrentTransaction existingRecurrence = new RecurrentTransaction();
    existingRecurrence.setId(1L);
    existingRecurrence.setStatus(RecurrenceStatusEnum.PAUSED);
    when(recurrentTransactionRepository.findActiveById(1L)).thenReturn(Optional.of(existingRecurrence));
    when(dateCalculator.nextDueDate(any())).thenReturn(Optional.empty());
    when(recurrentTransactionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    RecurrentTransactionDto result = service.resume(1L);

    assertThat(result.getStatus()).isEqualTo(RecurrenceStatusEnum.ACTIVE);
  }

  @Test
  void cancel_setsStatusToCancelled() {
    RecurrentTransaction existingRecurrence = new RecurrentTransaction();
    existingRecurrence.setId(1L);
    existingRecurrence.setStatus(RecurrenceStatusEnum.ACTIVE);
    when(recurrentTransactionRepository.findActiveById(1L)).thenReturn(Optional.of(existingRecurrence));
    when(dateCalculator.nextDueDate(any())).thenReturn(Optional.empty());
    when(recurrentTransactionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    RecurrentTransactionDto result = service.cancel(1L);

    assertThat(result.getStatus()).isEqualTo(RecurrenceStatusEnum.CANCELLED);
  }

  @Test
  void pauseResumeCancel_throwResourceNotFound_whenRecurrenceMissing() {
    when(recurrentTransactionRepository.findActiveById(1L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.pause(1L)).isInstanceOf(ResourceNotFoundException.class);
    assertThatThrownBy(() -> service.resume(1L)).isInstanceOf(ResourceNotFoundException.class);
    assertThatThrownBy(() -> service.cancel(1L)).isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void delete_softDeletesTheRecurrence_ratherThanHardDeleting() {
    RecurrentTransaction existingRecurrence = new RecurrentTransaction();
    existingRecurrence.setId(1L);
    when(recurrentTransactionRepository.findActiveById(1L)).thenReturn(Optional.of(existingRecurrence));

    service.delete(1L);

    verify(recurrentTransactionRepository).softDelete(existingRecurrence);
    verify(recurrentTransactionRepository, never()).delete(any(RecurrentTransaction.class));
  }

  @Test
  void delete_throwsResourceNotFound_whenRecurrenceMissing() {
    when(recurrentTransactionRepository.findActiveById(1L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.delete(1L)).isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void getById_throwsResourceNotFound_whenRecurrenceMissing() {
    when(recurrentTransactionRepository.findActiveById(1L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.getById(1L)).isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void getById_populatesNextDueDate_whenCalculatorReturnsOne() {
    RecurrentTransaction existingRecurrence = new RecurrentTransaction();
    existingRecurrence.setId(1L);
    existingRecurrence.setStartDate(OffsetDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC));
    when(recurrentTransactionRepository.findActiveById(1L)).thenReturn(Optional.of(existingRecurrence));
    when(dateCalculator.nextDueDate(existingRecurrence)).thenReturn(Optional.of(LocalDate.of(2024, 2, 1)));

    RecurrentTransactionDto result = service.getById(1L);

    assertThat(result.getNextDueDate()).isEqualTo(OffsetDateTime.of(2024, 2, 1, 0, 0, 0, 0, ZoneOffset.UTC));
  }

  @Test
  void getById_leavesNextDueDateNull_whenCalculatorReturnsEmpty() {
    RecurrentTransaction existingRecurrence = new RecurrentTransaction();
    existingRecurrence.setId(1L);
    existingRecurrence.setStartDate(OffsetDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC));
    when(recurrentTransactionRepository.findActiveById(1L)).thenReturn(Optional.of(existingRecurrence));
    when(dateCalculator.nextDueDate(existingRecurrence)).thenReturn(Optional.empty());

    RecurrentTransactionDto result = service.getById(1L);

    assertThat(result.getNextDueDate()).isNull();
  }

  @Test
  void getActiveForCurrentUser_mapsEveryVisibleRecurrence() {
    RecurrentTransaction firstRecurrence = new RecurrentTransaction();
    firstRecurrence.setId(1L);
    firstRecurrence.setStartDate(OffsetDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC));
    RecurrentTransaction secondRecurrence = new RecurrentTransaction();
    secondRecurrence.setId(2L);
    secondRecurrence.setStartDate(OffsetDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC));
    when(recurrentTransactionRepository.findActiveVisible()).thenReturn(List.of(firstRecurrence, secondRecurrence));
    when(dateCalculator.nextDueDate(any())).thenReturn(Optional.empty());

    List<RecurrentTransactionDto> results = service.getActiveForCurrentUser();

    assertThat(results).extracting(RecurrentTransactionDto::getId).containsExactly(1L, 2L);
  }
}
