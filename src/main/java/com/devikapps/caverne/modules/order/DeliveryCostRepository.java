package com.devikapps.caverne.modules.order;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeliveryCostRepository extends JpaRepository<DeliveryCost, UUID> {}
