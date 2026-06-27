# Board Clock (底座开发板时钟仪表盘) - 打包与本地依赖配置指南

本文档详细介绍了如何使用项目自带的打包脚本生成 Android APK，以及项目如何通过本地化缓存和国内镜像源实现“多环境即拉即编译”与“完全离线打包”。

---

## 🛠️ 打包脚本使用指南

为了简化编译过程，项目根目录下提供了针对不同操作系统的自动化打包脚本。它们会自动检测 Java 环境，在本地创建 `.gradle_home` 缓存依赖，并输出 APK。

### 🍏 macOS / Linux 用户
脚本文件：[build_apk.sh](file:///Users/zch/Documents/code/board-clock/build_apk.sh)

在终端运行以下命令：
* **打包 Debug 版本 (默认)**：
  ```bash
  ./build_apk.sh
  ```
* **打包 Release 版本**：
  ```bash
  ./build_apk.sh release
  ```

> [!NOTE]
> 如果首次运行提示 `Permission denied`，请先为脚本赋予执行权限：
> `chmod +x build_apk.sh`

### 💻 Windows 用户
脚本文件：[build_apk.bat](file:///Users/zch/Documents/code/board-clock/build_apk.bat)

在命令行 (CMD) 中运行：
* **打包 Debug 版本 (默认)**：
  ```cmd
  build_apk.bat
  ```
* **打包 Release 版本**：
  ```cmd
  build_apk.bat release
  ```

---

## 📦 本地依赖缓存设计 (.gradle_home)

为了避免将大量的编译环境及第三方依赖库误提交到 Git 仓库，同时又能在本地快速进行编译，本项目采用**“本地化缓存 + 忽略提交”**的策略：

1. **缓存目录重定向**：
   打包脚本执行时，会自动将环境变量 `GRADLE_USER_HOME` 重定向到项目根目录下的 `.gradle_home/`。这意味着所有的 Gradle 运行引擎（约 130MB）及下载的第三方 Maven 依赖包（如 AndroidX, Kotlin 等，共约 500MB ~ 1.5GB）都会被下载到项目内的 `.gradle_home/` 目录下。
   
2. **轻量 Git 库**：
   在 [.gitignore](file:///Users/zch/Documents/code/board-clock/.gitignore) 文件中已添加忽略规则：
   ```text
   .gradle_home
   ```
   这保证了您在克隆或提交代码时，Git 仓库非常轻量（只有几十 KB 的纯代码），而不会携带数 G 的二进制缓存。

---

## ⚡ 国内镜像加速配置 (即拉即编译)

在更换新的工作环境（如公司电脑）并重新 `git clone` 代码后，您不需要配置繁琐的代理，项目内置了国内高速镜像源：

* **Gradle 引擎下载**：
  在 [gradle-wrapper.properties](file:///Users/zch/Documents/code/board-clock/gradle/wrapper/gradle-wrapper.properties) 中，已将官方下载链接替换为**腾讯云镜像源**：
  `distributionUrl=https\://mirrors.cloud.tencent.com/gradle/gradle-9.5.1-bin.zip`
* **插件及第三方库依赖**：
  在 [settings.gradle.kts](file:///Users/zch/Documents/code/board-clock/settings.gradle.kts) 中，配置了优先使用**阿里云 Maven 镜像源**：
  `https://maven.aliyun.com/repository/google` 与 `https://maven.aliyun.com/repository/central`

**编译体验**：在新电脑上首次运行打包脚本时，Gradle 引擎和第三方依赖会通过国内 CDN 以几秒钟的速度自动拉取并缓存到项目本地，绝不会出现网络超时的情况。

---

## 🔌 物理离线打包 (U 盘拷贝)

如果您需要将项目拷贝到完全**断网/没有外网连接**的环境中开发：

1. 先在一台有网的电脑上成功运行一次打包脚本，使所有依赖库被完整拉取到本地 `.gradle_home/` 目录中。
2. 将包含 `.gradle_home/` 的**整个项目文件夹**拷贝到 U 盘或移动硬盘中。
3. 将项目拷贝进离线电脑中（注意：目标电脑需装有对应的 Java/JDK 21 环境，且操作系统与有网的电脑一致，如都是 Windows）。
4. 在离线电脑直接运行打包脚本，即可在 **1 秒内直接离线编译并生成 APK**！

---

## 📂 APK 输出与部署

编译成功后，生成的 APK 将被统一重命名并输出至 [dist/](file:///Users/zch/Documents/code/board-clock/dist) 目录：
* **[board-clock-[debug/release]-latest.apk](file:///Users/zch/Documents/code/board-clock/dist/board-clock-debug-latest.apk)**：最新一次成功编译生成的包（会覆盖历史的 latest 包）。
* **board-clock-[debug/release]-[timestamp].apk**：带时间戳的归档文件（作为历史版本备份）。

### 部署安装至开发板 (ADB)
如果您的电脑已通过 USB 或 Wi-Fi 连上开发板，且配置了 ADB 环境变量，可通过以下命令一键安装：
```bash
adb install -r dist/board-clock-debug-latest.apk
```
