package com.manuelfabri.expenses.service.implementation;

import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import org.modelmapper.ModelMapper;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.manuelfabri.expenses.dto.RecurrentTransactionDto;
import com.manuelfabri.expenses.dto.RecurrentTransactionRequestDto;
import com.manuelfabri.expenses.exception.ResourceNotFoundException;
import com.manuelfabri.expenses.model.Account;
import com.manuelfabri.expenses.model.Category;
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
import com.manuelfabri.expenses.service.RecurrentTransactionService;
import com.manuelfabri.expenses.service.TransactionRelatedEntities;

@Service
public class RecurrentTransactionServiceImplementation implements RecurrentTransactionService {
  private RecurrentTransactionRepository recurrentTransactionRepository;
  private AccountRepository accountRepository;
  private CategoryRepository categoryRepository;
  private SubcategoryRepository subcategoryRepository;
  private RecurrenceDateCalculator dateCalculator;
  private ModelMapper mapper;

  public RecurrentTransactionServiceImplementation(RecurrentTransactionRepository recurrentTransactionRepository,
      AccountRepository accountRepository, CategoryRepository categoryRepository,
      SubcategoryRepository subcategoryRepository, RecurrenceDateCalculator dateCalculator, ModelMapper mapper) {
    this.recurrentTransactionRepository = recurrentTransactionRepository;
    this.accountRepository = accountRepository;
    this.categoryRepository = categoryRepository;
    this.subcategoryRepository = subcategoryRepository;
    this.dateCalculator = dateCalculator;
    this.mapper = mapper;
  }

  private void validate(RecurrentTransactionRequestDto requestDto) {
    if (requestDto.getType() == TransactionTypeEnum.TRANSFER) {
      throw new IllegalArgumentException("UNSUPPORTED_RECURRENCE_TYPE");
    }

    if (requestDto.getFrequency() == RecurrenceFrequencyEnum.INTERVAL_DAYS) {
      if (requestDto.getIntervalDays() == null || requestDto.getIntervalDays() <= 0) {
        throw new IllegalArgumentException("MISSING_INTERVAL_DAYS");
      }
    } else {
      if (requestDto.getDaysOfMonth() == null || requestDto.getDaysOfMonth().isEmpty()) {
        throw new IllegalArgumentException("MISSING_DAYS_OF_MONTH");
      }
      if (requestDto.getDaysOfMonth().stream().anyMatch(day -> day < 1 || day > 31)) {
        throw new IllegalArgumentException("INVALID_DAY_OF_MONTH");
      }
    }
  }

  private TransactionRelatedEntities getRelatedEntities(RecurrentTransactionRequestDto requestDto) {
    Category category = categoryRepository.findActiveById(requestDto.getCategoryId())
        .orElseThrow(() -> new ResourceNotFoundException("Category", "id", requestDto.getCategoryId().toString()));
    Subcategory subcategory = subcategoryRepository.findActiveById(requestDto.getSubcategoryId()).orElse(null);
    if (subcategory != null && !subcategory.getParentCategory().equals(category)) {
      throw new IllegalArgumentException("SUBCATEGORY_NOT_BELONG_TO_CATEGORY");
    }
    Account account = accountRepository.findActiveById(requestDto.getAccountId())
        .orElseThrow(() -> new ResourceNotFoundException("Account", "id", requestDto.getAccountId().toString()));

    return new TransactionRelatedEntities(account, category, subcategory);
  }

  private RecurrentTransactionDto toDto(RecurrentTransaction recurrence) {
    RecurrentTransactionDto dto = mapper.map(recurrence, RecurrentTransactionDto.class);
    dto.setNextDueDate(dateCalculator.nextDueDate(recurrence)
        .map(date -> date.atStartOfDay().atOffset(recurrence.getStartDate().getOffset())).orElse(null));
    return dto;
  }

  @Override
  public List<RecurrentTransactionDto> getActiveForCurrentUser() {
    return recurrentTransactionRepository.findActiveVisible().stream().map(this::toDto).collect(Collectors.toList());
  }

