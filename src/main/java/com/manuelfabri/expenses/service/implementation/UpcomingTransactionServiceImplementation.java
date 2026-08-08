package com.manuelfabri.expenses.service.implementation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import com.manuelfabri.expenses.dto.ProgrammedTransactionsDto;
import com.manuelfabri.expenses.dto.UpcomingTransactionGroupDto;
import com.manuelfabri.expenses.dto.UpcomingTransactionItemDto;
import com.manuelfabri.expenses.dto.UpcomingTransactionsResponseDto;
import com.manuelfabri.expenses.model.CurrencyEnum;
import com.manuelfabri.expenses.model.RecurrenceStatusEnum;
import com.manuelfabri.expenses.model.RecurrentTransaction;
import com.manuelfabri.expenses.model.SourceTypeEnum;
import com.manuelfabri.expenses.model.Transaction;
import com.manuelfabri.expenses.model.TransactionTypeEnum;
import com.manuelfabri.expenses.model.User;
import com.manuelfabri.expenses.repository.RecurrentTransactionRepository;
import com.manuelfabri.expenses.repository.TransactionRepository;
import com.manuelfabri.expenses.service.RecurrenceDateCalculator;
import com.manuelfabri.expenses.service.UpcomingTransactionService;

@Service
public class UpcomingTransactionServiceImplementation implements UpcomingTransactionService {
  private RecurrentTransactionRepository recurrentTransactionRepository;
  private TransactionRepository transactionRepository;
  private RecurrenceDateCalculator dateCalculator;

  public UpcomingTransactionServiceImplementation(RecurrentTransactionRepository recurrentTransactionRepository,
      TransactionRepository transactionRepository, RecurrenceDateCalculator dateCalculator) {
    this.recurrentTransactionRepository = recurrentTransactionRepository;
    this.transactionRepository = transactionRepository;
    this.dateCalculator = dateCalculator;
  }

  private UpcomingTransactionItemDto fromRecurrence(RecurrentTransaction recurrence, Optional<LocalDate> dueDate) {
    UpcomingTransactionItemDto dto = new UpcomingTransactionItemDto();
    dto.setSourceType(SourceTypeEnum.RECURRING);
    dto.setSourceId(recurrence.getId());
    dto.setDate(dueDate.map(date -> date.atStartOfDay().atOffset(recurrence.getStartDate().getOffset())).orElse(null));
    dto.setDescription(recurrence.getDescription());
    dto.setCategoryIconName(recurrence.getCategory().getIconName());
    dto.setCategoryColor(recurrence.getCategory().getColor());
    dto.setAccountId(recurrence.getAccount().getId());
    dto.setSignedAmount(recurrence.getType().applySign(recurrence.getAmount()));
    dto.setFrequency(recurrence.getFrequency());
    dto.setIntervalDays(recurrence.getIntervalDays());
    dto.setDaysOfMonth(recurrence.getDaysOfMonth());
    return dto;
  }

  private UpcomingTransactionItemDto fromTransaction(Transaction transaction) {
    UpcomingTransactionItemDto dto = new UpcomingTransactionItemDto();
    dto.setSourceType(SourceTypeEnum.ONE_TIME);
    dto.setSourceId(transaction.getId());
    dto.setDate(transaction.getEventDate());
    dto.setDescription(transaction.getDescription());
    dto.setCategoryIconName(transaction.getCategory().getIconName());
    dto.setCategoryColor(transaction.getCategory().getColor());
    dto.setAccountId(transaction.getAccount().getId());
    dto.setSignedAmount(transaction.getAmount());
    return dto; // frequency/intervalDays/daysOfMonth left null
  }

  /**
   * Routes an item into the expense or income bucket for its type. Transfers (which shouldn't reach this service in
   * practice — recurrences can't be TRANSFER, and one-time transfers are never pending) are intentionally dropped
   * rather than crashing, so a mixed input never leaks a transfer into either bucket.
   */
  private void bucket(TransactionTypeEnum type, UpcomingTransactionItemDto dto,
      List<UpcomingTransactionItemDto> expenses, List<UpcomingTransactionItemDto> incomes) {
    if (type == TransactionTypeEnum.EXPENSE) {
      expenses.add(dto);
    } else if (type == TransactionTypeEnum.INCOME) {
      incomes.add(dto);
    }
  }

  /**
   * Same routing as {@link #bucket}, but only materializes a currency's list in the map the item actually belongs
   * to — computing both maps unconditionally would leave a spurious empty group in the other bucket.
   */
  private void bucketByCurrency(TransactionTypeEnum type, CurrencyEnum currency, UpcomingTransactionItemDto dto,
      Map<CurrencyEnum, List<UpcomingTransactionItemDto>> expensesByCurrency,
      Map<CurrencyEnum, List<UpcomingTransactionItemDto>> incomesByCurrency) {
    if (type == TransactionTypeEnum.EXPENSE) {
      expensesByCurrency.computeIfAbsent(currency, c -> new ArrayList<>()).add(dto);
    } else if (type == TransactionTypeEnum.INCOME) {
      incomesByCurrency.computeIfAbsent(currency, c -> new ArrayList<>()).add(dto);
    }
  }

