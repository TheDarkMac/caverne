# Étape de build
FROM gradle:9.4.1-jdk21 AS builder
WORKDIR /workspace/app
COPY . .
RUN ./gradlew --no-daemon clean bootJar

# Étape finale
FROM eclipse-temurin:21-jre
WORKDIR /app

# Copier le jar depuis l'étape builder
COPY --from=builder /workspace/app/build/libs/*.jar app.jar

# Créer un utilisateur non-root
RUN useradd -u 1001 -r -s /bin/false appuser \
    && chown appuser:appuser /app/app.jar

# Exposer le port
EXPOSE 8080

# Variables d'environnement
ENV JAVA_OPTS="-Xms512m -Xmx1024m"
ENV SPRING_PROFILES_ACTIVE=prod

# Basculer sur l’utilisateur non-root
USER appuser

# Entrée du conteneur
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
