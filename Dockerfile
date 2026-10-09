# syntax=docker/dockerfile:1

# ---------- Build: compila com o JDK ----------
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace

# Dependências primeiro: esta camada só é refeita quando o pom.xml muda
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline

COPY src src
RUN ./mvnw -B -q package -DskipTests

# Separa o jar em camadas (dependências mudam pouco, o código muda sempre): imagens mais rápidas de reconstruir e enviar
RUN java -Djarmode=tools -jar target/agendamento-*.jar extract --layers --launcher --destination extracted

# ---------- Runtime: só o JRE, sem ferramentas de build ----------
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S app && adduser -S app -G app
USER app

COPY --from=build /workspace/extracted/dependencies/ ./
COPY --from=build /workspace/extracted/spring-boot-loader/ ./
COPY --from=build /workspace/extracted/snapshot-dependencies/ ./
COPY --from=build /workspace/extracted/application/ ./

# Usa até 75% da memória do contentor (o padrão da JVM é 25%)
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"

EXPOSE 8080
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
