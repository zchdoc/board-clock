#!/bin/bash

# 设置颜色输出
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
NC='\033[0m' # 无颜色

echo -e "${BLUE}==================================================${NC}"
echo -e "${BLUE}           Board Clock APK 打包脚本 (macOS)       ${NC}"
echo -e "${BLUE}==================================================${NC}"

# 1. 检查 Java 环境
if ! command -v java &> /dev/null; then
    echo -e "${RED}[错误] 未检测到 Java 环境，请先安装 JDK 21+。${NC}"
    echo -e "推荐下载 Azul Zulu JDK 21 或 Oracle JDK 21。"
    exit 1
fi

JAVA_VER=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}')
echo -e "${GREEN}[信息] 检测到 Java 版本: ${JAVA_VER}${NC}"

# 2. 检查 Android SDK 环境变量或 local.properties
SDK_DIR=""
if [ -f "local.properties" ]; then
    # 提取 sdk.dir
    SDK_DIR=$(grep -E '^sdk.dir' local.properties | cut -d'=' -f2- | sed 's/\\//g')
fi

if [ -z "$SDK_DIR" ] && [ -z "$ANDROID_HOME" ]; then
    echo -e "${YELLOW}[警告] 未检测到 Android SDK 路径 (sdk.dir 或 ANDROID_HOME)。${NC}"
    echo -e "请在根目录创建 [local.properties](file:///Users/zch/Documents/code/board-clock/local.properties) 并写入: sdk.dir=/Users/您的用户名/Library/Android/sdk"
fi

# 3. 代理检测 (根据用户网络环境规则，检测 127.0.0.1:10809 是否开启)
PROXY_HOST="127.0.0.1"
PROXY_PORT="10809"
USE_PROXY=false

if nc -z "$PROXY_HOST" "$PROXY_PORT" 2>/dev/null; then
    echo -e "${YELLOW}[提示] 检测到本地代理端口 ${PROXY_HOST}:${PROXY_PORT} 已开启。${NC}"
    echo -e "${YELLOW}[提示] 将自动使用代理加速 Gradle 依赖下载。${NC}"
    USE_PROXY=true
else
    echo -e "${GREEN}[信息] 未检测到代理端口 ${PROXY_HOST}:${PROXY_PORT}，将直连网络。${NC}"
fi

# 4. 确定打包模式 (Debug 还是 Release)
BUILD_TYPE="debug"
GRADLE_TASK="assembleDebug"

if [ "$1" = "release" ]; then
    BUILD_TYPE="release"
    GRADLE_TASK="assembleRelease"
fi

echo -e "${GREEN}[信息] 当前打包模式: ${BUILD_TYPE} (任务: ${GRADLE_TASK})${NC}"

# 5. 确保 gradlew 具有执行权限
if [ -f "gradlew" ]; then
    chmod +x gradlew
else
    echo -e "${RED}[错误] 未在根目录找到 gradlew 脚本。${NC}"
    exit 1
fi

# 6. 执行打包
echo -e "${BLUE}[开始] 开始编译构建...${NC}"
echo -e "${GREEN}[信息] 已将 Gradle 缓存目录设置为当前项目下的 .gradle_home/${NC}"
export GRADLE_USER_HOME="$(pwd)/.gradle_home"

# 动态配置本地 Gradle 守护进程代理属性，确保下载依赖时能够通过代理
LOCAL_PROPERTIES_FILE="$(pwd)/.gradle_home/gradle.properties"
mkdir -p "$(pwd)/.gradle_home"
if [ "$USE_PROXY" = true ]; then
    echo "systemProp.http.proxyHost=${PROXY_HOST}" > "$LOCAL_PROPERTIES_FILE"
    echo "systemProp.http.proxyPort=${PROXY_PORT}" >> "$LOCAL_PROPERTIES_FILE"
    echo "systemProp.https.proxyHost=${PROXY_HOST}" >> "$LOCAL_PROPERTIES_FILE"
    echo "systemProp.https.proxyPort=${PROXY_PORT}" >> "$LOCAL_PROPERTIES_FILE"
    echo -e "${GREEN}[信息] 已为本地 Gradle 守护进程启用代理配置。${NC}"
else
    rm -f "$LOCAL_PROPERTIES_FILE"
fi

START_TIME=$(date +%s)

if [ "$USE_PROXY" = true ]; then
    HTTP_PROXY="http://${PROXY_HOST}:${PROXY_PORT}" HTTPS_PROXY="http://${PROXY_HOST}:${PROXY_PORT}" ./gradlew ${GRADLE_TASK} -Dhttp.proxyHost=${PROXY_HOST} -Dhttp.proxyPort=${PROXY_PORT} -Dhttps.proxyHost=${PROXY_HOST} -Dhttps.proxyPort=${PROXY_PORT}
else
    ./gradlew ${GRADLE_TASK}
fi

BUILD_RESULT=$?
END_TIME=$(date +%s)
DURATION=$((END_TIME - START_TIME))

if [ $BUILD_RESULT -ne 0 ]; then
    echo -e "${RED}[错误] 编译失败，请检查上方日志。${NC}"
    exit $BUILD_RESULT
fi

echo -e "${GREEN}[成功] 编译构建成功！耗时 ${DURATION} 秒。${NC}"

# 7. 整理并输出 APK 文件
DIST_DIR="dist"
mkdir -p "$DIST_DIR"

TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
SOURCE_APK=""
TARGET_APK=""

if [ "$BUILD_TYPE" = "debug" ]; then
    SOURCE_APK="app/build/outputs/apk/debug/app-debug.apk"
    TARGET_APK="${DIST_DIR}/board-clock-debug-${TIMESTAMP}.apk"
else
    # 寻找生成的 release apk
    SOURCE_APK=$(find app/build/outputs/apk/release -name "*.apk" | head -n 1)
    if [ -z "$SOURCE_APK" ]; then
        SOURCE_APK="app/build/outputs/apk/release/app-release.apk"
    fi
    TARGET_APK="${DIST_DIR}/board-clock-release-${TIMESTAMP}.apk"
fi

if [ -f "$SOURCE_APK" ]; then
    cp "$SOURCE_APK" "$TARGET_APK"
    # 创建最新包的软连接或副本
    cp "$SOURCE_APK" "${DIST_DIR}/board-clock-${BUILD_TYPE}-latest.apk"
    
    echo -e "${GREEN}==================================================${NC}"
    echo -e "${GREEN}🎉 APK 已成功导出！${NC}"
    echo -e "${GREEN}📂 输出目录: [dist/](file:///Users/zch/Documents/code/board-clock/dist) ${NC}"
    echo -e "${GREEN}📄 最新文件: [board-clock-${BUILD_TYPE}-latest.apk](file://$(pwd)/${DIST_DIR}/board-clock-${BUILD_TYPE}-latest.apk) ${NC}"
    echo -e "${GREEN}📄 备份归档: [$(basename ${TARGET_APK})](file://$(pwd)/${TARGET_APK}) ${NC}"
    echo -e "${GREEN}==================================================${NC}"
    
    # 提示安装指令
    echo -e "${BLUE}💡 部署提示:${NC}"
    echo -e "  通过 ADB 安装到开发板："
    echo -e "  adb install -r ${DIST_DIR}/board-clock-${BUILD_TYPE}-latest.apk"
else
    echo -e "${RED}[错误] 未能在预定路径找到生成的 APK: ${SOURCE_APK}${NC}"
    exit 1
fi
