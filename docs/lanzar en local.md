1. Compilar
$env:JAVA_HOME='C:\Users\x455035\2Programs\temurin-21.0.11'; $env:Path="$env:JAVA_HOME\bin;$env:Path"; & 'C:\Users\x455035\2Programs\maven-3.6.3\bin\mvn.cmd' -o -B "-Dmaven.repo.local=C:\Users\x455035\1Git\AprendiendoIA\ExampleProjects\dependecias" -Plocal-h2 compile

2. Ejecutar tests unitarios y de integración

$env:JAVA_HOME='C:\Users\x455035\2Programs\temurin-21.0.11'; $env:Path="$env:JAVA_HOME\bin;$env:Path"; & 'C:\Users\x455035\2Programs\maven-3.6.3\bin\mvn.cmd' -o -B "-Dmaven.repo.local=C:\Users\x455035\1Git\AprendiendoIA\ExampleProjects\dependecias" -Plocal-h2 verify

3. Arrancar la aplicación

$env:JAVA_HOME='C:\Users\x455035\2Programs\temurin-21.0.11'; $env:Path="$env:JAVA_HOME\bin;$env:Path"; & 'C:\Users\x455035\2Programs\maven-3.6.3\bin\mvn.cmd' -o -B "-Dmaven.repo.local=C:\Users\x455035\1Git\AprendiendoIA\ExampleProjects\dependecias" -Plocal-h2 "-Dspring-boot.run.profiles=local-h2" spring-boot:run
