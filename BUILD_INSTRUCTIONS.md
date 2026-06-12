# Board Clock (底座开发板时钟仪表盘) - 本地打包编译指南

这是一份专为开发人员准备的本地编译打包指南。当您在 **Google AI Studio** 中点击 **Export -> Download as .zip file** 下载了完整的项目源码后，可以按照本指南在您自己的电脑（Windows / macOS / Linux）上直接编译、打包生成 Android APK，并安装到您的开发板中。

---

## 📋 准备工作

在本地环境下编译 Android 项目，您需要准备以下环境：

### 1. 安装 Java 开发工具包 (JDK) - **必须为 JDK 17**
本项目的 Gradle 已经适配了 Java 17。
* **下载地址**：推荐使用 [Azul Zulu JDK 17](https://www.azul.com/downloads/?version=java-17-lts&package=jdk) 或 Oracle JDK 17。
* **验证安装**：
  打开您电脑的终端（Terminal / CMD / PowerShell），输入以下命令检查版本：
  ```bash
  java -version
  ```
  *确认输出中的主版本号为 `17`。*

### 2. 双通道编译方案选择（推荐方法一）

* **🍏 方案一：使用 Android Studio 编译（最推荐，简单、可视化）**
  * **下载**：下载并安装最新版的 [Android Studio](https://developer.android.com/studio)。
  * Android Studio 会自动为您配置好 Android SDK、NDK、构建工具以及内置的 Java 环境，极其适合带有开发板的调试工作。

* **💻 方案二：使用 命令行 (Command Line) 编译（轻量，无需打开笨重 IDE）**
  * 您不需要安装庞大的 Android Studio，但电脑里需要配置好 `ANDROID_HOME` 环境变量并安装了 Android Command-line Tools。

---

## 🛠️ 详细打包步骤

### 💡 方法一：使用 Android Studio 全自动打包 (推荐)

1. **解压源码包**：将下载得到的 `.zip` 压缩文件解压到您电脑的某个纯英文路径目录下。
2. **导入项目**：
   * 打开 Android Studio，在 Welcome 界面点击 **Open**。
   * 选择您刚才解压出来的目录（选择包含有 `settings.gradle.kts` 的根文件夹）。
   * 等待 Android Studio 自动进行首次 Sync (同步库依赖) 过程。因为需要下载各种依赖，这可能需要 1~5 分钟，具体取决于您的网络环境（如果在中国大陆，建议配置国内 Gradle 镜像源或开代理加速）。
3. **一键生成 Debug APK**：
   * 在顶部菜单栏，点击：**Build** -> **Build Bundle(s) / APK(s)** -> **Build APK(s)**。
   * 编译完成后，右下角会弹出气泡提示。点击气泡里的 **locate** 按钮，系统会立即打开文件浏览器，定位到生成的新 debug 倾力打包好的 APK 文件。
   * **输出路径**：`app/build/outputs/apk/debug/app-debug.apk`

---

### 💻 方法二：使用命令行 (Terminal) 极速打包

如果您习惯在黑窗口中处理，可以利用项目自带的 Gradle Wrapper 执行脚本一键编译：

1. **打开命令行终端**，使用 `cd` 命令切入到解压后的项目根目录下。
2. **赋予 Gradle 执行权限 (仅限 macOS / Linux 用户)**：
   ```bash
   chmod +x gradlew
   ```
   *(Windows 用户可以忽略这一步，直接使用 `gradlew.bat`)*

3. **执行编译打包命令**：
   * **Windows (CMD / PowerShell)**:
     ```cmd
     .\gradlew.bat assembleDebug
     ```
   * **macOS / Linux**:
     ```bash
     ./gradlew assembleDebug
     ```

4. **提取 APK**：
   编译成功后（显示 `BUILD SUCCESSFUL`），生成的 APK 文件将保存在：
   `app/build/outputs/apk/debug/app-debug.apk`

---

## 🔌 怎么部署安装到开发板？

在本地打包出 `app-debug.apk` 后，您可以通过以下几种方式将其运行在开发板上。

### 1. 使用 ADB 命令线刷（极速、最专业）
如果开发板已经通过 Type-C 或网线（ADB over Wi-Fi）连接到了您的电脑：
1. 确保电脑已安装 adb 工具，并且开发板开启了 **开发人员选项 -> USB 调试**。
2. 打开终端，输入命令检查连接：
   ```bash
   adb devices
   ```
3. 执行安装命令：
   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

### 2. 使用 U 盘/TF 卡进行物理拷贝安装
1. 将打包好的 `app-debug.apk` 拷贝到 U 盘或 SD 卡中。
2. 插到开发板的 USB 或卡槽接口。
3. 在开发板的 Android 系统中打开“文件浏览器”（File Manager）。
4. 找到 U 盘，双击 `app-debug.apk` 并在提示中授予安装未知源应用的权限，一键完成覆盖安装。

---

## ❓ 常见问题与报错排查 (Troubleshooting)

#### Q1: 编译时报错 `Unsupported class file major version 65` 或者 `SDK location not found`?
* **A**: 
  1. `Unsupported class file major version` 说明您的电脑上配置的 Java 版本（如 JDK 21）与当前 Gradle 支持的版本冲突。请前往 Android Studio 的 **Settings -> Build, Execution, Deployment -> Build Tools -> Gradle** 中，将 **Gradle JDK** 修改为指定的 **JDK 17**。
  2. `SDK location not found` 说明 Gradle 找不到您的 Android SDK。您可以在解压的项目根目录下，手动创建一个名为 `local.properties` 的文本文件，在里边写上您的 SDK 路径，例如：
     ```properties
     sdk.dir=C\:\\Users\\您的用户名\\AppData\\Local\\Android\\Sdk
     ```
     *(macOS 路径类似于 `sdk.dir=/Users/您的用户名/Library/Android/sdk`)*

#### Q2: 既然代码是兼容的，为什么本地打包的包名或签名与我线上板子已有的版本冲突？
* **A**: 本项目使用动态自适应签名与调试阶段 `debug.keystore` 进行签名构建。如果您发现本地生成的 APK 覆盖安装报错（"应用未安装" 或 "签名不一致"），请先通过 ADB 或开发板设置命令行卸载板子里的老版本，再重新安装。
  *(卸载命令: `adb uninstall com.example` 或对应的 applicationId)*

---
🚀祝您在开发板上玩的开心！如需进一步修改时钟、添加感知传感器代码或硬件映射绑定，也随时欢迎提问。
