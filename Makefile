.PHONY: ci-format ci-semgrep gatling perf-local

# Port the locally-running app is listening on (override: make perf-local PORT=8081)
PORT ?= 8080

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

# Run Gatling against an app you've already started (any port).
# Fails fast if /actuator/health is not reachable, so you don't burn a 1m30 run on Connection refused.
perf-local:
	@echo "Probing http://localhost:$(PORT)/api/v1/actuator/health ..."
	@if ! curl --silent --fail "http://localhost:$(PORT)/api/v1/actuator/health" >/dev/null 2>&1; then \
	  echo "ERROR: nothing healthy on port $(PORT). Start the app first or pass PORT=<port>."; \
	  exit 1; \
	fi
	@echo "App is up. Launching Gatling against port $(PORT)."
	GATLING_BASE_URL=http://localhost:$(PORT)/api/v1 ./gradlew gatlingRun
