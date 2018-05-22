FROM websphere-liberty:webProfile7

# RUN apt-get update && apt-get upgrade -y && apt-get install -y unzip

COPY /target/liberty/wlp/usr/servers/defaultServer /config/

# Upgrade to production license if URL to JAR provided
ARG LICENSE_JAR_URL

RUN \ 
  if [ $LICENSE_JAR_URL ]; then \
    wget $LICENSE_JAR_URL -O /tmp/license.jar \
    && java -jar /tmp/license.jar -acceptLicense /opt/ibm \
    && rm /tmp/license.jar; \
  fi

