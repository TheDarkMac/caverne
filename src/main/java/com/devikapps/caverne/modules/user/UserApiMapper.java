package com.devikapps.caverne.modules.user;

import org.springframework.stereotype.Component;

@Component
public class UserApiMapper {

  public org.openapitools.client.model.User toResponse(UserAccount user) {
    return new org.openapitools.client.model.User()
        .id(user.getId().intValue())
        .firstname(user.getFirstname())
        .lastname(user.getLastname())
        .email(user.getEmail())
        .phone(user.getPhone())
        .role(org.openapitools.client.model.User.RoleEnum.fromValue(toContractRole(user.getRole())))
        .status(user.getStatus());
  }

  public void applyUpdate(UserAccount user, org.openapitools.client.model.UserUpdate input) {
    if (input.getFirstname() != null) {
      user.setFirstname(input.getFirstname());
    }
    if (input.getLastname() != null) {
      user.setLastname(input.getLastname());
    }
    if (input.getPhone() != null) {
      user.setPhone(input.getPhone());
    }
  }

  public UserRole fromContractRole(String role) {
    if (role == null || role.isBlank()) {
      return null;
    }

    return switch (role.trim().toLowerCase()) {
      case "admin" -> UserRole.ADMIN;
      case "simple_user" -> UserRole.SIMPLE_USER;
      default -> throw new IllegalArgumentException("Invalid user role filter");
    };
  }

  private String toContractRole(UserRole role) {
    return switch (role) {
      case ADMIN -> "admin";
      case SIMPLE_USER -> "simple_user";
    };
  }
}
