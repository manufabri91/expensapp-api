package com.manuelfabri.expenses.service.implementation;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import com.manuelfabri.expenses.dto.TransactionRequestDto;
import com.manuelfabri.expenses.dto.TransactionDto;
import com.manuelfabri.expenses.exception.ResourceNotFoundException;
import com.manuelfabri.expenses.model.Account;
import com.manuelfabri.expenses.model.Category;
import com.manuelfabri.expenses.model.Subcategory;
import com.manuelfabri.expenses.model.Transaction;
import com.manuelfabri.expenses.model.TransactionTypeEnum;
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

  private static class EntityTriple {
    public final Account account;
    public final Category category;
    public final Subcategory subcategory;

    public EntityTriple(Account account, Category category, Subcategory subcategory) {
      this.account = account;
      this.category = category;
      this.subcategory = subcategory;
    }
  }

  private static class TransferCategories {
    public final Category category;
    public final Subcategory subcategoryIn;
    public final Subcategory subcategoryOut;

    public TransferCategories(Category category, Subcategory subcategoryIn, Subcategory subcategoryOut) {
      this.category = category;
      this.subcategoryIn = subcategoryIn;
      this.subcategoryOut = subcategoryOut;
    }
  }

  private EntityTriple getTransactionRelatedEntities(TransactionRequestDto transactionDto) {
    Long accountId = transactionDto.getAccountId();
    Long categoryId = transactionDto.getCategoryId();
    Long subcategoryId = transactionDto.getSubcategoryId();

    Category category = null;
    Subcategory subcategory = null;
    if (transactionDto.getType() != TransactionTypeEnum.TRANSFER) {
      if (categoryId == null) {
        throw new IllegalArgumentException("MISSING_ACCOUNT");
      }
      if (subcategoryId == null) {
        throw new IllegalArgumentException("MISSING_SUBCATEGORY");
      }
      category = categoryRepository.findActiveById(categoryId)
          .orElseThrow(() -> new ResourceNotFoundException("Category", "id", categoryId.toString()));
      subcategory = subcategoryRepository.findActiveById(subcategoryId).orElse(null);
      if (subcategory != null && !subcategory.getParentCategory().equals(category)) {
        throw new IllegalArgumentException("SUBCATEGORY_NOT_BELONG_TO_CATEGORY");
      }
    } else {
      TransferCategories transferCategories = getTransferCategory();
      category = transferCategories.category;
      subcategory = transferCategories.subcategoryOut;
    }

    Account account = accountRepository.findActiveById(accountId)
        .orElseThrow(() -> new ResourceNotFoundException("Account", "id", accountId.toString()));

    return new EntityTriple(account, category, subcategory);
  }

  private TransferCategories getTransferCategory() {
    Category transferCategory = categoryRepository.findActiveByNameAndIsSystem("CATEGORY.TRANSFER")
        .orElseThrow(() -> new ResourceNotFoundException("Category", "name", "CATEGORY.TRANSFER"));
    Subcategory transferInSubcategory = subcategoryRepository.findActiveByNameAndIsSystem("SUBCATEGORY.TRANSFER_IN")
        .orElseThrow(() -> new ResourceNotFoundException("Subcategory", "name", "SUBCATEGORY.TRANSFER_IN"));
    Subcategory transferOutSubcategory = subcategoryRepository.findActiveByNameAndIsSystem("SUBCATEGORY.TRANSFER_OUT")
        .orElseThrow(() -> new ResourceNotFoundException("Subcategory", "name", "SUBCATEGORY.TRANSFER_OUT"));

    return new TransferCategories(transferCategory, transferInSubcategory, transferOutSubcategory);
  }

  private Account getTransferAccount(Long destinationAccountId, Account originAccount) {
    if (destinationAccountId == null) {
      throw new IllegalArgumentException("MISSING_DESTINATION_ID");
    }

    if (destinationAccountId.equals(originAccount.getId())) {
      throw new IllegalArgumentException("DESTINATION_ACCOUNT_CANNOT_BE_SAME_AS_SOURCE");
    }

    Account destinationAccount = accountRepository.findActiveById(destinationAccountId)
        .orElseThrow(() -> new ResourceNotFoundException("Account", "id", destinationAccountId.toString()));

    if (destinationAccount.getCurrency() != originAccount.getCurrency()) {
      throw new IllegalArgumentException("CURRENCY_MISMATCH_BETWEEN_ACCOUNTS");
    }

    return destinationAccount;
  }

  private Transaction createTransferCounterpart(TransactionRequestDto transferSourceDto,
      EntityTriple relatedEntityTriple, Transaction linkTransaction) {
    User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    Account destinationAccount =
        getTransferAccount(transferSourceDto.getDestinationAccountId(), relatedEntityTriple.account);

    Transaction linked = mapper.map(transferSourceDto, Transaction.class);

    TransferCategories transferCategories = getTransferCategory();
    linked.setAmount(transferSourceDto.getAmount().abs());
    linked.setAccount(destinationAccount);
    linked.setOwner(user);
    linked.setCategory(relatedEntityTriple.category);
    linked.setSubcategory(transferCategories.subcategoryIn);
    linked.setLinkedTransaction(linkTransaction);
    linked.setExcludeFromTotals(true);
    return this.transactionRepository.save(linked);
  }

  @Override
  @Transactional
  public TransactionDto createTransaction(TransactionRequestDto transactionDto) {
    User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

    EntityTriple entities = getTransactionRelatedEntities(transactionDto);
    Account transactionAccount = entities.account;
    Category transactionCategory = entities.category;
    Subcategory transactionSubcategory = entities.subcategory;

    Transaction transaction = mapper.map(transactionDto, Transaction.class);

    var amount = transaction.getAmount().abs();
    if (transaction.getType() == TransactionTypeEnum.EXPENSE || transaction.getType() == TransactionTypeEnum.TRANSFER) {
      amount = amount.negate();
    }
    transaction.setAmount(amount);
    transaction.setOwner(user);
    transaction.setAccount(transactionAccount);
    transaction.setCategory(transactionCategory);
    transaction.setSubcategory(transactionSubcategory);

    Transaction newTransaction = this.transactionRepository.save(transaction);

    if (transactionDto.getType() == TransactionTypeEnum.TRANSFER) {
      Transaction newLinkedTransaction = createTransferCounterpart(transactionDto, entities, newTransaction);
      newTransaction.setLinkedTransaction(newLinkedTransaction);
      newTransaction.setExcludeFromTotals(true);
      this.transactionRepository.save(newTransaction);
    }

    return mapper.map(newTransaction, TransactionDto.class);
  }

  @Override
  public List<TransactionDto> getAllTransactions() {
    return this.transactionRepository.findActive().stream()
        .map(transaction -> mapper.map(transaction, TransactionDto.class)).collect(Collectors.toList());
  }

  @Override
  public Page<TransactionDto> getPagedTransactions(Pageable pageable) {
    return this.transactionRepository.findActivePaged(pageable)
        .map(transaction -> mapper.map(transaction, TransactionDto.class));
  }

  @Transactional
  @Override
  public TransactionDto updateTransaction(Long id, TransactionRequestDto transactionDto) {
    EntityTriple entities = getTransactionRelatedEntities(transactionDto);
    Account transactionAccount = entities.account;
    Category transactionCategory = entities.category;
    Subcategory transactionSubcategory = entities.subcategory;

    Transaction transaction = this.transactionRepository.findActiveById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Transaction", "id", id.toString()));

    transaction.setDescription(transactionDto.getDescription());
    transaction.setEventDate(transactionDto.getEventDate());
    transaction.setAmount(transactionDto.getAmount());
    transaction.setAccount(transactionAccount);
    transaction.setCategory(transactionCategory);
    transaction.setSubcategory(transactionSubcategory);
    transaction.setExcludeFromTotals(transactionDto.isExcludeFromTotals());

    TransactionTypeEnum currentType = transaction.getType();
    TransactionTypeEnum newType = transactionDto.getType();
    if (currentType != newType) {
      if (currentType == TransactionTypeEnum.TRANSFER) {
        this.transactionRepository.delete(transaction.getLinkedTransaction());
        transaction.setLinkedTransaction(null);
      } else if (newType == TransactionTypeEnum.TRANSFER) {
        Transaction linkedTransaction = createTransferCounterpart(transactionDto, entities, transaction);
        transaction.setLinkedTransaction(linkedTransaction);
        transaction.setExcludeFromTotals(true);
      }
      transaction.setType(transactionDto.getType());
    }

    Transaction updatedTransaction = this.transactionRepository.save(transaction);

    return mapper.map(updatedTransaction, TransactionDto.class);
  }

  @Transactional
  @Override
  public void deleteTransaction(Long id) {
    Transaction transaction = this.transactionRepository.findActiveById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Transaction", "id", id.toString()));

    if (transaction.getType() == TransactionTypeEnum.TRANSFER && transaction.getLinkedTransaction() != null) {
      this.transactionRepository.delete(transaction.getLinkedTransaction());
    }

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
