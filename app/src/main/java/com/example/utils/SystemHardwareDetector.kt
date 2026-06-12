package com.example.utils

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Build
import android.util.DisplayMetrics

data class SpecItem(val key: String, val value: String)
data class SensorSpec(val name: String, val vendor: String, val typeStr: String, val power: Float, val resolution: Float)

object SystemHardwareDetector {

    fun getSystemSpecs(context: Context): List<SpecItem> {
        val pm = context.packageManager
        val features = listOf(
            PackageManager.FEATURE_WIFI to "Wi-Fi 局域无线网筹",
            PackageManager.FEATURE_BLUETOOTH to "Bluetooth 蓝牙经典协议",
            PackageManager.FEATURE_BLUETOOTH_LE to "Bluetooth LE 低功耗蓝牙",
            PackageManager.FEATURE_USB_HOST to "USB Host 模式主控适配",
            PackageManager.FEATURE_USB_ACCESSORY to "USB Accessory 串行外设",
            PackageManager.FEATURE_CAMERA to "Camera 物理摄像模组",
            PackageManager.FEATURE_MICROPHONE to "Microphone 音频录音话筒",
            PackageManager.FEATURE_NFC to "NFC 近场接触通信集成",
            PackageManager.FEATURE_LOCATION to "Location 地理卫星定位器"
        )
        
        val supportedFeatures = features.map { (feature, label) ->
            val isSupported = pm.hasSystemFeature(feature)
            SpecItem(label, if (isSupported) "✅ 支持 / 检测到" else "❌ 不支持 / 未检测到")
        }

        return listOf(
            SpecItem("Android 操作系统版本", Build.VERSION.RELEASE),
            SpecItem("SDK API 水平", Build.VERSION.SDK_INT.toString()),
            SpecItem("安全修补程序补丁级别", Build.VERSION.SECURITY_PATCH ?: "未知"),
            SpecItem("支持的 CPU 架构 ABIs", Build.SUPPORTED_ABIS.joinToString(", ")),
            SpecItem("硬件底座/核心板配置", Build.BOARD),
            SpecItem("芯片/系统品牌 (Brand)", Build.BRAND),
            SpecItem("设备代号 (Device)", Build.DEVICE),
            SpecItem("处理器指令架构 (Hardware)", Build.HARDWARE),
            SpecItem("主板设计制造商 (Manufacturer)", Build.MANUFACTURER),
            SpecItem("终端产品型号 (Model)", Build.MODEL),
            SpecItem("系统编译版本标识 (Display)", Build.DISPLAY),
            SpecItem("Linux 内核主线版本", System.getProperty("os.version") ?: "未知")
        ) + supportedFeatures
    }

