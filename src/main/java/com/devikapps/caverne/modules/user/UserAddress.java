package com.devikapps.caverne.modules.user;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_addresses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserAddress {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private UserAccount user;

  @Column(nullable = false)
  private String location;

  @Column(nullable = false)
  private String postalCode;

  @Column(nullable = false, length = 3)
  private String countryCode;

  @Column(nullable = false)
  private boolean isDefault;
}