  @Override
  public RecurrentTransactionDto getById(Long id) {
    return recurrentTransactionRepository.findActiveById(id).map(this::toDto)
        .orElseThrow(() -> new ResourceNotFoundException("RecurrentTransaction", "id", id.toString()));
  }

  @Override
  @Transactional
  public RecurrentTransactionDto create(RecurrentTransactionRequestDto requestDto) {
    validate(requestDto);
    User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    TransactionRelatedEntities entities = getRelatedEntities(requestDto);

    RecurrentTransaction recurrence = new RecurrentTransaction();
    recurrence.setOwner(user);
    recurrence.setStatus(RecurrenceStatusEnum.ACTIVE);
    applyRequestToEntity(recurrence, requestDto, entities);

    RecurrentTransaction saved = recurrentTransactionRepository.save(recurrence);
    return toDto(saved);
  }

  @Override
  @Transactional
  public RecurrentTransactionDto update(Long id, RecurrentTransactionRequestDto requestDto) {
    validate(requestDto);
    RecurrentTransaction recurrence = recurrentTransactionRepository.findActiveById(id)
        .orElseThrow(() -> new ResourceNotFoundException("RecurrentTransaction", "id", id.toString()));
    TransactionRelatedEntities entities = getRelatedEntities(requestDto);

    applyRequestToEntity(recurrence, requestDto, entities);

    RecurrentTransaction saved = recurrentTransactionRepository.save(recurrence);
    return toDto(saved);
  }

  private void applyRequestToEntity(RecurrentTransaction recurrence, RecurrentTransactionRequestDto requestDto,
      TransactionRelatedEntities entities) {
    recurrence.setType(requestDto.getType());
    recurrence.setAmount(requestDto.getAmount().abs());
    recurrence.setDescription(requestDto.getDescription());
    recurrence.setAccount(entities.account());
    recurrence.setCategory(entities.category());
    recurrence.setSubcategory(entities.subcategory());
    recurrence.setFrequency(requestDto.getFrequency());
    recurrence.setIntervalDays(
        requestDto.getFrequency() == RecurrenceFrequencyEnum.INTERVAL_DAYS ? requestDto.getIntervalDays() : null);
    recurrence.setDaysOfMonth(requestDto.getFrequency() == RecurrenceFrequencyEnum.MONTHLY_DAYS
        ? new HashSet<>(requestDto.getDaysOfMonth()) : new HashSet<>());
    recurrence.setStartDate(requestDto.getStartDate());
    recurrence.setEndDate(requestDto.getEndDate());
    recurrence.setExcludeFromTotals(requestDto.isExcludeFromTotals());
  }

  private RecurrentTransaction findOwnedById(Long id) {
    return recurrentTransactionRepository.findActiveById(id)
        .orElseThrow(() -> new ResourceNotFoundException("RecurrentTransaction", "id", id.toString()));
  }

  @Override
  @Transactional
  public RecurrentTransactionDto pause(Long id) {
    RecurrentTransaction recurrence = findOwnedById(id);
    recurrence.setStatus(RecurrenceStatusEnum.PAUSED);
    return toDto(recurrentTransactionRepository.save(recurrence));
  }

  @Override
  @Transactional
  public RecurrentTransactionDto resume(Long id) {
    RecurrentTransaction recurrence = findOwnedById(id);
    recurrence.setStatus(RecurrenceStatusEnum.ACTIVE);
    return toDto(recurrentTransactionRepository.save(recurrence));
  }

  @Override
  @Transactional
  public RecurrentTransactionDto cancel(Long id) {
    RecurrentTransaction recurrence = findOwnedById(id);
    recurrence.setStatus(RecurrenceStatusEnum.CANCELLED);
    return toDto(recurrentTransactionRepository.save(recurrence));
  }

  @Override
  @Transactional
  public void delete(Long id) {
    RecurrentTransaction recurrence = findOwnedById(id);
    recurrentTransactionRepository.softDelete(recurrence);
  }
}
