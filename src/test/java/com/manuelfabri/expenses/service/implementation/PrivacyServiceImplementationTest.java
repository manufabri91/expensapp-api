package com.manuelfabri.expenses.service.implementation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.modelmapper.config.Configuration.AccessLevel;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import com.manuelfabri.expenses.dto.UserDataExportDto;
import com.manuelfabri.expenses.model.Account;
import com.manuelfabri.expenses.model.CurrencyEnum;
import com.manuelfabri.expenses.model.User;
import com.manuelfabri.expenses.model.UserSettings;
import com.manuelfabri.expenses.repository.AccountRepository;
import com.manuelfabri.expenses.repository.CategoryRepository;
import com.manuelfabri.expenses.repository.RecurrentTransactionRepository;
import com.manuelfabri.expenses.repository.SubcategoryRepository;
import com.manuelfabri.expenses.repository.TransactionRepository;
import com.manuelfabri.expenses.repository.UserRepository;
import com.manuelfabri.expenses.repository.UserSettingsRepository;
import com.manuelfabri.expenses.service.FirebaseService;

@ExtendWith(MockitoExtension.class)
class PrivacyServiceImplementationTest {

  @Mock
  private AccountRepository accountRepository;
  @Mock
  private TransactionRepository transactionRepository;
  @Mock
  private CategoryRepository categoryRepository;
  @Mock
  private SubcategoryRepository subcategoryRepository;
  @Mock
  private RecurrentTransactionRepository recurrentTransactionRepository;
  @Mock
  private UserSettingsRepository userSettingsRepository;
  @Mock
  private UserRepository userRepository;
  @Mock
  private FirebaseService firebaseService;
  @Mock
  private PlatformTransactionManager transactionManager;

  private PrivacyServiceImplementation service;
  private User currentUser;

