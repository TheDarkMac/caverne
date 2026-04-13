package com.devikapps.caverne.modules.order;

import com.devikapps.caverne.modules.user.UserAccount;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class Order {
  @Id @UuidGenerator private UUID id;

  private String reference;
  private LocalDateTime date;

  @Enumerated(EnumType.STRING)
  private OrderStatus status;

  private String currencyCode;

  private UUID deliveryCostId;

  // Recipient info stored in the existing order contact columns.
  @Column(name = "customer_name")
  private String recipientName;

  @Column(name = "customer_email")
  private String recipientEmail;

  @Column(name = "customer_phone")
  private String recipientPhone;

  private String shippingLocation;
  private String postalCode;
  private String countryCode;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id")
  @ToString.Exclude
  private UserAccount user;

  @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  @ToString.Exclude
  private List<OrderItem> items = new ArrayList<>();

  private BigDecimal totalAmount;

  @OneToMany(
      mappedBy = "order",
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      fetch = FetchType.EAGER)
  @Builder.Default
  @ToString.Exclude
  private List<OrderPayment> payments = new ArrayList<>();
}
