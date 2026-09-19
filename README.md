# Interview

A minimal Java 21 Spring Boot REST API.

## Prerequisites

* Java 21
* Maven
* Podman Desktop for local container execution

## Set Java 21

Update the JDK path for your local system:

```powershell
$env:JAVA_HOME = "C:\path\to\jdk-21"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"

java -version
```

## Run locally

```bash
mvn spring-boot:run
```

Call the health endpoint:

```text
GET http://localhost:8080/api/health
```

## Test

```bash
mvn test
```

## Local container execution with Podman Desktop

This project includes `Dockerfile.java21` to package the Spring Boot application as a Java 21 container image.

> **Note:** Docker Desktop could not be installed because of organization security and web-access restrictions. This project therefore uses **Podman Desktop**, a Docker-compatible container platform. Podman can build and run standard Dockerfiles without requiring Docker Desktop.

### Podman setup

* Windows with WSL 2 enabled
* Podman Desktop installed
* A rootless Podman machine running

Configure the Podman machine for rootless execution and user-mode networking:

```bash
podman machine stop
podman machine set --rootful=false --user-mode-networking=true
podman machine start
```

Verify that Podman networking is working:

```bash
podman run --rm --network=pasta docker.io/library/hello-world
```

### Dockerfile

The application image is defined in:

```text
Dockerfile.java21
```

```dockerfile
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

COPY target/interview-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

### Build the application JAR

```bash
mvn clean package -DskipTests
```

### Build the container image

Run this command from the project root:

```bash
podman build -t interview-service:java21 -f Dockerfile.java21 .
```

Verify the image:

```bash
podman images
```

Expected image:

```text
localhost/interview-service   java21
```

### Run the container locally

```bash
podman run --rm --name interview-service --network=pasta -p 8080:8080 interview-service:java21
```

The application is exposed on:

```text
http://localhost:8080
```

### Validate the containerized application

Open a second terminal and run:

```bash
curl.exe http://localhost:8080/api/health
```

To stop the container, press `Ctrl+C` in the terminal running the container.

## Issues encountered and resolutions

| Issue                                                   | Cause                                                                        | Resolution                                                                                  |
| ------------------------------------------------------- | ---------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------- |
| Docker Desktop installation was blocked                 | Organization security and web-access restrictions                            | Used Podman Desktop as a Docker-compatible local container platform                         |
| Java 21 runtime image was required                      | The application requires Java 21                                             | Used `eclipse-temurin:21-jre-jammy` as the base image                                       |
| Container image required the built application artifact | Maven creates the Spring Boot JAR in the `target` directory                  | Copied `target/interview-0.0.1-SNAPSHOT.jar` into the container image                       |
| `netavark` / `nftables` networking error                | The initial Podman machine was configured for rootful networking             | Switched the Podman machine to rootless mode and will validate with `pasta` networking      |
| `useradd` build step failed                             | Podman encountered a network error when starting a temporary build container | Simplified the initial Dockerfile; non-root execution can be added after network validation |

## Current status

The Java 21 container image was successfully built locally using Podman. Container execution and API validation will be confirmed after rootless `pasta` networking is verified.