  @BeforeEach
  void setUp() {
    ModelMapper mapper = new ModelMapper();
    mapper.getConfiguration().setFieldMatchingEnabled(true).setFieldAccessLevel(AccessLevel.PRIVATE)
        .setSkipNullEnabled(true);

    service = new PrivacyServiceImplementation(accountRepository, transactionRepository, categoryRepository,
        subcategoryRepository, recurrentTransactionRepository, userSettingsRepository, userRepository,
        firebaseService, mapper, transactionManager);

    currentUser = new User("owner-1", "owner@example.com", "owner", "Owner", "One", List.of());
    currentUser.setTermsAcceptedAt(OffsetDateTime.parse("2026-01-01T00:00:00Z"));
    currentUser.setTermsVersion("1.0");

    SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
    securityContext.setAuthentication(new UsernamePasswordAuthenticationToken(currentUser, null));
    SecurityContextHolder.setContext(securityContext);

    TransactionStatus status = mock(TransactionStatus.class);
    Mockito.lenient().when(transactionManager.getTransaction(any())).thenReturn(status);
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void exportUserDataIncludesProfileConsentAndOwnedRows() {
    when(userSettingsRepository.findById("owner-1")).thenReturn(Optional.of(new UserSettings("owner-1")));
    Account account = new Account(1L, "Checking", CurrencyEnum.USD, currentUser);
    when(accountRepository.findByOwnerAndDeletedFalse(currentUser)).thenReturn(List.of(account));
    when(transactionRepository.findByOwnerAndDeletedFalse(currentUser)).thenReturn(List.of());
    when(categoryRepository.findByOwnerAndDeletedFalse(currentUser)).thenReturn(List.of());
    when(subcategoryRepository.findByOwnerAndDeletedFalse(currentUser)).thenReturn(List.of());
    when(recurrentTransactionRepository.findByOwnerAndDeletedFalse(currentUser)).thenReturn(List.of());

    UserDataExportDto export = service.exportUserData();

    assertThat(export.getId()).isEqualTo("owner-1");
    assertThat(export.getEmail()).isEqualTo("owner@example.com");
    assertThat(export.getTermsVersion()).isEqualTo("1.0");
    assertThat(export.getAccounts()).hasSize(1);
    assertThat(export.getAccounts().get(0).getName()).isEqualTo("Checking");
    assertThat(export.getExportedAt()).isNotNull();
  }

  @Test
  void exportUserDataOmitsSettingsWhenNoneExistForTheUser() {
    when(userSettingsRepository.findById("owner-1")).thenReturn(Optional.empty());
    when(accountRepository.findByOwnerAndDeletedFalse(currentUser)).thenReturn(List.of());
    when(transactionRepository.findByOwnerAndDeletedFalse(currentUser)).thenReturn(List.of());
    when(categoryRepository.findByOwnerAndDeletedFalse(currentUser)).thenReturn(List.of());
    when(subcategoryRepository.findByOwnerAndDeletedFalse(currentUser)).thenReturn(List.of());
    when(recurrentTransactionRepository.findByOwnerAndDeletedFalse(currentUser)).thenReturn(List.of());

    UserDataExportDto export = service.exportUserData();

    assertThat(export.getSettings()).isNull();
  }

  @Test
  void deleteCurrentUserAccountDeletesOwnedDataInForeignKeySafeOrderThenTheFirebaseUser() {
    when(recurrentTransactionRepository.findByOwner(currentUser)).thenReturn(List.of());
    when(transactionRepository.findByOwner(currentUser)).thenReturn(List.of());
    when(subcategoryRepository.findByOwner(currentUser)).thenReturn(List.of());
    when(categoryRepository.findByOwner(currentUser)).thenReturn(List.of());
    when(accountRepository.findByOwner(currentUser)).thenReturn(List.of());
    when(userSettingsRepository.findById("owner-1")).thenReturn(Optional.empty());

    service.deleteCurrentUserAccount();

    InOrder inOrder = Mockito.inOrder(recurrentTransactionRepository, transactionRepository, subcategoryRepository,
        categoryRepository, accountRepository, userRepository, firebaseService);
    inOrder.verify(recurrentTransactionRepository).deleteAll(List.of());
    inOrder.verify(transactionRepository).deleteAll(List.of());
    inOrder.verify(subcategoryRepository).deleteAll(List.of());
    inOrder.verify(categoryRepository).deleteAll(List.of());
    inOrder.verify(accountRepository).deleteAll(List.of());
    inOrder.verify(userRepository).delete(currentUser);
    // The Firebase deletion must only happen after the local deletions above have gone through,
    // since it runs after transactionTemplate.executeWithoutResult(...) has already committed.
    inOrder.verify(firebaseService).deleteUser("owner-1");
  }

  @Test
  void deleteCurrentUserAccountStillDeletesLocalDataEvenWhenTheFirebaseCleanupFails() {
    when(recurrentTransactionRepository.findByOwner(currentUser)).thenReturn(List.of());
    when(transactionRepository.findByOwner(currentUser)).thenReturn(List.of());
    when(subcategoryRepository.findByOwner(currentUser)).thenReturn(List.of());
    when(categoryRepository.findByOwner(currentUser)).thenReturn(List.of());
    when(accountRepository.findByOwner(currentUser)).thenReturn(List.of());
    when(userSettingsRepository.findById("owner-1")).thenReturn(Optional.empty());
    Mockito.doThrow(new RuntimeException("firebase unavailable")).when(firebaseService).deleteUser("owner-1");

    // Must not throw: local data is already gone by the time the Firebase call fails, so this is
    // logged and swallowed rather than surfaced as a failed request.
    service.deleteCurrentUserAccount();

    verify(userRepository).delete(currentUser);
  }

  @Test
  void deleteCurrentUserAccountDeletesUserSettingsWhenPresent() {
    when(recurrentTransactionRepository.findByOwner(currentUser)).thenReturn(List.of());
    when(transactionRepository.findByOwner(currentUser)).thenReturn(List.of());
    when(subcategoryRepository.findByOwner(currentUser)).thenReturn(List.of());
    when(categoryRepository.findByOwner(currentUser)).thenReturn(List.of());
    when(accountRepository.findByOwner(currentUser)).thenReturn(List.of());
    UserSettings settings = new UserSettings("owner-1");
    when(userSettingsRepository.findById("owner-1")).thenReturn(Optional.of(settings));

    service.deleteCurrentUserAccount();

    verify(userSettingsRepository, times(1)).delete(settings);
  }

  @Test
  void deleteCurrentUserAccountOnlyDeletesRowsScopedToTheCurrentUser() {
    Account ownedAccount = new Account(1L, "Checking", CurrencyEnum.USD, currentUser);
    when(recurrentTransactionRepository.findByOwner(currentUser)).thenReturn(List.of());
    when(transactionRepository.findByOwner(currentUser)).thenReturn(List.of());
    when(subcategoryRepository.findByOwner(currentUser)).thenReturn(List.of());
    when(categoryRepository.findByOwner(currentUser)).thenReturn(List.of());
    when(accountRepository.findByOwner(currentUser)).thenReturn(List.of(ownedAccount));
    when(userSettingsRepository.findById("owner-1")).thenReturn(Optional.empty());

    service.deleteCurrentUserAccount();

    // findByOwner(currentUser) scopes every lookup, so Mockito would fail to match these stubs
    // (and the deleteAll/delete calls below) if the service ever queried or deleted on behalf of
    // a different user.
    verify(accountRepository).deleteAll(List.of(ownedAccount));
    verify(userRepository).delete(currentUser);
  }

}
