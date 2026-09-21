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
* https://github.com/containers/netavark/issues/1495?utm_source=chatgpt.com


## Kubernetes learning with Kind in Ubuntu WSL2

Kubernetes learning is intentionally being introduced in small, verifiable steps before deploying the Java `interview-service`. The first workload is Nginx, which separates Kubernetes concepts from Spring Boot troubleshooting.

ELK remains outside Kubernetes in this phase. The existing Podman Compose/native Windows ELK workflow is unchanged.

### Prerequisites for local Kubernetes

Install and run the following tools inside Ubuntu WSL2:

* Podman
* `kubectl`
* Kind

Podman builds container images and runs the local Kind node container. Kind creates the local Kubernetes cluster. `kubectl` manages Kubernetes workloads inside that cluster.

```text
Podman → builds images and runs the Kind node
Kind → creates the local Kubernetes cluster
kubectl → manages Kubernetes resources in the cluster
```

### Install `kubectl`

```bash
sudo apt-get update
sudo apt-get install -y apt-transport-https ca-certificates curl gnupg

sudo mkdir -p -m 755 /etc/apt/keyrings
curl -fsSL https://pkgs.k8s.io/core:/stable:/v1.37/deb/Release.key \
  | sudo gpg --dearmor -o /etc/apt/keyrings/kubernetes-apt-keyring.gpg

echo 'deb [signed-by=/etc/apt/keyrings/kubernetes-apt-keyring.gpg] https://pkgs.k8s.io/core:/stable:/v1.37/deb/ /' \
  | sudo tee /etc/apt/sources.list.d/kubernetes.list

sudo apt-get update
sudo apt-get install -y kubectl

kubectl version --client
```

### Install Kind

```bash
curl -Lo kind https://kind.sigs.k8s.io/dl/v0.33.0/kind-linux-amd64
chmod +x kind
sudo install -m 0755 kind /usr/local/bin/kind
rm kind

kind version
podman version
```

### Create the local cluster

Kind is explicitly configured to use Podman in the current WSL2 terminal session:

```bash
export KIND_EXPERIMENTAL_PROVIDER=podman
```

`KIND_EXPERIMENTAL_PROVIDER` is the exact environment-variable name recognized by Kind. The value `podman` instructs Kind to use Podman instead of auto-detecting another runtime such as Docker.

Create the cluster:

```bash
kind create cluster --name faang-jobs --wait 5m
```

Verify the cluster:

```bash
kind get clusters
kubectl config current-context
kubectl get nodes
kubectl get pods --all-namespaces
kubectl get namespaces
```

Expected local cluster and context:

```text
Cluster: faang-jobs
Context: kind-faang-jobs
Node:    faang-jobs-control-plane
```

The initial cluster has one Kind node. Kubernetes system Pods and application Pods run on this node during the first learning phase.

### Create an application namespace

Application workloads are deployed to a dedicated namespace rather than `kube-system` or `default`.

```bash
kubectl create namespace faang-jobs-dev
kubectl get namespaces
```

`faang-jobs-dev` is a logical Kubernetes workspace for this project. A namespace organizes resources, avoids naming conflicts, and later supports permissions, quotas, and network policies. It is not a physical node boundary: Pods from one namespace can run on one or multiple nodes.

### Nginx first: Kubernetes learning workload

Nginx is used before the Java application to validate image loading, Deployments, Pods, Services, scaling, logs, and self-healing without application-specific complexity.

Download Nginx with Podman:

```bash
podman pull docker.io/library/nginx:alpine
```

Load the image into the Kind node. The image-archive workflow is used because `kind load docker-image` did not detect a locally available Podman image in this WSL2 environment.

```bash
podman save --output nginx-alpine.tar docker.io/library/nginx:alpine
kind load image-archive nginx-alpine.tar --name faang-jobs
```

Verify images cached inside the Kind node:

```bash
podman exec faang-jobs-control-plane crictl images
podman exec faang-jobs-control-plane crictl images | grep nginx
```

Create a single Nginx replica and expose it with a Kubernetes Service:

```bash
kubectl create deployment nginx-test \
  --image=docker.io/library/nginx:alpine \
  --replicas=1 \
  --namespace=faang-jobs-dev

kubectl expose deployment nginx-test \
  --port=80 \
  --target-port=80 \
  --namespace=faang-jobs-dev
```

Verify the resources:

```bash
kubectl get all -n faang-jobs-dev
kubectl get pods -n faang-jobs-dev -o wide
kubectl get services -n faang-jobs-dev -o wide
```

Access the Service locally:

```bash
kubectl port-forward -n faang-jobs-dev service/nginx-test 8081:80
```

In a second terminal:

```bash
curl http://127.0.0.1:8081
```

> **Note:** `kubectl port-forward service/nginx-test ...` selects one backing Pod for the forwarding session. It is useful for local access but does not by itself demonstrate load balancing across all replicas.

### Scale to three replicas

Scale the Deployment to three Nginx Pods:

```bash
kubectl scale deployment nginx-test \
  --replicas=3 \
  --namespace=faang-jobs-dev

kubectl get pods -n faang-jobs-dev -o wide
```

The Deployment maintains the requested replica count. Each Pod has a unique Pod IP, while the `nginx-test` Service provides one stable ClusterIP and routes to ready Pods matching the label `app=nginx-test`.

### View Pod logs

View logs for one Pod:

```bash
kubectl logs nginx-test-<pod-id> -n faang-jobs-dev
```

Follow logs continuously:

```bash
kubectl logs -f nginx-test-<pod-id> -n faang-jobs-dev
```

View logs from all Nginx Pods:

```bash
kubectl logs -n faang-jobs-dev \
  -l app=nginx-test \
  --prefix=true \
  --all-containers=true
```

### Test Pod self-healing

Open one terminal and watch Pod changes:

```bash
kubectl get pods -n faang-jobs-dev -w
```

In a second terminal, delete one specific Pod:

```bash
kubectl delete pod nginx-test-<pod-id> -n faang-jobs-dev
```

The Deployment controller detects that the actual Pod count is lower than the desired replica count and quickly creates a replacement Pod with a new name. This validates Kubernetes self-healing.

Do not use `kubectl delete pod` to stop an application permanently: a Deployment recreates the Pod. To temporarily stop the Nginx workload while retaining the Deployment, Service, and cached image:

```bash
kubectl scale deployment nginx-test \
  --replicas=0 \
  --namespace=faang-jobs-dev
```

Start it again later:

```bash
kubectl scale deployment nginx-test \
  --replicas=1 \
  --namespace=faang-jobs-dev
```

### Next Kubernetes steps

After validating Nginx, deploy `interview-service` into `faang-jobs-dev` using the same image-build and Kind image-loading workflow. The next application-focused steps are:
1. Deploy one Java service replica.
2. Validate the `/api/health` endpoint.
3. Add readiness, liveness, and startup probes.
4. Define CPU and memory requests and limits.
5. Scale to three replicas.
6. Repeat self-healing and Service-routing tests.
7. Keep ELK external to Kubernetes until the core application deployment is understood and stable.
8. Next would Deploy angular app in another namespace


