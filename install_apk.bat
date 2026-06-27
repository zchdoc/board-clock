@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

echo ==================================================
echo        Board Clock APK 一键无线部署安装脚本     
echo ==================================================

:: 1. 检查并配置 adb 路径
where adb >nul 2>nul
if %errorlevel% neq 0 (
    set LOCAL_ADB_DIR=%~dp0tools\adb\win
    if exist "!LOCAL_ADB_DIR!\adb.exe" (
        set PATH=!LOCAL_ADB_DIR!;%PATH%
        echo [信息] 全局 adb 未配置，已将项目本地 tools\adb\win 临时加入当前运行环境。
    ) else (
        echo [错误] 未检测到 adb 命令，且未在项目本地 tools\adb\win\ 下找到 adb.exe。
        pause
        exit /b 1
    )
)

:: 2. 检查是否有已连接的设备
echo [开始] 正在检测已连接的设备...
set DEVICE_COUNT=0
for /f "skip=1 tokens=1,2" %%i in ('adb devices') do (
    if "%%j"=="device" (
        set /a DEVICE_COUNT+=1
    )
)

if %DEVICE_COUNT% equ 0 (
    echo [错误] 未找到任何已连接的设备！
    echo.
    echo 💡 解决办法：
    echo   请先运行连接脚本：adb_wifi_connect.bat 连接您的开发板。
    echo.
    pause
    exit /b 1
)

echo [信息] 已检测到连接设备。

:: 3. 检查 APK 文件是否存在
set APK_PATH=dist\board-clock-debug-latest.apk
if not exist "%APK_PATH%" (
    echo [错误] 未在 dist\ 目录下找到最新的 APK 文件！
    echo.
    echo 💡 解决办法：
    echo   请先运行打包脚本：build_apk.bat 编译生成 APK。
    echo.
    pause
    exit /b 1
)

:: 4. 执行安装
echo [开始] 正在推送安装 %APK_PATH% 到开发板...
set INSTALL_SUCCESS=0
for /f "delims=" %%a in ('adb install -r "%APK_PATH%" 2^>^&1') do (
    echo %%a
    echo %%a | findstr /i "Success" >nul
    if !errorlevel! equ 0 (
        set INSTALL_SUCCESS=1
    )
)

if %INSTALL_SUCCESS% equ 1 (
    echo ==================================================
    echo 🎉 安装成功！应用已无线部署到您的开发板上。
    echo ==================================================
) else (
    echo [错误] 安装失败！请检查上方输出错误信息。
)

pause
endlocal
