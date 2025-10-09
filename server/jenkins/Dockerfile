# Minimal Jenkins LTS image
# Builds on top of the official Jenkins image to allow future customization
FROM jenkins/jenkins:lts-jdk21

# Optional: set timezone inside the container (overridden by JAVA_OPTS/TZ if provided)
ENV TZ=${TZ}
ENV JAVA_OPTS=${JAVA_OPTS}
ENV JENKINS_OPTS=${JENKINS_OPTS}

# Expose Jenkins web and inbound agent ports
EXPOSE 8080 50000

# By default, the upstream image starts Jenkins with the jenkins user
# and persists data under /var/jenkins_home
# No extra steps required for a quick test setup.
 
# -----------------------------
# Plugins installation
# -----------------------------
# Copy plugin list and install via official CLI
USER root
COPY ./config/plugins.txt /usr/share/jenkins/ref/plugins.txt

RUN mkdir -p /usr/share/jenkins/ref/casc_configs
COPY ./config/jenkins.yaml /usr/share/jenkins/ref/casc_configs/jenkins.yaml

RUN jenkins-plugin-cli --plugin-file /usr/share/jenkins/ref/plugins.txt

# -----------------------------
# Tools for pipelines
# -----------------------------
# Install: git, ssh client, curl, DB clients, and Docker CLI + compose plugin
RUN set -eux; \
    apt-get update; \
    apt-get install -y --no-install-recommends \
      ca-certificates \
      curl \
      jq \
      gnupg \
      git \
      openssh-client \
      postgresql-client \
      mariadb-client \
      default-mysql-client || true; \
    rm -rf /var/lib/apt/lists/*; \
    mkdir -p /etc/apt/keyrings; \
    install -m 0755 -d /etc/apt/keyrings; \
    curl -fsSL https://download.docker.com/linux/debian/gpg | gpg --dearmor -o /etc/apt/keyrings/docker.gpg; \
    chmod a+r /etc/apt/keyrings/docker.gpg; \
    echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/debian $(. /etc/os-release && echo $VERSION_CODENAME) stable" > /etc/apt/sources.list.d/docker.list; \
    apt-get update; \
    apt-get install -y --no-install-recommends docker-ce-cli docker-compose-plugin; \
    rm -rf /var/lib/apt/lists/*

# Bootstrap entrypoint to auto-fetch Keycloak client secret at runtime
COPY ./config/entrypoint.sh /usr/local/bin/jenkins-entrypoint.sh
RUN chmod +x /usr/local/bin/jenkins-entrypoint.sh

# Switch back to jenkins user and use our entrypoint (which execs jenkins.sh)
USER jenkins
ENTRYPOINT ["/usr/local/bin/jenkins-entrypoint.sh"]
