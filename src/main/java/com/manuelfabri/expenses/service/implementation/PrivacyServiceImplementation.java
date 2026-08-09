package com.manuelfabri.expenses.service.implementation;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import com.manuelfabri.expenses.dto.AccountDto;
import com.manuelfabri.expenses.dto.CategoryDto;
import com.manuelfabri.expenses.dto.RecurrentTransactionDto;
import com.manuelfabri.expenses.dto.SubcategoryDto;
import com.manuelfabri.expenses.dto.TransactionDto;
import com.manuelfabri.expenses.dto.UserDataExportDto;
import com.manuelfabri.expenses.dto.UserSettingsDto;
import com.manuelfabri.expenses.model.User;
import com.manuelfabri.expenses.repository.AccountRepository;
import com.manuelfabri.expenses.repository.CategoryRepository;
import com.manuelfabri.expenses.repository.RecurrentTransactionRepository;
import com.manuelfabri.expenses.repository.SubcategoryRepository;
import com.manuelfabri.expenses.repository.TransactionRepository;
import com.manuelfabri.expenses.repository.UserRepository;
import com.manuelfabri.expenses.repository.UserSettingsRepository;
import com.manuelfabri.expenses.service.FirebaseService;
import com.manuelfabri.expenses.service.PrivacyService;

@Service
public class PrivacyServiceImplementation implements PrivacyService {

  private static final Logger log = LoggerFactory.getLogger(PrivacyServiceImplementation.class);

  private AccountRepository accountRepository;
  private TransactionRepository transactionRepository;
  private CategoryRepository categoryRepository;
  private SubcategoryRepository subcategoryRepository;
  private RecurrentTransactionRepository recurrentTransactionRepository;
  private UserSettingsRepository userSettingsRepository;
  private UserRepository userRepository;
  private FirebaseService firebaseService;
  private ModelMapper mapper;
  // Runs the destructive deletion in its own transaction that commits before this method
  // continues, so the compensating Firebase deletion below only ever fires after the local data
  // is durably gone - mirroring (in reverse) the create-then-rollback pattern used for
  // registration in AuthServiceImplementation. A @Transactional annotation on this method would
  // not give that guarantee, since the transaction wouldn't commit until the whole method
  // (including the Firebase call) returns.
  private TransactionTemplate transactionTemplate;

  public PrivacyServiceImplementation(AccountRepository accountRepository,
      TransactionRepository transactionRepository, CategoryRepository categoryRepository,
      SubcategoryRepository subcategoryRepository, RecurrentTransactionRepository recurrentTransactionRepository,
      UserSettingsRepository userSettingsRepository, UserRepository userRepository, FirebaseService firebaseService,
      ModelMapper mapper, PlatformTransactionManager transactionManager) {
    this.accountRepository = accountRepository;
    this.transactionRepository = transactionRepository;
    this.categoryRepository = categoryRepository;
    this.subcategoryRepository = subcategoryRepository;
    this.recurrentTransactionRepository = recurrentTransactionRepository;
    this.userSettingsRepository = userSettingsRepository;
    this.userRepository = userRepository;
    this.firebaseService = firebaseService;
    this.mapper = mapper;
    this.transactionTemplate = new TransactionTemplate(transactionManager);
  }

  private User getCurrentUser() {
    return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
  }

  @Override
  public UserDataExportDto exportUserData() {
    User user = getCurrentUser();

    UserDataExportDto export = new UserDataExportDto();
    export.setId(user.getId());
    export.setEmail(user.getEmail());
    export.setUsername(user.getUsername());
    export.setFirstName(user.getFirstName());
    export.setLastName(user.getLastName());
    export.setTermsAcceptedAt(user.getTermsAcceptedAt());
    export.setTermsVersion(user.getTermsVersion());

    userSettingsRepository.findById(user.getId())
        .ifPresent(settings -> export.setSettings(mapper.map(settings, UserSettingsDto.class)));

    export.setAccounts(mapToDtoList(accountRepository.findByOwnerAndDeletedFalse(user), AccountDto.class));
    export.setTransactions(mapToDtoList(transactionRepository.findByOwnerAndDeletedFalse(user), TransactionDto.class));
    export.setCategories(mapToDtoList(categoryRepository.findByOwnerAndDeletedFalse(user), CategoryDto.class));
    export
        .setSubcategories(mapToDtoList(subcategoryRepository.findByOwnerAndDeletedFalse(user), SubcategoryDto.class));
    export.setRecurrentTransactions(mapToDtoList(recurrentTransactionRepository.findByOwnerAndDeletedFalse(user),
        RecurrentTransactionDto.class));

    export.setExportedAt(Instant.now().atOffset(ZoneOffset.UTC));

    return export;
  }

  private <S, D> List<D> mapToDtoList(List<S> source, Class<D> targetClass) {
    return source.stream().map(item -> mapper.map(item, targetClass)).collect(Collectors.toList());
  }

  @Override
  public void deleteCurrentUserAccount() {
    User user = getCurrentUser();
    String userId = user.getId();

    // FK-safe order: every owner FK on these tables is ON DELETE RESTRICT, and JPA cascade from
    // User only covers transactions/accounts, so categories/subcategories/recurrent transactions
    // must be deleted explicitly before the user row itself can go.
    transactionTemplate.executeWithoutResult(status -> {
      recurrentTransactionRepository.deleteAll(recurrentTransactionRepository.findByOwner(user));
      transactionRepository.deleteAll(transactionRepository.findByOwner(user));
      subcategoryRepository.deleteAll(subcategoryRepository.findByOwner(user));
      categoryRepository.deleteAll(categoryRepository.findByOwner(user));
      accountRepository.deleteAll(accountRepository.findByOwner(user));
      userSettingsRepository.findById(userId).ifPresent(userSettingsRepository::delete);
      userRepository.delete(user);
    });

    try {
      firebaseService.deleteUser(userId);
    } catch (RuntimeException firebaseException) {
      log.error("Local data for user {} was deleted, but removing the Firebase account failed; "
          + "the Firebase account is now orphaned and should be cleaned up manually", userId, firebaseException);
    }
  }

}
