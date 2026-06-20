# Oxygen Launcher

An Android launcher project for running **Mindustry Java Edition**.

Its overall approach is similar to [PojavLauncher](https://github.com/PojavLauncherTeam/PojavLauncher): prepare a runtime environment on Android, unpack and manage the bundled JRE, and then launch `Mindustry.jar` through a local bridge layer.

[中文说明](./README_zh.md)

## Overview

Oxygen Launcher is not a Mindustry port and does not reimplement the game. Its goal is to run the desktop `Mindustry.jar` directly on Android through a launcher-based approach.

This repository mainly includes:

- Android app shell and entry activity
- Core launch logic written with Kotlin Multiplatform
- JNI / C++ launcher bridge code
- Bundled JRE assets and runtime file management
- Launcher add-on mods and runtime resources

## How It Differs from PojavLauncher

The main difference from PojavLauncher is the graphics backend adaptation strategy.

Instead of relying on the standard desktop rendering path, this project adds a launcher backend for Mindustry based on LWJGL, EGL, and OpenGL ES, and communicates with the launcher through [oxygen-launcher-api](https://github.com/EmmmM9O/oxygen-launcher-api).

The [oxygen-launcher-api](https://github.com/EmmmM9O/oxygen-launcher-api) project contains:

- Launcher add-on mods
- The extra backend used by the launcher

## How It Works

The runtime flow is roughly:

1. Prepare working directories and runtime resources on first launch
2. Unpack the bundled JRE for the current device architecture
3. Check whether `Mindustry.jar` exists locally
4. Download the official game jar if it is missing
5. Inject the Oxygen Launcher backend and launcher mod into Mindustry
6. Start the JVM on Android using a Pojav-like JNI / launcher approach
7. Load Mindustry with the required runtime arguments

The default target file is `Mindustry.jar`.

## Use Cases

- Test a Java 25 runtime on Android
- Use it together with [mindustry-fabric-loader](https://github.com/Qendolin/mindustry-fabric-loader) on Android
- Study the Android launch chain for Mindustry
- Experiment with an ANGLE-based launch path

## Project Structure

```text
android/   Android application module
core/      Launch logic, shared code, and Android bridge code
gradle/    Gradle wrapper and version configuration
```

## Build

This is a Gradle-based Android project built with:

- Kotlin
- Kotlin Multiplatform
- Android Gradle Plugin
- JNI / C++
- CMake

Before building, prepare:

- Android Studio or a working Android SDK
- Android NDK
- JDK and Gradle environment

Common build command:

```bash
./gradlew assembleDebug
```

## Status

This repository is currently experimental and engineering-oriented. Its focus is launching Mindustry on Android through a Pojav-like approach rather than providing a polished end-user distribution.

If you are reading the codebase, the most relevant parts are usually:

- JVM launch argument assembly
- Android-to-JNI bridge logic
- JRE extraction and runtime directory management
- `Mindustry.jar` detection and download flow

Debug builds are currently available from the repository Releases page.

## Notice

Mindustry and its related assets belong to their original authors and projects. This repository mainly provides the launcher implementation and is not the game itself.
