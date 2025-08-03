package com.manuelfabri.expenses.service.implementation;

import java.util.Collections;
import org.springframework.stereotype.Service;
import com.manuelfabri.expenses.dto.UserRegisterDto;
import com.manuelfabri.expenses.exception.ResourceNotFoundException;
import com.manuelfabri.expenses.model.Role;
import com.manuelfabri.expenses.model.User;
import com.manuelfabri.expenses.repository.UserRepository;
import com.manuelfabri.expenses.service.UserService;

@Service
public class UserServiceImplementation implements UserService {

  private UserRepository userRepository;

  public UserServiceImplementation(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  @Override
  public User createUserFromRequest(UserRegisterDto userRegisterData, String userId, Role startingRole) {
    User user = new User(userId, userRegisterData.getEmail(), userRegisterData.getUserName(),
        userRegisterData.getFirstName(), userRegisterData.getLastName(), Collections.singletonList(startingRole));
    User savedUser = userRepository.save(user);

    return savedUser;
  }


  @Override
  public User getUserById(String id) {
    return this.userRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("User", "id", id.toString()));
  }

}
