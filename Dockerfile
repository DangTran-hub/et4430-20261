# ---------- Build ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
# Tải thư viện trước để lần build sau dùng lại cache
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src src
RUN mvn -B -q -DskipTests package

# ---------- Run ----------
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
# Máy miễn phí của Render chỉ có 512 MB RAM: giới hạn heap và dùng GC nhẹ
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70 -XX:+UseSerialGC -Xss512k -XX:TieredStopAtLevel=1"
EXPOSE 8080
# - Nếu không có DB_URL thì ghép từ DB_HOST/DB_PORT/DB_NAME (Render truyền vào từ database)
# - Cổng lấy từ biến PORT của Render; forward-headers để giữ https sau proxy
CMD ["sh", "-c", "export DB_URL=\"${DB_URL:-jdbc:postgresql://${DB_HOST}:${DB_PORT:-5432}/${DB_NAME}}\"; exec java -Dserver.port=${PORT:-8080} -Dserver.forward-headers-strategy=native -Dspring.datasource.hikari.maximum-pool-size=5 -jar app.jar"]
