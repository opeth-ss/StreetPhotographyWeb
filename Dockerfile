# Stage: WildFly 10.1.0 with MySQL configuration
FROM eclipse-temurin:8-jdk

# Install required tools
RUN apt-get update && \
    apt-get install -y curl unzip && \
    rm -rf /var/lib/apt/lists/*

# Set WildFly version and environment variables
ENV WILDFLY_VERSION 10.1.0.Final
ENV WILDFLY_HOME /opt/wildfly
ENV LAUNCH_JBOSS_IN_BACKGROUND true

# Download and install WildFly
RUN curl -O https://download.jboss.org/wildfly/$WILDFLY_VERSION/wildfly-$WILDFLY_VERSION.zip && \
    unzip wildfly-$WILDFLY_VERSION.zip -d /opt && \
    mv /opt/wildfly-$WILDFLY_VERSION $WILDFLY_HOME && \
    rm wildfly-$WILDFLY_VERSION.zip

# Create jboss user and group
RUN groupadd -r jboss && useradd -r -g jboss jboss

# Create MySQL module directory
RUN mkdir -p $WILDFLY_HOME/modules/com/mysql/main

# Copy MySQL driver and module configuration
COPY mysql-connector-java-8.0.33.jar $WILDFLY_HOME/modules/com/mysql/main/
COPY module.xml $WILDFLY_HOME/modules/com/mysql/main/

# Copy datasource configuration
COPY mysql-ds.xml $WILDFLY_HOME/standalone/configuration/

# Set permissions
RUN chown -R jboss:jboss $WILDFLY_HOME/standalone/configuration/
RUN chmod -R 755 $WILDFLY_HOME/standalone/configuration/

# Add management user (optional for admin access)
RUN $WILDFLY_HOME/bin/add-user.sh admin Admin#123 --silent

# Copy application WAR from multi-stage build (if used)
# Comment this line if you are not using a multi-stage build with /app/target/
# COPY --from=build /app/target/streetphotography.war $WILDFLY_HOME/standalone/deployments/

# OR if building within this container, adjust accordingly:
# COPY target/streetphotography.war $WILDFLY_HOME/standalone/deployments/

# Fix general permissions
RUN chmod -R a+rwX $WILDFLY_HOME

# Expose ports
EXPOSE 8080 9990

# Start WildFly
CMD ["/opt/wildfly/bin/standalone.sh", "-b", "0.0.0.0", "-bmanagement", "0.0.0.0", "--server-config=standalone.xml"]
