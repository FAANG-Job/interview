# Interview

A minimal Java 21 Spring Boot REST API with Jenkins CI and local container-image support through Podman Desktop.

## Prerequisites

* Java 21
* Maven
* Windows with WSL 2, for local container execution
* Podman Desktop, for local container-image builds and execution

## Set Java 21

Update the JDK path for your local system:

```powershell
$env:JAVA_HOME = "C:\path\to\jdk-21"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"

java -version
```

## Run the application locally

```bash
mvn spring-boot:run
```

Health endpoint:

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

> **Note:** Docker Desktop could not be installed because of organization security and web-access restrictions. Podman Desktop is used as a Docker-compatible alternative. It can build and run standard Dockerfiles without Docker Desktop.

### Podman machine setup

Use a rootless Podman machine with user-mode networking:

```bash
podman machine stop
podman machine set --rootful=false --user-mode-networking=true
podman machine start
```

Verify that Podman networking works:

```bash
podman run --rm --network=pasta docker.io/library/hello-world
```

### Build the Java 21 image

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

### Run the container

```bash
podman run --rm --name interview-service --network=pasta -p 8080:8080 interview-service:java21
```

### Validate the containerized application

Open a second terminal and run:

```bash
curl.exe http://localhost:8080/api/health
```

To stop the container, press `Ctrl+C` in the terminal running the container.

## Jenkins CI pipeline

A Jenkins pipeline is configured to retrieve source code from this GitHub repository and validate changes.

### Current pipeline workflow

1. Create a feature branch.
2. Commit and push the changes to GitHub.
3. Create a pull request.
4. Jenkins retrieves the source code from GitHub.
5. Jenkins executes the Maven build and unit tests.
6. Review the build result before merging the pull request.

### Jenkins configuration

1. Open Jenkins and select **New Item**.

2. Enter a pipeline name, such as `interview-ci`.

3. Select **Pipeline**.

4. Under **Pipeline**, select **Pipeline script from SCM**.

5. Select **Git**.

6. Enter the GitHub repository URL.

7. Select the required branch.

8. Set the script path to:

   ```text
   Jenkinsfile
   ```

9. Save the configuration and select **Build Now**.

### Pipeline validation

The pipeline was intentionally tested with a failing unit test. Jenkins marked the build as failed, confirming that the CI configuration correctly detects test failures.

After correcting the unit test, run the pipeline again and confirm a successful build before merging the change.

> The Podman image-build stage can be added to Jenkins after local container execution and networking validation are complete.

## Issues encountered and resolutions

| Issue                                                   | Cause                                                                         | Resolution                                                                                       |
| ------------------------------------------------------- | ----------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------ |
| Docker Desktop installation was blocked                 | Organization security and web-access restrictions                             | Used Podman Desktop as a Docker-compatible local container platform                              |
| Java 21 runtime image was required                      | The application requires Java 21                                              | Used the Eclipse Temurin Java 21 runtime image                                                   |
| Container image required the built application artifact | Maven generates the Spring Boot JAR in `target`                               | Copied `target/interview-0.0.1-SNAPSHOT.jar` into the image                                      |
| `netavark` / `nftables` networking error                | The Podman machine was configured for rootful networking                      | Switched the Podman machine to rootless mode and will validate with `pasta` networking           |
| `useradd` build step failed                             | Podman encountered a network error while starting a temporary build container | Simplified the initial Dockerfile; non-root execution can be added after networking is validated |

## Evidence

Add screenshots to the following paths before committing them:

```text
assets/jenkins/jenkins-pipeline-config.png
assets/jenkins/jenkins-build-failure.png
assets/jenkins/jenkins-build-success.png
assets/podman/podman-image-build.png
assets/podman/podman-images-list.png
assets/podman/podman-container-running.png
```

Do not include passwords, tokens, credentials, or internal URLs in screenshots.

### Jenkins pipeline configuration

![Jenkins pipeline configuration](assets/jenkins/jenkins-pipeline-config.png)

### Jenkins build validation

![Jenkins build result](assets/jenkins/jenkins-build-success.png)

### Podman image build

![Podman image build](assets/podman/podman-image-build.png)

### Podman image verification

![Podman image list](assets/podman/podman-images-list.png)

### Local container execution

Add this screenshot only after the container starts successfully and the health endpoint responds:

![Running container](assets/podman/podman-container-running.png)

## Security

* Store GitHub credentials and tokens in Jenkins Credentials.
* Do not commit passwords, API keys, tokens, or private URLs.
* Use environment variables or Jenkins credentials for environment-specific configuration.

## Current status

The Java 21 container image was successfully built locally using Podman. Container execution and API validation are pending successful rootless `pasta` networking verification.