  private List<UpcomingTransactionGroupDto> groupByCurrency(
      Map<CurrencyEnum, List<UpcomingTransactionItemDto>> byCurrency) {
    List<UpcomingTransactionGroupDto> groups = new ArrayList<>();
    for (Map.Entry<CurrencyEnum, List<UpcomingTransactionItemDto>> entry : byCurrency.entrySet()) {
      List<UpcomingTransactionItemDto> items = new ArrayList<>(entry.getValue());
      items.sort(Comparator.comparing(UpcomingTransactionItemDto::getDate, Comparator.nullsLast(Comparator.naturalOrder())));

      BigDecimal total = items.stream().map(UpcomingTransactionItemDto::getSignedAmount).reduce(BigDecimal.ZERO,
          BigDecimal::add);

      UpcomingTransactionGroupDto group = new UpcomingTransactionGroupDto();
      group.setCurrency(entry.getKey().name());
      group.setTotal(total);
      group.setItems(items);
      groups.add(group);
    }
    groups.sort(Comparator.comparing(UpcomingTransactionGroupDto::getCurrency));
    return groups;
  }

  @Override
  public UpcomingTransactionsResponseDto getUpcomingTransactions() {
    User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

    LocalDate today = OffsetDateTime.now(ZoneOffset.UTC).toLocalDate();
    LocalDate endOfMonth = YearMonth.from(today).atEndOfMonth();
    OffsetDateTime windowStart = OffsetDateTime.of(today, LocalTime.MIN, ZoneOffset.UTC);
    OffsetDateTime windowEnd = OffsetDateTime.of(endOfMonth, LocalTime.MAX, ZoneOffset.UTC);

    Map<CurrencyEnum, List<UpcomingTransactionItemDto>> expensesByCurrency = new LinkedHashMap<>();
    Map<CurrencyEnum, List<UpcomingTransactionItemDto>> incomesByCurrency = new LinkedHashMap<>();

    for (RecurrentTransaction recurrence : recurrentTransactionRepository.findActiveVisible()) {
      if (recurrence.getStatus() != RecurrenceStatusEnum.ACTIVE) {
        continue;
      }
      dateCalculator.nextDueDate(recurrence)
          .filter(dueDate -> !dueDate.isBefore(today) && !dueDate.isAfter(endOfMonth)).ifPresent(dueDate -> {
            UpcomingTransactionItemDto dto = fromRecurrence(recurrence, Optional.of(dueDate));
            CurrencyEnum currency = recurrence.getAccount().getCurrency();
            bucketByCurrency(recurrence.getType(), currency, dto, expensesByCurrency, incomesByCurrency);
          });
    }

    for (Transaction transaction : transactionRepository
        .findByOwnerAndPendingTrueAndEventDateBetweenAndDeletedFalse(user, windowStart, windowEnd)) {
      UpcomingTransactionItemDto dto = fromTransaction(transaction);
      CurrencyEnum currency = transaction.getAccount().getCurrency();
      bucketByCurrency(transaction.getType(), currency, dto, expensesByCurrency, incomesByCurrency);
    }

    UpcomingTransactionsResponseDto response = new UpcomingTransactionsResponseDto();
    response.setExpenses(groupByCurrency(expensesByCurrency));
    response.setIncomes(groupByCurrency(incomesByCurrency));
    return response;
  }

  @Override
  public ProgrammedTransactionsDto getProgrammedTransactions() {
    User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

    List<UpcomingTransactionItemDto> expenses = new ArrayList<>();
    List<UpcomingTransactionItemDto> incomes = new ArrayList<>();

    for (RecurrentTransaction recurrence : recurrentTransactionRepository.findActiveVisible()) {
      RecurrenceStatusEnum status = recurrence.getStatus();
      boolean isActiveOrPaused = status == RecurrenceStatusEnum.ACTIVE || status == RecurrenceStatusEnum.PAUSED;
      if (!isActiveOrPaused) {
        continue;
      }
      Optional<LocalDate> dueDate = dateCalculator.nextDueDate(recurrence);
      bucket(recurrence.getType(), fromRecurrence(recurrence, dueDate), expenses, incomes);
    }

    for (Transaction transaction : transactionRepository.findByOwnerAndPendingTrueAndDeletedFalse(user)) {
      bucket(transaction.getType(), fromTransaction(transaction), expenses, incomes);
    }

    expenses.sort(Comparator.comparing(UpcomingTransactionItemDto::getDate, Comparator.nullsLast(Comparator.naturalOrder())));
    incomes.sort(Comparator.comparing(UpcomingTransactionItemDto::getDate, Comparator.nullsLast(Comparator.naturalOrder())));

    ProgrammedTransactionsDto response = new ProgrammedTransactionsDto();
    response.setExpenses(expenses);
    response.setIncomes(incomes);
    return response;
  }
}
