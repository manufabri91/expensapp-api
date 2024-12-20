package com.manuelfabri.expenses.service.implementation;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;
import org.modelmapper.ModelMapper;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import com.manuelfabri.expenses.dto.TransactionRequestDto;
import com.manuelfabri.expenses.dto.TransactionDto;
import com.manuelfabri.expenses.exception.ResourceNotFoundException;
import com.manuelfabri.expenses.model.Account;
import com.manuelfabri.expenses.model.Category;
import com.manuelfabri.expenses.model.Subcategory;
import com.manuelfabri.expenses.model.Transaction;
import com.manuelfabri.expenses.model.User;
import com.manuelfabri.expenses.repository.AccountRepository;
import com.manuelfabri.expenses.repository.CategoryRepository;
import com.manuelfabri.expenses.repository.SubcategoryRepository;
import com.manuelfabri.expenses.repository.TransactionRepository;
import com.manuelfabri.expenses.service.TransactionService;

@Service
public class TransactionServiceImplementation implements TransactionService {
  private TransactionRepository transactionRepository;
  private CategoryRepository categoryRepository;
  private AccountRepository accountRepository;
  private SubcategoryRepository subcategoryRepository;
  private ModelMapper mapper;

  public TransactionServiceImplementation(TransactionRepository transactionRepository,
      CategoryRepository categoryRepository, SubcategoryRepository subcategoryRepository,
      AccountRepository accountRepository, ModelMapper mapper) {
    this.transactionRepository = transactionRepository;
    this.accountRepository = accountRepository;
    this.categoryRepository = categoryRepository;
    this.subcategoryRepository = subcategoryRepository;
    this.mapper = mapper;
  }


  @Override
  public TransactionDto createTransaction(TransactionRequestDto transactionDto) {
    User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

    Account transactionAccount = accountRepository.findActiveById(transactionDto.getAccountId())
        .orElseThrow(() -> new ResourceNotFoundException("Account", "id", transactionDto.getAccountId().toString()));

    Category transactionCategory = categoryRepository.findActiveById(transactionDto.getCategoryId())
        .orElseThrow(() -> new ResourceNotFoundException("Category", "id", transactionDto.getCategoryId().toString()));

    Subcategory transactionSubcategory =
        subcategoryRepository.findActiveById(transactionDto.getSubcategoryId()).orElse(null);

    if (transactionSubcategory != null && !transactionSubcategory.getParentCategory().equals(transactionCategory)) {
      throw new IllegalArgumentException("Subcategory does not belong to the provided category.");
    }

    Transaction transaction = mapper.map(transactionDto, Transaction.class);
    transaction.setOwner(user);
    transaction.setId(null); // TODO: Revisar como hacer para que el mapper lo deje nulo
    transaction.setAccount(transactionAccount);
    transaction.setCategory(transactionCategory);
    transaction.setSubcategory(transactionSubcategory);

    Transaction newTransaction = this.transactionRepository.save(transaction);

    return mapper.map(newTransaction, TransactionDto.class);
  }

  @Override
  public List<TransactionDto> getAllTransactions() {
    return this.transactionRepository.findActive().stream()
        .map(transaction -> mapper.map(transaction, TransactionDto.class)).collect(Collectors.toList());

  }

  @Override
  public void deleteTransaction(Long id) {
    Transaction transaction = this.transactionRepository.findActiveById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Transaction", "id", id.toString()));
    this.transactionRepository.delete(transaction);
  }

  @Override
  public TransactionDto getById(Long id) {
    return this.transactionRepository.findActiveById(id).map((tx) -> mapper.map(tx, TransactionDto.class))
        .orElseThrow(() -> new ResourceNotFoundException("Transaction", "id", id.toString()));
  }


  @Override
  public List<TransactionDto> getTransactionsByAccountId(Long id) {
    User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    Account account = this.accountRepository.findActiveById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Account", "id", id.toString()));
    return this.transactionRepository.findByOwnerAndAccountAndDeletedFalse(user, account).stream()
        .map(transaction -> mapper.map(transaction, TransactionDto.class)).collect(Collectors.toList());
  }


  @Override
  public List<TransactionDto> getTransactionsByCategoryId(Long id) {
    User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    Category category = this.categoryRepository.findActiveById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id.toString()));
    return this.transactionRepository.findByOwnerAndCategoryAndDeletedFalse(user, category).stream()
        .map(transaction -> mapper.map(transaction, TransactionDto.class)).collect(Collectors.toList());
  }

  @Override
  public List<TransactionDto> getTransactionsBySubcategoryId(Long id) {
    User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    Subcategory subcategory = this.subcategoryRepository.findActiveById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id.toString()));
    return this.transactionRepository.findByOwnerAndSubcategoryAndDeletedFalse(user, subcategory).stream()
        .map(transaction -> mapper.map(transaction, TransactionDto.class)).collect(Collectors.toList());
  }


  @Override
  public List<TransactionDto> getMonthlyTransactions(int year, int month) {
    User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    OffsetDateTime startDate = OffsetDateTime.of(year, month, 1, 0, 0, 0, 0, ZoneOffset.UTC);
    OffsetDateTime endDate = startDate.plusMonths(1).minusSeconds(1);
    return this.transactionRepository.findByOwnerAndEventDateBetweenAndDeletedFalse(user, startDate, endDate).stream()
        .map(transaction -> mapper.map(transaction, TransactionDto.class)).collect(Collectors.toList());
  }



}
