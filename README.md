# Interview

A minimal Java 21 Spring Boot REST API with Jenkins CI, Podman containerization, and ELK observability configuration.

## Prerequisites

* Java 21
* Maven
* Windows with WSL 2
* Podman Desktop

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

## Run unit tests

```bash
mvn test
```

## Build the application JAR

```bash
mvn clean package -DskipTests
```

The generated JAR is:

```text
target/interview-0.0.1-SNAPSHOT.jar
```

## Local container execution with Podman Desktop

`Dockerfile.java21` packages the application as a Java 21 container image.

> **Note:** Docker Desktop could not be installed because of organization security and web-access restrictions. Podman Desktop is used as a Docker-compatible alternative.

### Podman machine setup

Configure a rootless Podman machine with user-mode networking:

```bash
podman machine stop
podman machine set --rootful=false --user-mode-networking=true
podman machine start
```

Verify rootless networking:

```bash
podman info --format '{{.Host.Security.Rootless}}'
podman run --rm --network=pasta docker.io/library/hello-world
```

Expected rootless output:

```text
true
```

### Build the Java 21 image

```bash
podman build -t interview-service:java21 -f Dockerfile.java21 .
```

Verify the image:

```bash
podman images
```

### Run the container

```bash
podman run --rm --name interview-service --network=pasta -p 8080:8080 interview-service:java21
```

Validate the containerized application:

```bash
curl.exe http://localhost:8080/api/health
```

To stop the container, press `Ctrl+C`.

## Jenkins CI pipeline

A Jenkins pipeline is configured to retrieve source code from this GitHub repository and validate changes.

### Pipeline workflow

1. Create a feature branch.
2. Commit and push changes to GitHub.
3. Create a pull request.
4. Jenkins retrieves the source code from GitHub.
5. Jenkins executes the Maven build and unit tests.
6. Review the build result before merging the pull request.

### Pipeline validation

The pipeline was intentionally tested with a failing unit test. Jenkins marked the build as failed, confirming that the CI configuration correctly detects test failures.

After correcting the unit test, run the pipeline again and confirm a successful build before merging the change.

## ELK observability configuration

`compose.observability.yaml` defines the following Elastic Stack components:

* Elasticsearch
* Kibana
* Logstash
* Filebeat

All components use Elastic version `9.5.4`.

### Download ELK images

Download the images defined in the Compose file:

```bash
podman compose -f compose.observability.yaml pull
```

The following images are downloaded:

```text
docker.elastic.co/elasticsearch/elasticsearch:9.5.4
docker.elastic.co/kibana/kibana:9.5.4
docker.elastic.co/logstash/logstash:9.5.4
docker.elastic.co/beats/filebeat:9.5.4
```

### ELK image-download evidence

![ELK image download](assets/observability/download-elk-images.png)

![ELK images pulling](assets/observability/download-elk-pulling.png)

![ELK images pulled successfully](assets/observability/download-elk-pulled.png)

### Compose bridge-network limitation

The ELK images download successfully through Podman. However, starting the multi-container Compose stack is currently blocked by a Podman-on-WSL `netavark`/`nftables` bridge-network error.

The single-container application works successfully with rootless `pasta` networking. The ELK stack continues to run through the native Windows installation of Filebeat, Logstash, Elasticsearch, and Kibana.

## Issues encountered and resolutions

| Issue                                                   | Cause                                                                         | Resolution                                                                                                      |
| ------------------------------------------------------- | ----------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------- |
| Docker Desktop installation was blocked                 | Organization security and web-access restrictions                             | Used Podman Desktop as a Docker-compatible local container platform                                             |
| Java 21 runtime image was required                      | The application requires Java 21                                              | Used the Eclipse Temurin Java 21 runtime image                                                                  |
| Container image required the built application artifact | Maven generates the Spring Boot JAR in `target`                               | Copied `target/interview-0.0.1-SNAPSHOT.jar` into the image                                                     |
| Podman rootful network error                            | `netavark` / `nftables` failed with rootful networking                        | Switched to rootless mode and validated single-container execution with `pasta`                                 |
| ELK Compose bridge-network error                        | Podman-on-WSL bridge networking remains affected by `netavark` / `nftables`   | Kept the Compose configuration and image-download workflow; continued using the native Windows ELK installation |
| `useradd` build step failed                             | Podman encountered a network error while starting a temporary build container | Simplified the initial Dockerfile; non-root execution can be added after full network validation                |

## Evidence

```text
assets/
  jenkins/
    jenkins-pipeline-config.png
    jenkins-build-failure.png
    jenkins-build-success.png
  podman/
    podman-image-build.png
    podman-images-list.png
    podman-container-running.png
  observability/
    download-elk-images.png
    download-elk-pulling.png
    download-elk-pulled.png
```

Do not include passwords, tokens, Jenkins credentials, or internal URLs in screenshots.

### Jenkins pipeline configuration

![Jenkins pipeline configuration](assets/jenkins/jenkins-pipeline-config.png)

### Jenkins build validation

![Jenkins build result](assets/jenkins/jenkins-build-success.png)

### Podman image build

![Podman image build](assets/podman/podman-image-build.png)

### Podman image verification

![Podman image list](assets/podman/podman-images-list.png)

### Local container execution

![Running container](assets/podman/podman-container-running.png)

## Security

* Store GitHub credentials and tokens in Jenkins Credentials.
* Do not commit passwords, API keys, tokens, or private URLs.
* Use environment variables or Jenkins credentials for environment-specific configuration.

## Current status

* Jenkins CI pipeline configured and validated with a controlled unit-test failure.
* Java 21 application image successfully built with Podman.
* Single-container application execution validated with rootless `pasta` networking.
* ELK images successfully downloaded using `compose.observability.yaml`.
* Native Windows ELK installation remains the active local observability runtime.
* ELK Compose startup is pending resolution of the Podman/WSL bridge-network limitation.
