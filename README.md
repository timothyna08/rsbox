# RSBox

Kotlin game server and bundled development client for revision 217.

## Requirements

- A Java 11 JDK. Use Java 11 for both Gradle and the client, which uses the Java Applet APIs.
- Internet access for Gradle dependencies and the initial game cache download.
- Free local ports 43594 (game) and 80 (HTTP).

The Gradle wrapper supplies Gradle 8.14. Kotlin 2.2.10 matches the Kotlin metadata in the available OpenRS2 snapshot; the original Kotlin 1.9 compiler cannot read that dependency.

## Windows quick start

Open a terminal in this repository. The Windows launcher uses the Java 11 toolchain in `%USERPROFILE%\.gradle\jdks\adoptium-11-x64-hotspot-windows` when present. Otherwise, set `JAVA_HOME` to your Java 11 JDK first.

```powershell
.\rsbox.cmd build
.\rsbox.cmd setup
.\rsbox.cmd server
```

Keep the server running and open a second terminal in this repository:

```powershell
.\rsbox.cmd client
```

The client connects to `127.0.0.1`. A new local username and password create a player on first login. Use a password reserved for local development.

Setup downloads [OpenRS2 cache 1563](https://archive.openrs2.org/caches/runescape/1563) and its matching XTEA keys, creates `data/configs/server.toml`, and generates RSA keys for the server and client. Run setup once. It refuses to run when `data/` already exists; preserve that directory because it contains player saves and private keys.

JRebel is disabled by default. Developers who use it can pass `-Pjrebel=true` to Gradle.

## IntelliJ IDEA

Open the repository as a Gradle project, use the Gradle wrapper, and select a Java 11 JDK for both the project SDK and Gradle JVM. The checked-in run configurations provide `server [setup]`, `server [run]`, and `client [run]`. Run the server before the client.

## Direct Gradle commands

With `JAVA_HOME` set to a Java 11 JDK:

```powershell
.\gradlew.bat build
.\gradlew.bat ":server:setup server"
.\gradlew.bat ":server:run server"
.\gradlew.bat ":client:run client"
```

On macOS or Linux, use `./gradlew` instead of `.\gradlew.bat`.

## Local configuration

Server settings live in `data/configs/server.toml`. The bundled client and cache use revision 217, so keep the server revision at 217. For a server restricted to this computer, set the network address to `127.0.0.1`.

The `data/` directory and generated `modulus.txt` files are ignored by Git. The client resource `client/src/main/resources/modulus.txt` must match `data/rsa/modulus.txt`; the setup task creates both together.
