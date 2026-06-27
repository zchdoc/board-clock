@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

echo ==================================================
echo         ADB over Wi-Fi 一键无线连接脚本           
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

:: 2. 读取上一次保存的 IP
set LAST_IP_FILE=.last_ip
set DEFAULT_IP=
if exist "%LAST_IP_FILE%" (
    set /p DEFAULT_IP=<"%LAST_IP_FILE%"
)

:: 3. 让用户输入或确认 IP
if not "!DEFAULT_IP!"=="" (
    set /p IP_INPUT="请输入开发板 IP 地址 (默认: !DEFAULT_IP!): "
    if "!IP_INPUT!"=="" (
        set IP_INPUT=!DEFAULT_IP!
    )
) else (
    set /p IP_INPUT="请输入开发板 IP 地址 (例如: 192.168.31.100): "
    if "!IP_INPUT!"=="" (
        echo [错误] IP 地址不能为空！
        pause
        exit /b 1
    )
)

:: 保存当前 IP 供下次使用
echo !IP_INPUT!>"%LAST_IP_FILE%"

:: 4. 尝试无线连接
echo [开始] 正在尝试无线连接到 !IP_INPUT!:5555 ...
adb disconnect !IP_INPUT!:5555 >nul 2>&1

:: 执行连接并抓取输出
set CONNECT_SUCCESS=0
for /f "delims=" %%a in ('adb connect !IP_INPUT!:5555 2^>^&1') do (
    set CONNECT_OUT=%%a
    echo %%a | findstr /i "connected" >nul
    if !errorlevel! equ 0 (
        set CONNECT_SUCCESS=1
    )
)

if !CONNECT_SUCCESS! equ 1 (
    echo [成功] 设备已成功通过 Wi-Fi 连接！现在可以进行无线安装或调试了。
    echo 你可以运行: adb devices 查看已连接设备
    pause
    exit /b 0
) else (
    echo [失败] 无法直接连接。
    echo 提示: 如果开发板首次进行无线连接，或刚刚重启过，需要先用 USB 线激活。
    echo.
    set /p ANSWER="请问现在是否可以用 USB 线将开发板连接到电脑？(y/n): "
    
    if /i "!ANSWER!"=="y" (
        echo [第一步] 正在检测 USB 连接设备...
        :: 检查设备数量
        set USB_COUNT=0
        for /f "skip=1 tokens=1,2" %%i in ('adb devices') do (
            if "%%j"=="device" (
                set /a USB_COUNT+=1
            )
        )
        
        if !USB_COUNT! equ 0 (
            echo [错误] 未检测到通过 USB 连接的 Android 设备，请检查 USB 线是否插好并开启了开发者调试。
            pause
            exit /b 1
        )
        
        echo [成功] 已找到 USB 设备。正在开启开发板的 5555 无线调试端口...
        adb tcpip 5555
        if !errorlevel! equ 0 (
            echo [成功] 5555 端口已激活！
            echo 现在您可以拔掉 USB 数据线了。
            echo [第二步] 正在重新尝试 Wi-Fi 连接...
            timeout /t 2 >nul
            
            set CONNECT_SUCCESS=0
            for /f "delims=" %%a in ('adb connect !IP_INPUT!:5555 2^>^&1') do (
                echo %%a | findstr /i "connected" >nul
                if !errorlevel! equ 0 (
                    set CONNECT_SUCCESS=1
                )
            )
            
            if !CONNECT_SUCCESS! equ 1 (
                echo [成功] 无线连接配置完成！接下来您可以完全脱离 USB 线进行部署了。
            ) else (
                echo [错误] 无线连接再次失败。
                echo 请确保您的电脑与开发板处于【同一个 Wi-Fi 局域网】下，且 IP 输入正确。
            )
        ) else (
            echo [错误] 激活 5555 端口失败，请检查设备状态。
        )
    ) else (
        echo [结束] 未进行 USB 激活。请确认开发板 IP 是否正确，或手动开启开发板上的无线调试服务。
    )
)

pause
endlocal
