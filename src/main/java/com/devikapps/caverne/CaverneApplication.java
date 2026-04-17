package com.devikapps.caverne;

import java.time.Clock;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootApplication
public class CaverneApplication {

  public static void main(String[] args) {
    SpringApplication.run(CaverneApplication.class, args);
  }

  @Bean
  Clock clock() {
    return Clock.systemUTC();
  }

  @Bean
  TransactionTemplate transactionTemplate(PlatformTransactionManager txManager) {
    return new TransactionTemplate(txManager);
  }
}
