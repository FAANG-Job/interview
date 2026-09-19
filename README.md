# Interview


``` Set java path
$env:JAVA_HOME = "C:\work\FAANG-JOBS\jdk-21.0.12.1"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
```
A minimal Java 21 Spring Boot REST API.


```bash
mvn spring-boot:run
```

Call `GET http://localhost:8080/api/health`.

## Test

```bash
mvn test
```
