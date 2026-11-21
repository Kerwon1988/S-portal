1. Compilar
$env:JAVA_HOME='C:\Users\x455035\2Programs\temurin-21.0.11'; $env:Path="$env:JAVA_HOME\bin;$env:Path"; & 'C:\Users\x455035\2Programs\maven-3.6.3\bin\mvn.cmd' -o -B "-Dmaven.repo.local=C:\Users\x455035\1Git\AprendiendoIA\ExampleProjects\dependecias" -Plocal-h2 compile

2. Ejecutar solo los tests relacionados con las clases modificadas, añadidas o impactadas. Sustituye `NombreTest`/`NombreIT` por las clases aplicables; ejecuta todos los tests focales necesarios para cubrir el cambio:

$env:JAVA_HOME='C:\Users\x455035\2Programs\temurin-21.0.11'; $env:Path="$env:JAVA_HOME\bin;$env:Path"; & 'C:\Users\x455035\2Programs\maven-3.6.3\bin\mvn.cmd' -o -B "-Dmaven.repo.local=C:\Users\x455035\1Git\AprendiendoIA\ExampleProjects\dependecias" -Plocal-h2 "-Dtest=NombreTest" test

$env:JAVA_HOME='C:\Users\x455035\2Programs\temurin-21.0.11'; $env:Path="$env:JAVA_HOME\bin;$env:Path"; & 'C:\Users\x455035\2Programs\maven-3.6.3\bin\mvn.cmd' -o -B "-Dmaven.repo.local=C:\Users\x455035\1Git\AprendiendoIA\ExampleProjects\dependecias" -Plocal-h2 "-Dtest=NombreTest" "-Dit.test=NombreIT" integration-test

La suite completa (`verify` sin selectores) queda reservada como último recurso si no existe una selección focal fiable o si un criterio de aceptación la exige explícitamente; documenta el motivo cuando la ejecutes.

3. Arrancar la aplicación

$env:JAVA_HOME='C:\Users\x455035\2Programs\temurin-21.0.11'; $env:Path="$env:JAVA_HOME\bin;$env:Path"; & 'C:\Users\x455035\2Programs\maven-3.6.3\bin\mvn.cmd' -o -B "-Dmaven.repo.local=C:\Users\x455035\1Git\AprendiendoIA\ExampleProjects\dependecias" -Plocal-h2 "-Dspring-boot.run.profiles=local-h2" spring-boot:run




clear
$env:JAVA_HOME='C:\Users\x455035\2Programs\temurin-21.0.11'; $env:Path="$env:JAVA_HOME\bin;$env:Path"; $mvn='C:\Users\x455035\2Programs\maven-3.6.3\bin\mvn.cmd'; $repo='C:\Users\x455035\1Git\AprendiendoIA\ExampleProjects\dependecias'; & $mvn -o -B "-Dmaven.repo.local=$repo" -Plocal-h2 spring-boot:run '-Dspring-boot.run.profiles=local-h2,e2e' '-Dspring-boot.run.arguments=--server.port=8090 --portal.simbiosphere.client.enabled=true --portal.simbiosphere.client.base-url=http://localhost:8081 --portal.simbiosphere.client.key-id=s-portal-local-key --portal.simbiosphere.client.secret=sportal-local-contract-secret-20261121 --portal.simbiosphere.worker.enabled=true'