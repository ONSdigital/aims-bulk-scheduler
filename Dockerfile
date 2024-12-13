FROM openjdk:21-oracle
COPY build/libs/aims-bulk-service-scheduler-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]