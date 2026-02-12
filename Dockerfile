# Stage 1: build con Maven
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# Copia pom e sorgenti
COPY pom.xml .
COPY src ./src

# Build del jar (salta i test per velocizzare)
RUN mvn clean package -DskipTests

# Stage 2: runtime con Java leggero
FROM eclipse-temurin:21-jre
WORKDIR /app

# Copia il jar buildato
COPY --from=build /app/target/*.jar app.jar

# Espone la porta di Spring Boot
EXPOSE 8080

# Variabili d'ambiente (Render può sovrascriverle)
ENV SPRING_PROFILES_ACTIVE=prod

# Comando per avviare l'app
ENTRYPOINT ["java", "-jar", "app.jar"]
