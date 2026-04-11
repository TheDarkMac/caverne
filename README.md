# La Caverne Malgache


### Specifications

- java 21
- docker
- docker-compose

### Environment Variable

***some field is not necessary in production***

create .env file copy-paste all lignes inside .env.example
change with appropriate credential, all thing inside .env.example has precise detail on comment

### Test

run 
```bash
./gradlew test
```
so the is a test that not matched with this step. inside .env, there is a variable name RUN_REAL_STRIPE_TEST, make it true and run separately the test RealStripePaymentIntegrationTest . **this part need stripe apiKey** 