FROM eclipse-temurin:24-jdk

WORKDIR /app

COPY . .

RUN sed -i 's/\r$//' mvnw \
    && chmod +x mvnw \
    && ./mvnw clean package -DskipTests

EXPOSE 8080

CMD ["sh", "-c", "java -Dserver.port=${PORT:-8080} -jar target/api-gateway-0.0.1-SNAPSHOT.jar"]
