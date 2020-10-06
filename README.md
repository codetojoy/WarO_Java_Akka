
WarO_Java_Akka
=========

Work-in-Progress!!! ... a Java submission for War-O as a code exercise

* this project uses:
    - Akka actor framework
    - Java 10 `var`
    - Java 15 (second preview) `record`
    - Java 15 (preview) `sealed`
    - Spring's Java configuration is used to configure players

To Build:
---------

* requires JDK 15+

useful commands:

* `./gradlew clean test`
    - on Windows, use `gradlew.bat`
* `./gradlew run`
* `./gradlew build`

See test output in `~/build/reports/tests/index.html`


To Run:
---------

* configure `src/main/java/org/peidevs/waro/config/Config.java`
* `./run.sh`
    - edit `build.gradle` to use Akka or a simple console app (for trouble-shooting)

Rules:
---------

Rules are [here](Rules.md).
