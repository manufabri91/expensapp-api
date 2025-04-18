#
# Build stage
#
FROM maven:3.8.5-openjdk-17
COPY src /home/app/src
COPY pom.xml /home/app
RUN mvn -f /home/app/pom.xml clean package
EXPOSE 8080
ENTRYPOINT ["java","-Dspring.profiles.active=dev","-jar","/home/app/target/expensapp_api.jar"]
# ENTRYPOINT ["java","-jar","/home/app/target/expensapp_api.jar"]
