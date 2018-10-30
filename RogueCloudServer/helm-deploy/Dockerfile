FROM websphere-liberty:webProfile7

RUN apt-get update && apt-get upgrade -y && apt-get install -y unzip maven
RUN apt-get install openjdk-8-jdk -y
RUN mkdir src
COPY RogueCloudClient src/RogueCloudClient
COPY RogueCloudClientLiberty src/RogueCloudClientLiberty
COPY RogueCloudResources src/RogueCloudResources
COPY RogueCloudShared src/RogueCloudShared
COPY RogueCloudServer src/RogueCloudServer
COPY pom.xml src/
COPY .mvn src/
RUN apt-get remove openjdk-8-jdk -y

RUN ls src
RUN cd src/ && JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64/ mvn  package
RUN cp -rf src/RogueCloudServer/target/liberty/wlp/usr/servers/defaultServer/* /config/

# # Upgrade to production license if URL to JAR provided
RUN rm -rf src
ARG LICENSE_JAR_URL

EXPOSE 19080
RUN cd src/RogueCloudClientLiberty/ && ../mvnw liberty:run-server
