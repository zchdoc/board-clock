@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

echo ==================================================
echo            Board Clock APK 打包脚本 (Windows)
echo ==================================================

:: 1. 检查 Java 环境
where java >nul 2>nul
if %errorlevel% neq 0 (
    echo [错误] 未检测到 Java 环境，请先安装 JDK 21+。
    echo 推荐下载 Azul Zulu JDK 21 或 Oracle JDK 21。
    pause
    exit /b 1
)

for /f "tokens=3" %%g in ('java -version 2^>^&1 ^| findstr /i "version"') do (
    set JAVA_VER=%%g
    set JAVA_VER=!JAVA_VER:"=!
)
echo [信息] 检测到 Java 版本: !JAVA_VER!

:: 2. 检查 Android SDK 环境变量或 local.properties
set SDK_DIR=
if exist local.properties (
    for /f "tokens=1,2 delims==" %%i in (local.properties) do (
        set key=%%i
        set val=%%j
        :: 去除空格和转义符
        if "!key!"=="sdk.dir" (
            set SDK_DIR=!val:\\=\!
        )
    )
)

if "!SDK_DIR!"=="" (
    if "%ANDROID_HOME%"=="" (
        echo [警告] 未检测到 Android SDK 路径 (sdk.dir 或 ANDROID_HOME)。
        echo 请在根目录创建 local.properties 并写入：sdk.dir=C:\Users\您的用户名\AppData\Local\Android\Sdk
    )
)

:: 3. 代理检测 (检测 127.0.0.1:10809 是否开启)
set USE_PROXY=0
netstat -ano | findstr 127.0.0.1:10809 >nul 2>nul
if %errorlevel% equ 0 (
    echo [提示] 检测到本地代理端口 127.0.0.1:10809 已开启。
    echo [提示] 将自动设置代理加速 Gradle 依赖下载。
    set USE_PROXY=1
) else (
    echo [信息] 未检测到代理端口 127.0.0.1:10809，将直连网络。
)

:: 4. 确定打包模式
set BUILD_TYPE=debug
set GRADLE_TASK=assembleDebug

if "%1"=="release" (
    set BUILD_TYPE=release
    set GRADLE_TASK=assembleRelease
)

echo [信息] 当前打包模式: %BUILD_TYPE% (任务: %GRADLE_TASK%)

:: 5. 确保 gradlew.bat 存在
if not exist gradlew.bat (
    echo [错误] 未在根目录找到 gradlew.bat 脚本。
    pause
    exit /b 1
)

:: 6. 执行打包
echo [开始] 开始编译构建...
echo [信息] 已将 Gradle 缓存目录设置为当前项目下的 .gradle_home/
set GRADLE_USER_HOME=%~dp0.gradle_home

:: 动态配置本地 Gradle 守护进程代理属性，确保下载依赖时能够通过代理
set LOCAL_PROPERTIES_FILE=%~dp0.gradle_home\gradle.properties
if not exist "%~dp0.gradle_home" mkdir "%~dp0.gradle_home"

if %USE_PROXY% equ 1 (
    echo systemProp.http.proxyHost=127.0.0.1 > "%LOCAL_PROPERTIES_FILE%"
    echo systemProp.http.proxyPort=10809 >> "%LOCAL_PROPERTIES_FILE%"
    echo systemProp.https.proxyHost=127.0.0.1 >> "%LOCAL_PROPERTIES_FILE%"
    echo systemProp.https.proxyPort=10809 >> "%LOCAL_PROPERTIES_FILE%"
    echo [信息] 已为本地 Gradle 守护进程启用代理配置。
) else (
    if exist "%LOCAL_PROPERTIES_FILE%" del /q "%LOCAL_PROPERTIES_FILE%"
)

:: 设置临时代理环境变量 (如果启用)
if %USE_PROXY% equ 1 (
    set HTTP_PROXY=http://127.0.0.1:10809
    set HTTPS_PROXY=http://127.0.0.1:10809
    call gradlew.bat %GRADLE_TASK% -Dhttp.proxyHost=127.0.0.1 -Dhttp.proxyPort=10809 -Dhttps.proxyHost=127.0.0.1 -Dhttps.proxyPort=10809
) else (
    call gradlew.bat %GRADLE_TASK%
)

set BUILD_RESULT=%errorlevel%

:: 清除代理变量
if %USE_PROXY% equ 1 (
    set HTTP_PROXY=
    set HTTPS_PROXY=
)

if %BUILD_RESULT% neq 0 (
    echo [错误] 编译失败，请检查上方日志。
    pause
    exit /b %BUILD_RESULT%
)

echo [成功] 编译构建成功！

:: 7. 整理并输出 APK 文件
if not exist dist mkdir dist

:: 获取时间戳 (Windows 格式: YYYYMMDD_HHMMSS)
set cur_date=%date:~0,4%%date:~5,2%%date:~8,2%
set cur_time=%time:~0,2%%time:~3,2%%time:~6,2%
:: 处理小时个位数时有空格的情况
set cur_time=%cur_time: =0%
set TIMESTAMP=%cur_date%_%cur_time%

set TARGET_APK=dist\board-clock-%BUILD_TYPE%-%TIMESTAMP%.apk
set LATEST_APK=dist\board-clock-%BUILD_TYPE%-latest.apk

if "%BUILD_TYPE%"=="debug" (
    set SOURCE_APK=app\build\outputs\apk\debug\app-debug.apk
) else (
    set SOURCE_APK=app\build\outputs\apk\release\app-release.apk
)

if exist "%SOURCE_APK%" (
    copy /y "%SOURCE_APK%" "%TARGET_APK%" >nul
    copy /y "%SOURCE_APK%" "%LATEST_APK%" >nul
    
    echo ==================================================
    echo 🎉 APK 已成功导出！
    echo 📂 输出目录: dist\
    echo 📄 最新文件: %LATEST_APK%
    echo 📄 备份归档: %TARGET_APK%
    echo ==================================================
    echo 💡 部署提示:
    echo   通过 ADB 安装到开发板：
    echo   adb install -r %LATEST_APK%
) else (
    echo [错误] 未能在预定路径找到生成的 APK: %SOURCE_APK%
    pause
    exit /b 1
)

endlocal
