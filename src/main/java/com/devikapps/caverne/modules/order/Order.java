package com.devikapps.caverne.modules.order;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String reference;
    private LocalDateTime date;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    private String currencyCode;

    // V1 Anonymous customer info
    private String customerName;
    private String customerEmail;
    private String customerPhone;
    private String shippingLocation;
    private String postalCode;
    private String countryCode;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();

    private BigDecimal totalAmount;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "order_payments", joinColumns = @JoinColumn(name = "order_id"))
    @Builder.Default
    private List<OrderPayment> payments = new ArrayList<>();
}
