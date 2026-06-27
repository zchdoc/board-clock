#!/bin/bash

# 设置颜色输出
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
NC='\033[0m' # 无颜色

echo -e "${BLUE}==================================================${NC}"
echo -e "${BLUE}        Board Clock APK 一键无线部署安装脚本     ${NC}"
echo -e "${BLUE}==================================================${NC}"

# 1. 检查并获取 adb 命令路径
ADB_CMD="adb"
if ! command -v adb &> /dev/null; then
    LOCAL_ADB="$(pwd)/tools/adb/mac/adb"
    if [ -f "$LOCAL_ADB" ]; then
        ADB_CMD="$LOCAL_ADB"
        echo -e "${GREEN}[信息] 全局 adb 未配置，将自动使用项目本地 adb：tools/adb/mac/adb${NC}"
    else
        echo -e "${RED}[错误] 未检测到 adb 命令，且未在项目本地 tools/adb/mac/ 下找到 adb。${NC}"
        exit 1
    fi
fi

# 2. 检查是否有已连接的设备
echo -e "${BLUE}[开始] 正在检测已连接的设备...${NC}"
DEVICE_COUNT=$("$ADB_CMD" devices | grep -v "List" | grep "device$" | wc -l)

if [ "$DEVICE_COUNT" -eq 0 ]; then
    echo -e "${RED}[错误] 未找到任何已连接的设备！${NC}"
    echo -e "${YELLOW}💡 解决办法：${NC}"
    echo -e "  请先在终端运行连接脚本：${BLUE}./adb_wifi_connect.sh${NC} 连接您的开发板。"
    exit 1
fi

# 打印连接中的设备信息
echo -e "${GREEN}[信息] 检测到已连接设备：${NC}"
"$ADB_CMD" devices | grep -v "List" | grep "device$"

# 3. 检查 APK 文件是否存在
APK_PATH="dist/board-clock-debug-latest.apk"
if [ ! -f "$APK_PATH" ]; then
    echo -e "${RED}[错误] 未在 dist/ 目录下找到最新的 APK 文件！${NC}"
    echo -e "${YELLOW}💡 解决办法：${NC}"
    echo -e "  请先在终端运行打包脚本：${BLUE}./build_apk.sh${NC} 编译生成 APK。"
    exit 1
fi

# 4. 执行安装
echo -e "${BLUE}[开始] 正在推送安装 $APK_PATH 到开发板...${NC}"
INSTALL_OUT=$("$ADB_CMD" install -r "$APK_PATH" 2>&1)

if echo "$INSTALL_OUT" | grep -q "Success"; then
    echo -e "${GREEN}==================================================${NC}"
    echo -e "${GREEN}🎉 安装成功！应用已无线部署到您的开发板上。${NC}"
    echo -e "${GREEN}==================================================${NC}"
else
    echo -e "${RED}[错误] 安装失败！详细错误信息：${NC}"
    echo "$INSTALL_OUT"
    exit 1
fi
