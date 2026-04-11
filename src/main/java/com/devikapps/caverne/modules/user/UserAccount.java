package com.devikapps.caverne.modules.user;

import jakarta.persistence.*;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "app_users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserAccount {
  @Id @UuidGenerator private UUID id;

  @Column(nullable = false)
  private String firstname;

  @Column(nullable = false)
  private String lastname;

  @Column private String email;

  private String phone;

  @Column(nullable = false)
  private String passwordHash;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private AuthProviderCode authProvider;

  @Column private String externalAuthId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private UserRole role;

  @Column(nullable = false)
  private String status;

  @PrePersist
  @PreUpdate
  void applyDefaults() {
    if (authProvider == null) {
      authProvider = AuthProviderCode.LOCAL;
    }
  }
}
