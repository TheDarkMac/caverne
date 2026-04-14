.PHONY: ci-format ci-semgrep gatling

ci-format:
	find src -name "*.java" -print0 | xargs -0 -n 500 java -jar google-java-format-1.23.0-all-deps.jar --dry-run --set-exit-if-changed

ci-semgrep:
	pip install semgrep
	semgrep scan --config auto --exclude build --exclude clients --exclude docs/openapi.json --sarif --output semgrep.sarif .

gatling:
	./gradlew --no-daemon bootJar gatlingClasses
	docker compose up -d
	@echo "Waiting for Postgres..."
	@until docker compose exec postgres pg_isready -U caverne -d caverne >/dev/null 2>&1; do sleep 1; done
	CATALOG_NICHES_IMPORT_ENABLED=true \
	  SPRING_DATASOURCE_URL=jdbc:postgresql://127.0.0.1:5432/caverne \
	  SPRING_DATASOURCE_USERNAME=caverne \
	  SPRING_DATASOURCE_PASSWORD=caverne \
	  SPRING_DATASOURCE_DRIVER_CLASS_NAME=org.postgresql.Driver \
	  SPRING_DOCKER_COMPOSE_ENABLED=false \
	  AUTH_PROVIDERS_SUPABASE_ENABLED=false \
	  BOOTSTRAP_ADMIN_ENABLED=false \
	  STRIPE_ENABLED=false \
	  java -jar build/libs/*.jar > app.log 2>&1 & echo $$! > app.pid
	@echo "Waiting for application..."
	@for i in $$(seq 1 60); do \
	  if curl --silent --fail http://127.0.0.1:8080/api/v1/actuator/health >/dev/null 2>&1; then \
	    echo "Application ready"; break; \
	  fi; \
	  sleep 2; \
	  if [ $$i -eq 60 ]; then echo "ERROR: app did not start"; cat app.log; kill $$(cat app.pid) || true; exit 1; fi; \
	done
	./gradlew --no-daemon gatlingRun; EXIT=$$?; kill $$(cat app.pid) || true; exit $$EXIT
