FROM openjdk:8-jre-alpine
RUN apk add mysql-client

RUN mkdir /dbscripts
COPY target/dbmanager-*.jar /dbmanager.jar

ENTRYPOINT ["java","-jar","/dbmanager.jar"]