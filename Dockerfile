FROM openjdk:8-jre-alpine
COPY target/uberjar/pricing-service.jar /app/pricing-service.jar
WORKDIR /app
EXPOSE 8081
CMD ["java", "-jar", "pricing-service.jar"]
