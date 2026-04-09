# ──────────────────────────────────────────
# Stage 1: Build
# ──────────────────────────────────────────
FROM gradle:8.11-jdk21 AS builder

WORKDIR /app

# 의존성 캐시를 극대화하기 위해 gradle 설정 파일 먼저 복사
COPY build.gradle settings.gradle ./
COPY gradle ./gradle
COPY gradlew ./

# 의존성 다운로드 (소스 변경 없을 때 캐시 활용)
RUN ./gradlew dependencies --no-daemon || true

# 소스 코드 복사 후 빌드 (테스트 제외)
COPY src ./src
RUN ./gradlew bootJar -x test --no-daemon

# ──────────────────────────────────────────
# Stage 2: Run
# ──────────────────────────────────────────
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# 타임존 설정 (Asia/Seoul)
ENV TZ=Asia/Seoul
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# 비루트 사용자로 실행 (보안)
RUN addgroup --system spring && adduser --system --ingroup spring spring
USER spring

# 빌드된 JAR 복사
COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "-Dspring.profiles.active=prod", "app.jar"]
