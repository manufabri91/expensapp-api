# Stage 1: build
FROM maven:3.9.16-eclipse-temurin-17-alpine@sha256:b7c8d7298dbabbe750441736cd99a1d120a58083d5af8d2bd75a1624cfdd8f3a AS build
# The published Temurin Alpine images lag the patched Java 17 release.
RUN apk upgrade --no-cache brotli-libs \
    && wget -q -O /tmp/java.tar.gz https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.21%2B9/OpenJDK17U-jdk_x64_alpine-linux_hotspot_17.0.21_9.tar.gz \
    && echo 'db018054d52c1c109ad128c1412ead2d84ae91052ce60ca370df17b6a89831f5  /tmp/java.tar.gz' | sha256sum -c - \
    && rm -rf /opt/java/openjdk \
    && mkdir -p /opt/java/openjdk \
    && tar -xzf /tmp/java.tar.gz --strip-components=1 -C /opt/java/openjdk \
    && rm /tmp/java.tar.gz \
    && java -version
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: run
FROM eclipse-temurin:24-jre-alpine@sha256:4044b6c87cb088885bcd0220f7dc7a8a4aab76577605fa471945d2e98270741f
RUN apk upgrade --no-cache brotli-libs \
    && wget -q -O /tmp/java.tar.gz https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.21%2B9/OpenJDK17U-jre_x64_alpine-linux_hotspot_17.0.21_9.tar.gz \
    && echo 'e42129db57e3965d9d8befdb097531d7887916690338ff0be7d1b0113287c872  /tmp/java.tar.gz' | sha256sum -c - \
    && rm -rf /opt/java/openjdk \
    && mkdir -p /opt/java/openjdk \
    && tar -xzf /tmp/java.tar.gz --strip-components=1 -C /opt/java/openjdk \
    && rm /tmp/java.tar.gz \
    && java -version
WORKDIR /app
COPY --from=build /app/target/expensapp_api.jar app.jar
EXPOSE 8080
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+UseContainerSupport -XX:+UseG1GC -Djava.security.egd=file:/dev/./urandom"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