    fun getHardwareSpecs(context: Context): List<SpecItem> {
        val dm: DisplayMetrics = context.resources.displayMetrics
        val width = dm.widthPixels
        val height = dm.heightPixels
        val density = dm.density
        val densityDpi = dm.densityDpi
        val xdpi = dm.xdpi
        val ydpi = dm.ydpi
        
        // Compute estimated inches
        val widthInches = width / if (xdpi > 0) xdpi else 160f
        val heightInches = height / if (ydpi > 0) ydpi else 160f
        val screenInchesEstimate = Math.sqrt((widthInches * widthInches + heightInches * heightInches).toDouble())
        val formattedInches = String.format("%.2f", screenInchesEstimate)

        // Sensors availability
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val allSensors = sensorManager.getSensorList(Sensor.TYPE_ALL)

        val tempSensor = sensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE) != null
        val humiditySensor = sensorManager.getDefaultSensor(Sensor.TYPE_RELATIVE_HUMIDITY) != null
        val pressureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE) != null
        val lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT) != null
        val gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null
        val accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null

        return listOf(
            SpecItem("物理屏幕最高分辨率", "${width} × ${height} 像素"),
            SpecItem("设备逻辑显示缩放比 (density)", "$density"),
            SpecItem("设备绝对像素密度 (DPI)", "$densityDpi dpi"),
            SpecItem("水平 X 轴 DPI (xdpi)", String.format("%.1f", xdpi)),
            SpecItem("垂直 Y 轴 DPI (ydpi)", String.format("%.1f", ydpi)),
            SpecItem("估算屏幕物理斜线尺寸", "$formattedInches 英寸"),
            SpecItem("物理传感器硬件注册数", "${allSensors.size} 个有效节点"),
            SpecItem("温度传感器 (Ambient Temp)", if (tempSensor) "✅ 硬件就绪 (支持底层开发)" else "❌ 未注册 (缺少该元件)"),
            SpecItem("相对湿度传感器 (Humidity)", if (humiditySensor) "✅ 硬件就绪 (支持底层开发)" else "❌ 未注册 (缺少该元件)"),
            SpecItem("气压压力传感器 (Pressure)", if (pressureSensor) "✅ 硬件就绪 (支持底层开发)" else "❌ 未注册 (缺少该元件)"),
            SpecItem("环境可见光感应器 (Light)", if (lightSensor) "✅ 硬件就绪 (支持底层开发)" else "❌ 未注册 (缺少该元件)"),
            SpecItem("物理姿态陀螺仪 (Gyroscope)", if (gyroSensor) "✅ 硬件就绪 (支持底层开发)" else "❌ 未注册 (缺少该元件)"),
            SpecItem("多轴重力加速度计 (Accelerometer)", if (accelSensor) "✅ 硬件就绪 (支持底层开发)" else "❌ 未注册 (缺少该元件)")
        )
    }

    fun getAllSensorsDetail(context: Context): List<SensorSpec> {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val allSensors = sensorManager.getSensorList(Sensor.TYPE_ALL)
        return allSensors.map { sensor ->
            val typeStr = getSensorTypeString(sensor.type)
            SensorSpec(
                name = sensor.name,
                vendor = sensor.vendor,
                typeStr = typeStr,
                power = sensor.power,
                resolution = sensor.resolution
            )
        }
    }

    private fun getSensorTypeString(type: Int): String {
        return when (type) {
            Sensor.TYPE_ACCELEROMETER -> "物理三轴加速度仪"
            Sensor.TYPE_MAGNETIC_FIELD -> "地磁罗盘感应计"
            Sensor.TYPE_ORIENTATION -> "经典平面朝向陀螺[旧称]"
            Sensor.TYPE_GYROSCOPE -> "高频姿态航向陀螺仪"
            Sensor.TYPE_LIGHT -> "环境照度敏感器"
            Sensor.TYPE_PRESSURE -> "气密大气压计"
            Sensor.TYPE_PROXIMITY -> "近距离光磁阻断器"
            Sensor.TYPE_GRAVITY -> "合成重力感知流"
            Sensor.TYPE_LINEAR_ACCELERATION -> "去除重力加速度轴"
            Sensor.TYPE_ROTATION_VECTOR -> "高复合航位推算矩阵"
            Sensor.TYPE_RELATIVE_HUMIDITY -> "空气毛发与湿敏板"
            Sensor.TYPE_AMBIENT_TEMPERATURE -> "环境外周温度传感器"
            else -> "非标准专有型传感器 (硬件代码: $type)"
        }
    }

    fun generateSystemMarkdownReport(context: Context): String {
        val sb = StringBuilder()
        sb.append("### ─── Android 系统检测报告 ───\n\n")
        getSystemSpecs(context).forEach { item ->
            sb.append("- **${item.key}**: ${item.value}\n")
        }
        sb.append("\n*报告生成时间: [2026] 针对底座开发板专属输出*")
        return sb.toString()
    }

    fun generateHardwareAndSensorsMarkdownReport(context: Context): String {
        val sb = StringBuilder()
        sb.append("### ─── 物理硬件与传感器自测报告 ───\n\n")
        getHardwareSpecs(context).forEach { item ->
            sb.append("- **${item.key}**: ${item.value}\n")
        }
        sb.append("\n#### 🔍 已装载传感器清单列表 (${getAllSensorsDetail(context).size} 个节点):\n\n")
        getAllSensorsDetail(context).forEachIndexed { index, sensor ->
            sb.append("${index + 1}. **${sensor.name}**\n")
            sb.append("   - 硬件供应商: `${sensor.vendor}`\n")
            sb.append("   - 开发内核驱动分类: ${sensor.typeStr}\n")
            sb.append("   - 工作额定功耗: `${sensor.power} mA`\n")
            sb.append("   - 硬件最大分辨率: `${sensor.resolution}`\n")
        }
        return sb.toString()
    }
}
