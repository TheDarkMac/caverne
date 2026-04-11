package com.devikapps.caverne.modules.catalog;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {
  @Id @UuidGenerator private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "category_id")
  private Category category;

  @Column(nullable = false)
  private String label;

  @Column(nullable = false, unique = true)
  private String reference;

  private LocalDate limitDate;

  @Column(columnDefinition = "TEXT")
  private String description;

  private String size;

  @Builder.Default private boolean isActive = true;

  @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private List<Price> prices = new ArrayList<>();
}
