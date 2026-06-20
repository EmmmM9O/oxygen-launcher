# Oxygen Launcher

一个用于在 Android 设备上启动 **Mindustry** 的启动器项目。

它的整体思路类似 [PojavLauncher](https://github.com/PojavLauncherTeam/PojavLauncher)：在 Android 上准备运行时环境，解压并管理随应用分发的 JRE，然后通过本地桥接层启动 `Mindustry.jar`。

## 项目简介

Oxygen Launcher 的目标不是移植 Mindustry，也不是重新实现游戏逻辑，而是以“启动器”的方式在 Android 上直接运行桌面版 `Mindustry.jar`。

当前仓库主要包含：

- Android 应用壳与入口 Activity
- Kotlin Multiplatform 编写的核心启动逻辑
- JNI / C++ 启动桥接代码
- 内置 JRE 资源与运行时文件管理
- 启动器附加模组与运行时资源

## 与 PojavLauncher 的区别

Oxygen Launcher 与 PojavLauncher 的核心差异在于图形后端适配方式。

这个项目并不是直接复用传统的桌面渲染路径，而是为 Mindustry 增加了一个基于 LWJGL、EGL 和 OpenGL ES 的启动器后端，并通过 [oxygen-launcher-api](https://github.com/EmmmM9O/oxygen-launcher-api) 与启动器本体通信。

你可以在 [oxygen-launcher-api](https://github.com/EmmmM9O/oxygen-launcher-api) 项目中找到：

- 启动器附加模组
- 启动器使用的附加后端

## 工作方式

项目大致按下面的流程运行：

1. 首次启动时准备工作目录与运行时资源
2. 按设备架构解压随应用分发的 JRE
3. 检查本地是否存在 `Mindustry.jar`
4. 如果缺少游戏本体，可由启动器下载官方发布版本
5. 为 Mindustry 注入 Oxygen Launcher 后端与启动器模组
6. 通过类似 Pojav 的 JNI / Launcher 方案在 Android 上启动 JVM
7. 加载 Mindustry 本体并附加运行参数

默认目标文件名为 `Mindustry.jar`。

## 适用场景

- 在 Android 设备上测试 Java 25 运行环境
- 在 Android 设备上配合 [mindustry-fabric-loader](https://github.com/Qendolin/mindustry-fabric-loader) 使用
- 研究 Mindustry 在 Android 上的启动链路
- 测试基于 ANGLE 的启动方案

## 项目结构

```text
android/   Android 应用模块
core/      启动逻辑、公共代码与 Android 桥接代码
gradle/    Gradle Wrapper 与版本配置
```

## 构建说明

这是一个基于 Gradle 的 Android 项目，主要技术栈包括：

- Kotlin
- Kotlin Multiplatform
- Android Gradle Plugin
- JNI / C++
- CMake

构建前请先准备：

- Android Studio 或可用的 Android SDK
- Android NDK
- JDK 与 Gradle 环境

常见构建命令：

```bash
./gradlew assembleDebug
```

## 当前状态

这是一个偏实验性、偏工程验证的启动器仓库，重点在于“以类似 Pojav 的方式在 Android 上启动 Mindustry”，而不是提供已经完整打磨的终端用户发行版。

如果你正在阅读这个项目，最值得关注的部分通常是：

- JVM 启动参数的组织方式
- Android 到 JNI 的桥接逻辑
- JRE 解压与运行时目录管理
- `Mindustry.jar` 的检测与下载流程

目前可在仓库的 Release 页面获取调试构建版本。
