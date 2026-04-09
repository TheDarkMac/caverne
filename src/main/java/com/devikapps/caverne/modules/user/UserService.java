package com.devikapps.caverne.modules.user;

import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

  private final UserRepository userRepository;
  private final AuthSessionRepository authSessionRepository;
  private final UserApiMapper userApiMapper;
  private final PasswordEncoder passwordEncoder;

  @Transactional(readOnly = true)
  public Page<org.openapitools.client.model.User> findAll(String role, Pageable pageable) {
    UserRole expectedRole = userApiMapper.fromContractRole(role);
    return userRepository.findAll(withRole(expectedRole), pageable).map(userApiMapper::toResponse);
  }

  @Transactional(readOnly = true)
  public org.openapitools.client.model.User getCurrentUser(UserAccount user) {
    return userApiMapper.toResponse(user);
  }

  public org.openapitools.client.model.User updateCurrentUser(
      UserAccount user, org.openapitools.client.model.UserUpdate input) {
    userApiMapper.applyUpdate(user, input);
    return userApiMapper.toResponse(userRepository.save(user));
  }

  public org.openapitools.client.model.User createUser(
      org.openapitools.client.model.UserCreateInput input) {
    validateCreateInput(input);
    String normalizedEmail = normalizeEmail(input.getEmail());
    String normalizedPhone = normalizePhone(input.getPhone());

    if (normalizedEmail != null
        && userRepository.findByEmailIgnoreCase(normalizedEmail).isPresent()) {
      throw new ResponseStatusException(UNPROCESSABLE_ENTITY, "email is already registered");
    }
    if (normalizedPhone != null && userRepository.findByPhone(normalizedPhone).isPresent()) {
      throw new ResponseStatusException(UNPROCESSABLE_ENTITY, "phone is already registered");
    }

    UserRole role =
        userApiMapper.fromContractRole(input.getRole() == null ? null : input.getRole().getValue());
    if (role == null) {
      role = UserRole.SIMPLE_USER;
    }

    String status = isBlank(input.getStatus()) ? "active" : input.getStatus().trim();

    UserAccount user =
        UserAccount.builder()
            .firstname(input.getFirstname().trim())
            .lastname(input.getLastname().trim())
            .email(normalizedEmail)
            .phone(normalizedPhone)
            .passwordHash(passwordEncoder.encode(input.getPassword()))
            .authProvider(AuthProviderCode.LOCAL)
            .role(role)
            .status(status)
            .build();

    return userApiMapper.toResponse(userRepository.save(user));
  }

  @Transactional(readOnly = true)
  public org.openapitools.client.model.User getById(Long id) {
    return userApiMapper.toResponse(findEntityById(id));
  }

  public void deleteById(Long id) {
    UserAccount user = findEntityById(id);
    authSessionRepository.deleteByUser(user);
    userRepository.delete(user);
  }

  private UserAccount findEntityById(Long id) {
    return userRepository
        .findById(id)
        .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "User not found"));
  }

  private Specification<UserAccount> withRole(UserRole role) {
    return (root, query, builder) -> role == null ? null : builder.equal(root.get("role"), role);
  }

  private void validateCreateInput(org.openapitools.client.model.UserCreateInput input) {
    if (input == null
        || isBlank(input.getFirstname())
        || isBlank(input.getLastname())
        || isBlank(input.getPassword())
        || (isBlank(input.getEmail()) && isBlank(input.getPhone()))) {
      throw new ResponseStatusException(
          UNPROCESSABLE_ENTITY,
          "firstname, lastname, password, and either email or phone are required");
    }
  }

  private String normalizeEmail(String value) {
    if (isBlank(value)) {
      return null;
    }
    return value.trim().toLowerCase();
  }

  private String normalizePhone(String value) {
    if (isBlank(value)) {
      return null;
    }
    return value.trim();
  }

  private boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
