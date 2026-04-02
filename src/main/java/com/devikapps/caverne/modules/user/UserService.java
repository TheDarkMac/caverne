package com.devikapps.caverne.modules.user;

import static org.springframework.http.HttpStatus.NOT_FOUND;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
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
}
