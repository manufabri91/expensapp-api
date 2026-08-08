package com.manuelfabri.expenses.service.implementation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.manuelfabri.expenses.constants.LegalVersions;
import com.manuelfabri.expenses.dto.UserRegisterDto;
import com.manuelfabri.expenses.model.Role;
import com.manuelfabri.expenses.model.User;
import com.manuelfabri.expenses.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceImplementationTest {

  @Mock
  private UserRepository userRepository;

  @Test
  void createUserFromRequestStampsTermsAcceptanceOnRegistration() {
    UserServiceImplementation service = new UserServiceImplementation(userRepository);
    when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

    UserRegisterDto registerData = new UserRegisterDto();
    registerData.setEmail("owner@example.com");
    registerData.setUserName("owner123");
    registerData.setPassword("password1");
    registerData.setFirstName("Owner");
    registerData.setLastName("One");
    registerData.setAcceptedTerms(true);

    Role role = new Role(1L);
    role.setName("BASIC_USER");
    User created = service.createUserFromRequest(registerData, "uid-1", role);

    assertThat(created.getTermsAcceptedAt()).isNotNull();
    assertThat(created.getTermsVersion()).isEqualTo(LegalVersions.TERMS_VERSION);
    assertThat(created.getRoles()).isEqualTo(List.of(role));
  }

}
