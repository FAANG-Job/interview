# Spring Boot CI/CD and Container Deployment

## Objective

This project automates the build and deployment of a Spring Boot application. It builds the application JAR, packages it into a Podman container image, transfers the deployment artifacts to an Oracle Linux server, and replaces the running application container.

## Current Architecture

```text
GitHub Repository
        ↓
Jenkins Build Job: FANNG-JOB-INTERVIEW
        ↓
Maven Build and Test
        ↓
Deployment Artifacts
(JAR, Dockerfile, deploy.sh)
        ↓
Jenkins Deployment Job: TRANSFER-JAR
        ↓
SSH Publisher
        ↓
Oracle Linux Server
        ↓
Podman Image Build and Container Deployment
```

## Jenkins Build Job

The Jenkins job `FANNG-JOB-INTERVIEW` checks out the Spring Boot project from GitHub.

The job uses Java 21 and Maven to build the application:

```bat
call mvn clean package
```

The Maven build creates the application JAR:

```text
target/interview-0.0.1-SNAPSHOT.jar
```

After a successful build, Jenkins creates a staging folder named `deployment-artifacts` and copies the following files into it:

```text
deployment-artifacts/
├── Dockerfile
├── deploy.sh
└── interview-0.0.1-SNAPSHOT.jar
```

These artifacts are archived by Jenkins so they can be consumed by another Jenkins job.

## Jenkins Deployment Job

The Jenkins job `TRANSFER-JAR` retrieves the archived artifacts from the latest successful build of `FANNG-JOB-INTERVIEW`.

It copies the artifacts into its local workspace:

```text
incoming/
├── Dockerfile
├── deploy.sh
└── interview-0.0.1-SNAPSHOT.jar
```

The Publish Over SSH plugin transfers these files to the Oracle Linux server:

```text
/home/roaggarw/jenkins-jars/
├── Dockerfile
├── deploy.sh
└── interview-0.0.1-SNAPSHOT.jar
```

After transfer, Jenkins executes `deploy.sh` remotely.

## Remote Deployment Process

The deployment script performs the following actions:

1. Changes to the deployment directory.
2. Converts `deploy.sh` from Windows CRLF line endings to Linux LF line endings.
3. Grants execute permission to the deployment script.
4. Builds a Podman image.
5. Stops and removes the old application container.
6. Starts the new application container on port 8080.

Current Podman deployment flow:

```bash
podman build -t interview-app:1.0 .

podman rm -f interview-app || true

podman run -d \
  --name interview-app \
  -p 8080:8080 \
  interview-app:1.0
```

## Dockerfile

The Dockerfile uses Java 21, copies the Spring Boot JAR into the image, exposes port 8080, and starts the application.

```dockerfile
FROM container-registry.oracle.com/graalvm/jdk:21

WORKDIR /app

COPY interview-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
```

## Problems Solved

* Configured Java 21 and Maven in a Windows Jenkins job.
* Learned that Maven commands in a Windows batch script require `call`, because Maven runs through `mvn.cmd`.
* Created separate Jenkins build and deployment jobs.
* Archived build artifacts and copied them between Jenkins jobs.
* Transferred deployment files securely through SSH.
* Built and deployed a Podman container on Oracle Linux.
* Replaced an existing running container during deployment.
* Fixed Windows CRLF line-ending errors in a Linux shell script.

## Production Improvement Roadmap

### 1. Add tests to CI

* Run unit tests before creating the JAR.
* Fail the Jenkins build if tests fail.
* Add integration tests for critical API flows.

### 2. Use versioned images

Replace the fixed image tag `interview-app:1.0` with a Jenkins build number or Git commit ID.

Example:

```text
interview-app:${BUILD_NUMBER}
```

Versioned images make deployments traceable and allow rollback to a known working version.

### 3. Add safe deployment and rollback

* Add a Spring Boot health endpoint.
* Start the new container and verify its health.
* Remove the old container only after the new container is healthy.
* Keep the previous image version available for rollback.

### 4. Externalize configuration

* Use environment variables for ports, database URLs, usernames, and application configuration.
* Store secrets in Jenkins Credentials or a secret-management tool.
* Do not place passwords or API keys in the Dockerfile, deployment script, or Git repository.

### 5. Use Podman Compose or Docker Compose

Use a compose file to define the application, database, networks, environment variables, and persistent volumes in one deployment configuration.

### 6. Add reverse proxy and HTTPS

* Place Nginx or Caddy in front of the Spring Boot container.
* Expose HTTPS through the reverse proxy.
* Keep the application container on an internal application port.

### 7. Add monitoring and logging

* Monitor application logs.
* Expose and monitor health endpoints.
* Monitor container CPU and memory usage.
* Configure alerts for failed deployments, unavailable applications, and high resource usage.

## Future Kubernetes Learning Path

After strengthening the single-server container deployment, learn Kubernetes in this order:

```text
Pod → Deployment → Service → ConfigMap → Secret → Ingress → Rolling Update → Autoscaling
```

Kubernetes should be used when the application needs multiple services, self-healing, automated scaling, rolling deployments, or high availability across multiple servers.

## CV Summary

Built a Jenkins-based CI/CD pipeline for a Spring Boot application using Java 21, Maven, GitHub, archived artifacts, SSH, Podman, Oracle Linux, Dockerfile, and Bash deployment automation.
