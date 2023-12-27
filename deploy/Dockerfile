ARG LIBERTY_SERVER_IMAGE_VERSION=<passed-in-from-team-city>
FROM gcr.io/xpo-ltl-dev/${LIBERTY_SERVER_IMAGE_VERSION}
ARG LIBERTY_SERVER_PATH=<passed-in-from-team-city>
ARG MAVEN_VERSION=<passed-in-from-teamcity>
ARG PROJECT_NAME=<passed-in-from-teamcity>
COPY target/${PROJECT_NAME}-${MAVEN_VERSION}.war /opt/${LIBERTY_SERVER_PATH}/wlp/usr/servers/defaultServer/apps