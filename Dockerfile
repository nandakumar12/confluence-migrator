FROM maven:3-openjdk-11 as build
WORKDIR /app
COPY pom.xml pom.xml
RUN mvn clean install spring-boot:repackage
COPY ./ .
EXPOSE 8000 8888
CMD ["mvn", "spring-boot:run"]