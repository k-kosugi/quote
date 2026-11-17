FROM maven:3-eclipse-temurin-27 AS build
WORKDIR /workspace

# 依存関係の取得を先に行い, ソースの変更時にキャッシュを再利用する.
# mvnw は ACR Tasks 上で Maven の展開に失敗するため, Maven 入りのイメージを使う
COPY pom.xml ./
RUN mvn -q dependency:go-offline

COPY src src
RUN mvn -q package -DskipTests

FROM eclipse-temurin:27-jre
RUN useradd --system --no-create-home quote
USER quote
WORKDIR /app

COPY --from=build /workspace/target/quote-*-SNAPSHOT.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
