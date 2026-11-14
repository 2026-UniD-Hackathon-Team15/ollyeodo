# 백엔드 배포 이미지 (#13). Render 등에서 ollyeodo-backend/ 를 루트로 빌드한다.
# 1단계: Gradle로 실행 jar 빌드 (테스트는 CI·로컬에서 돌리고 이미지 빌드에서는 생략)
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
RUN chmod +x gradlew && ./gradlew --no-daemon --quiet dependencies > /dev/null
COPY src src
RUN ./gradlew --no-daemon --quiet bootJar -x test

# 2단계: 실행용 JRE 이미지
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
# 무료 인스턴스(512MB)에 맞춘 메모리 설정
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70 -XX:+UseSerialGC -Xss512k -XX:TieredStopAtLevel=1"
# Render는 PORT 환경변수(기본 10000)로 포트를 넘겨준다. application.yaml의 server.port가 이를 읽는다.
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
