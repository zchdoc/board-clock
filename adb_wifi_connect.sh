#!/bin/bash

# 设置颜色输出
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
NC='\033[0m' # 无颜色

LAST_IP_FILE=".last_ip"
DEFAULT_IP=""

echo -e "${BLUE}==================================================${NC}"
echo -e "${BLUE}        ADB over Wi-Fi 一键无线连接脚本           ${NC}"
echo -e "${BLUE}==================================================${NC}"

# 1. 检查并获取 adb 命令路径
ADB_CMD="adb"
if ! command -v adb &> /dev/null; then
    # 尝试寻找本地项目目录下的 adb
    LOCAL_ADB="$(pwd)/tools/adb/mac/adb"
    if [ -f "$LOCAL_ADB" ]; then
        ADB_CMD="$LOCAL_ADB"
        echo -e "${GREEN}[信息] 全局 adb 未配置，将自动使用项目本地 adb：tools/adb/mac/adb${NC}"
    else
        echo -e "${RED}[错误] 未检测到 adb 命令，且未在项目本地 tools/adb/mac/ 下找到 adb。${NC}"
        exit 1
    fi
fi

# 2. 读取上一次保存的 IP
if [ -f "$LAST_IP_FILE" ]; then
    DEFAULT_IP=$(cat "$LAST_IP_FILE")
fi

# 3. 让用户输入或确认 IP
if [ -n "$DEFAULT_IP" ]; then
    read -p "$(echo -e "${YELLOW}请输入开发板 IP 地址 (默认: $DEFAULT_IP): ${NC}")" IP_INPUT
    if [ -z "$IP_INPUT" ]; then
        IP_INPUT="$DEFAULT_IP"
    fi
else
    read -p "$(echo -e "${YELLOW}请输入开发板 IP 地址 (例如: 192.168.31.100): ${NC}")" IP_INPUT
    if [ -z "$IP_INPUT" ]; then
        echo -e "${RED}[错误] IP 地址不能为空！${NC}"
        exit 1
    fi
fi

# 保存当前 IP 供下次使用
echo "$IP_INPUT" > "$LAST_IP_FILE"

# 4. 尝试无线连接
echo -e "${BLUE}[开始] 正在尝试无线连接到 $IP_INPUT:5555 ...${NC}"
"$ADB_CMD" disconnect "$IP_INPUT:5555" > /dev/null 2>&1
CONNECT_OUT=$("$ADB_CMD" connect "$IP_INPUT:5555" 2>&1)

if echo "$CONNECT_OUT" | grep -q "connected"; then
    echo -e "${GREEN}[成功] ${CONNECT_OUT}${NC}"
    echo -e "${GREEN}[成功] 设备已成功通过 Wi-Fi 连接！现在可以进行无线安装或调试了。${NC}"
    echo -e "你可以运行: $ADB_CMD devices 查看已连接设备"
    exit 0
else
    echo -e "${RED}[失败] 无法直接连接。${NC}"
    echo -e "${YELLOW}提示: 如果开发板首次进行无线连接，或刚刚重启过，需要先用 USB 线激活。${NC}"
    echo ""
    read -p "$(echo -e "${BLUE}请问现在是否可以用 USB 线将开发板连接到电脑？(y/n): ${NC}")" ANSWER
    
    if [ "$ANSWER" = "y" ] || [ "$ANSWER" = "Y" ]; then
        echo -e "${BLUE}[第一步] 正在检测 USB 连接 of 设备...${NC}"
        USB_DEVICES=$("$ADB_CMD" devices | grep -v "List" | grep "device$" | wc -l)
        if [ "$USB_DEVICES" -eq 0 ]; then
            echo -e "${RED}[错误] 未检测到通过 USB 连接的 Android 设备，请检查 USB 线是否插好并开启了开发者调试。${NC}"
            exit 1
        fi
        
        echo -e "${GREEN}[成功] 已找到 USB 设备。正在开启开发板的 5555 无线调试端口...${NC}"
        "$ADB_CMD" tcpip 5555
        if [ $? -eq 0 ]; then
            echo -e "${GREEN}[成功] 5555 端口已激活！${NC}"
            echo -e "${YELLOW}现在您可以拔掉 USB 数据线了。${NC}"
            echo -e "${BLUE}[第二步] 正在重新尝试 Wi-Fi 连接...${NC}"
            sleep 2
            
            CONNECT_OUT=$("$ADB_CMD" connect "$IP_INPUT:5555" 2>&1)
            if echo "$CONNECT_OUT" | grep -q "connected"; then
                echo -e "${GREEN}[成功] ${CONNECT_OUT}${NC}"
                echo -e "${GREEN}[成功] 无线连接配置完成！接下来您可以完全脱离 USB 线进行部署了。${NC}"
            else
                echo -e "${RED}[错误] 无线连接再次失败: ${CONNECT_OUT}${NC}"
                echo -e "请确保您的电脑与开发板处于【同一个 Wi-Fi 局域网】下，且 IP 输入正确。"
            fi
        else
            echo -e "${RED}[错误] 激活 5555 端口失败，请检查设备状态。${NC}"
        fi
    else
        echo -e "${YELLOW}[结束] 未进行 USB 激活。请确认开发板 IP 是否正确，或手动开启开发板上的无线调试服务。${NC}"
    fi
fi
