.PHONY: ci-format ci-semgrep

ci-format:
	find src -name "*.java" -print0 | xargs -0 -n 500 java -jar google-java-format-1.23.0-all-deps.jar --dry-run --set-exit-if-changed

ci-semgrep:
	pip install semgrep
	semgrep scan --config auto --exclude build --exclude clients --exclude docs/openapi.json --sarif --output semgrep.sarif .
