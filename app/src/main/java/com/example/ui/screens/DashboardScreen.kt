package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.media.RingtoneManager
import android.net.Uri
import android.widget.Toast
import android.widget.VideoView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.utils.SystemHardwareDetector
import com.example.utils.VoiceState
import com.example.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

// Dynamic Theme Colors definitions to support light & dark modes with Material 3 contrast
data class ThemeColors(
    val background: Color,
    val surface: Color,
    val cardBackground: Color,
    val border: Color,
    val accent: Color,
    val accentSecondary: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val success: Color,
    val slotEmpty: Color,
    val isDark: Boolean
)

val DarkThemeColors = ThemeColors(
    background = Color(0xFF000000),      // Pitch black background for maximum clock emphasis
    surface = Color(0xFF121212),         // Deep gray panel surface
    cardBackground = Color(0xFF1E1E1E),  // Clean card background
    border = Color(0xFF2C2C2C),          // Dark charcoal sleek border
    accent = Color(0xFFFFFFFF),          // Premium white for maximized clock numbers
    accentSecondary = Color(0xFFCCCCCC), // Light grey for secondary indicators
    textPrimary = Color.White,           // Crisp white for primary text
    textSecondary = Color.White.copy(alpha = 0.6f),
    success = Color(0xFFFFFFFF),         // Clean neutral active state
    slotEmpty = Color(0xFF0A0A0A),       // Invisible or ultra-subtle empty slots
    isDark = true
)

val LightThemeColors = ThemeColors(
    background = Color(0xFFFFFFFF),      // Clean white background
    surface = Color(0xFFF5F5F5),         // Off-white surface panels
    cardBackground = Color(0xFFEAEAEA),  // Warm light gray cards
    border = Color(0xFFD0D0D0),          // Soft gray border line
    accent = Color(0xFF000000),          // Bold black text for clock display
    accentSecondary = Color(0xFF555555), // Mid-to-dark gray for secondary labels
    textPrimary = Color(0xFF000000),     // Solid black primary text
    textSecondary = Color(0xFF666666),   // Soft charcoal secondary text
    success = Color(0xFF000000),         // Clean active slate state
    slotEmpty = Color(0xFFFAFAFA),       // Very light background for slots
    isDark = false
)

// Simplified Dynamic Translation engine
fun getLocalizedText(isEnglish: Boolean, key: String): String {
    val dict = mapOf(
        "title" to if (isEnglish) "Board Clock & Hardware Debug Workshop" else "调试工作台",
        "title_desc_sub" to if (isEnglish) "BOARD CLOCK INTEGRATED SYSTEM" else "显示及语音交互底座",
        "clock_screen" to if (isEnglish) "Clock Screen" else "时钟大屏",
        "not_bound" to if (isEnglish) "No app mapping shortcut linked" else "未关联本地指令快捷应用",
        "copy_report" to if (isEnglish) "Copy Report" else "一键复制",
        "copied" to if (isEnglish) "Report copied to clipboard! 📋" else "🗒️ 自测数据已成功复制系统剪纸板",
        "app_mappings" to if (isEnglish) "Command Links [0-9]" else "指令链接绑定 [0-9]",
        "hw_detect" to if (isEnglish) "Hardware Sensors" else "感知传感器检测",
        "sys_detect" to if (isEnglish) "Android System API" else "系统SDK规格检测",
        "timezone" to if (isEnglish) "Timezone: ${java.util.TimeZone.getDefault().id}" else "设备时区: ${java.util.TimeZone.getDefault().id}",
        "voice_status" to if (isEnglish) "Voice Recognizer Engine" else "声学拾音识别引擎",
        "start_voice" to if (isEnglish) "Listen" else "启动监听",
        "stop_voice" to if (isEnglish) "Mute" else "阻断静口",
        "device_report" to if (isEnglish) "Physical Sensors Detail Block" else "物理传感器、屏幕总览硬件底座细则",
        "system_report" to if (isEnglish) "OS Framework Specs Block" else "系统基调、基础可用SDK特征细则"
    )
    return dict[key] ?: key
}

@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    onToggleVoice: (Boolean) -> Unit,
    onToggleOrientation: () -> Unit
) {
    val context = LocalContext.current
    
    // Dynamic system triggers & preferences
    val isFullscreen by viewModel.isFullscreen.collectAsStateWithLifecycle()
    val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
    val isEnglish by viewModel.isEnglish.collectAsStateWithLifecycle()
    val timeOffsetMs by viewModel.timeOffsetMs.collectAsStateWithLifecycle()
    val useSystemTime by viewModel.useSystemTime.collectAsStateWithLifecycle()
    val showSeconds by viewModel.showSeconds.collectAsStateWithLifecycle()
    val showVideoBackground by viewModel.showVideoBackground.collectAsStateWithLifecycle()
    val videoUriPortraitStr by viewModel.videoUriPortrait.collectAsStateWithLifecycle()
    val videoUriLandscapeStr by viewModel.videoUriLandscape.collectAsStateWithLifecycle()

    // Screen height and width parameters to adjust orientation beautifully
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isPortrait = configuration.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT

    // Determine active video based on orientation: portrait video for portrait mode, landscape video for landscape mode
    val activeVideoUri = if (isPortrait) {
        videoUriPortraitStr
    } else {
        videoUriLandscapeStr
    }
    val clockColorInt by viewModel.clockColor.collectAsStateWithLifecycle()
    val dateColorInt by viewModel.dateColor.collectAsStateWithLifecycle()
    val voiceControlEnabled by viewModel.voiceControlEnabled.collectAsStateWithLifecycle()

    val colors = if (isDarkMode) DarkThemeColors else LightThemeColors
    val clockColor = if (clockColorInt != 0) Color(clockColorInt) else colors.accent
    val dateColor = if (dateColorInt != 0) Color(dateColorInt) else colors.textPrimary

    var targetPickerIsPortrait by remember { mutableStateOf(true) }

    // Local video picker launcher
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, takeFlags)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            if (targetPickerIsPortrait) {
                viewModel.setVideoUriPortrait(uri.toString())
            } else {
                viewModel.setVideoUriLandscape(uri.toString())
            }
        }
    }

    // Local states
    var selectedSlotForMapping by remember { mutableStateOf(0) }
    var showAppSelector by remember { mutableStateOf(false) }

    // Collect voice status
    val voiceState by viewModel.voiceState.collectAsStateWithLifecycle()
    val actionLog by viewModel.lastActionLog.collectAsStateWithLifecycle()
    val rmsLevel by viewModel.rmsLevel.collectAsStateWithLifecycle()
    val appMappings by viewModel.appMappings.collectAsStateWithLifecycle()
    val pendingAppLaunch by viewModel.pendingAppLaunch.collectAsStateWithLifecycle()
    val clockStyle by viewModel.clockStyle.collectAsStateWithLifecycle()
    val clockFont by viewModel.clockFont.collectAsStateWithLifecycle()

    // Dynamic Time tick
    var currentTimeMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(useSystemTime, timeOffsetMs) {
        while (true) {
            currentTimeMillis = if (useSystemTime) {
                System.currentTimeMillis()
            } else {
                System.currentTimeMillis() + timeOffsetMs
            }
            kotlinx.coroutines.delay(200)
        }
    }

    // Formatter locales
    val locale = if (isEnglish) Locale.ENGLISH else Locale.SIMPLIFIED_CHINESE
    val timeStr = remember(currentTimeMillis, locale, showSeconds) {
        val pattern = if (showSeconds) "HH:mm:ss" else "HH:mm"
        SimpleDateFormat(pattern, locale).format(Date(currentTimeMillis))
    }
    val dateStr = remember(currentTimeMillis, locale) {
        val pattern = if (isEnglish) "MMMM dd, yyyy" else "yyyy年MM月dd日"
        SimpleDateFormat(pattern, locale).format(Date(currentTimeMillis))
    }
    val weekStr = remember(currentTimeMillis, locale) {
        val pattern = if (isEnglish) "EEEE" else "星期EEEE"
        val raw = SimpleDateFormat(pattern, locale).format(Date(currentTimeMillis))
        if (!isEnglish) raw.replace("星期星期", "星期") else raw
    }

    // Timer / Countdown States
    var countdownSeconds by remember { mutableStateOf(0) }
    var activeRingtone by remember { mutableStateOf<android.media.Ringtone?>(null) }

    LaunchedEffect(countdownSeconds) {
        if (countdownSeconds > 0) {
            kotlinx.coroutines.delay(1000L)
            countdownSeconds -= 1
            if (countdownSeconds == 0) {
                // Play alarm sound
                try {
                    val alertUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    val r = RingtoneManager.getRingtone(context, alertUri)
                    r?.play()
                    activeRingtone = r
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                activeRingtone?.stop()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    if (isFullscreen) {
        var showColorTunePanel by remember { mutableStateOf(false) }
        var showPasswordDialog by remember { mutableStateOf(false) }
        var passwordInput by remember { mutableStateOf("") }
        var passwordError by remember { mutableStateOf(false) }

        // Fullscreen Clock display - Maximized hours, minutes, and seconds, with everything else below
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.background)
                .testTag("fullscreen_clock_view"),
            contentAlignment = Alignment.Center
        ) {
            // Render video dynamic background if configured & enabled
            if (showVideoBackground && !activeVideoUri.isNullOrEmpty()) {
                VideoBackgroundPlayer(
                    uriString = activeVideoUri!!,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Clickable background layer doing nothing to avoid accidental exit/trigger settings.
            Box(
                modifier = Modifier.fillMaxSize()
            )

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                val maxW = maxWidth.value
                val maxH = maxHeight.value

                val hourText = remember(currentTimeMillis, locale) {
                    SimpleDateFormat("HH", locale).format(Date(currentTimeMillis))
                }
                val minText = remember(currentTimeMillis, locale) {
                    SimpleDateFormat("mm", locale).format(Date(currentTimeMillis))
                }
                val secText = remember(currentTimeMillis, locale) {
                    SimpleDateFormat("ss", locale).format(Date(currentTimeMillis))
                }

                val dynamicFontSize = remember(maxW, maxH, clockStyle, showSeconds) {
                    if (clockStyle == "vertical") {
                        // Stacked layout has 2 digits per row, so width limit is maxW / 1.45f
                        val scaleByW = maxW / 1.45f
                        val scaleByH = maxH / 2.7f
                        minOf(scaleByW, scaleByH)
                    } else if (clockStyle == "stretched") {
                        // Stretched horizontal needs a slightly smaller font size, because graphicsLayer scales it up vertically.
                        // We also slightly compress horizontally to let it fit nicely.
                        if (showSeconds) {
                            val scaleByW = maxW / 5.4f
                            val scaleByH = maxH / 3.4f
                            minOf(scaleByW, scaleByH)
                        } else {
                            val scaleByW = maxW / 3.4f
                            val scaleByH = maxH / 3.0f
                            minOf(scaleByW, scaleByH)
                        }
                    } else {
                        // Classic inline horizontal row layout
                        if (showSeconds) {
                            val scaleByW = maxW / 5.2f
                            val scaleByH = maxH / 2.8f
                            minOf(scaleByW, scaleByH)
                        } else {
                            val scaleByW = maxW / 3.4f
                            val scaleByH = maxH / 2.6f
                            minOf(scaleByW, scaleByH)
                        }
                    }
                }

                val clockFontFamily = remember(clockFont) {
                    when (clockFont) {
                        "serif" -> FontFamily.Serif
                        "monospace" -> FontFamily.Monospace
                        else -> FontFamily.SansSerif
                    }
                }

                val clockDateSubtitle = remember(currentTimeMillis, locale, isEnglish, clockStyle) {
                    if (clockStyle == "vertical") {
                        if (isEnglish) {
                            val dayWeek = SimpleDateFormat("EEE", locale).format(Date(currentTimeMillis))
                            val monthDay = SimpleDateFormat("MMM d", locale).format(Date(currentTimeMillis))
                            "$dayWeek, $monthDay"
                        } else {
                            val md = SimpleDateFormat("M/d", locale).format(Date(currentTimeMillis))
                            val wk = SimpleDateFormat("EEE", locale).format(Date(currentTimeMillis))
                            "$md $wk"  // e.g., "6/17 周三"
                        }
                    } else {
                        "$dateStr   $weekStr"
                    }
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (countdownSeconds > 0) {
                        // Display countdown text beautifully above the clock
                        val m = countdownSeconds / 60
                        val s = countdownSeconds % 60
                        val countdownStr = String.format("%02d:%02d", m, s)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .background(Color.Red.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isEnglish) "Timer: $countdownStr" else "倒计时: $countdownStr",
                                color = Color.White,
                                fontSize = maxOf(14f, dynamicFontSize * 0.20f).sp,
                                fontWeight = FontWeight.Bold,
                                style = androidx.compose.ui.text.TextStyle(
                                    shadow = androidx.compose.ui.graphics.Shadow(
                                        color = Color.Black.copy(alpha = 0.5f),
                                        offset = androidx.compose.ui.geometry.Offset(2f, 2f),
                                        blurRadius = 4f
                                    )
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height((maxH * 0.04f).dp))
                    }

                    if (clockStyle == "vertical") {
                        // 1. Stacked layout - Hour on top, Minute below, elegant letter spacing
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy((-16).dp) // Tightly stacked!
                        ) {
                            Text(
                                text = hourText,
                                color = clockColor,
                                fontSize = dynamicFontSize.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = clockFontFamily,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                softWrap = false,
                                style = androidx.compose.ui.text.TextStyle(
                                    letterSpacing = (-4).sp,
                                    shadow = androidx.compose.ui.graphics.Shadow(
                                        color = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.5f),
                                        offset = androidx.compose.ui.geometry.Offset(2f, 2f),
                                        blurRadius = 6f
                                    )
                                )
                            )

                            Row(
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = minText,
                                    color = clockColor,
                                    fontSize = dynamicFontSize.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = clockFontFamily,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    softWrap = false,
                                    style = androidx.compose.ui.text.TextStyle(
                                        letterSpacing = (-4).sp,
                                        shadow = androidx.compose.ui.graphics.Shadow(
                                            color = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.5f),
                                            offset = androidx.compose.ui.geometry.Offset(2f, 2f),
                                            blurRadius = 6f
                                        )
                                    )
                                )

                                if (showSeconds) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = ":$secText",
                                        color = clockColor.copy(alpha = 0.70f),
                                        fontSize = (dynamicFontSize * 0.28f).sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = clockFontFamily,
                                        modifier = Modifier.padding(bottom = (dynamicFontSize * 0.08f).dp),
                                        style = androidx.compose.ui.text.TextStyle(
                                            shadow = androidx.compose.ui.graphics.Shadow(
                                                color = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.5f),
                                                offset = androidx.compose.ui.geometry.Offset(1f, 1f),
                                                blurRadius = 4f
                                            )
                                        )
                                    )
                                }
                            }
                        }
                    } else if (clockStyle == "stretched") {
                        // 1. Stretched horizontal layout (scaled vertically, narrower horizontal profile like iOS)
                        Text(
                            text = timeStr,
                            color = clockColor,
                            fontSize = (dynamicFontSize * 1.15f).sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = clockFontFamily,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier
                                .graphicsLayer(
                                    scaleY = 1.65f, // Long elegant vertical stretch
                                    scaleX = 0.82f  // Narrower horizontally
                                )
                                .padding(vertical = (maxH * 0.04f).dp), // Padding creates margin bounds due to vertical scale
                            style = androidx.compose.ui.text.TextStyle(
                                letterSpacing = (-2).sp,
                                shadow = androidx.compose.ui.graphics.Shadow(
                                    color = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.5f),
                                    offset = androidx.compose.ui.geometry.Offset(2f, 2f),
                                    blurRadius = 6f
                                )
                            )
                        )
                    } else {
                        // 1. Classic horizontal line layout (Expanded & highly polished)
                        Text(
                            text = timeStr,
                            color = clockColor,
                            fontSize = dynamicFontSize.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = clockFontFamily,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            softWrap = false,
                            style = androidx.compose.ui.text.TextStyle(
                                letterSpacing = (-2).sp,
                                shadow = androidx.compose.ui.graphics.Shadow(
                                    color = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.5f),
                                    offset = androidx.compose.ui.geometry.Offset(2f, 2f),
                                    blurRadius = 6f
                                )
                            )
                        )
                    }

                    val spacerHeightMultiplier = if (clockStyle == "stretched") 0.06f else 0.03f
                    Spacer(modifier = Modifier.height((maxH * spacerHeightMultiplier).dp))

                    // 2. Date subtitle
                    Text(
                        text = clockDateSubtitle,
                        color = dateColor,
                        fontSize = if (clockStyle == "vertical") {
                            maxOf(16f, dynamicFontSize * 0.16f).sp
                        } else {
                            maxOf(14f, dynamicFontSize * 0.25f).sp
                        },
                        fontWeight = FontWeight.Bold,
                        fontFamily = clockFontFamily,
                        textAlign = TextAlign.Center,
                        style = androidx.compose.ui.text.TextStyle(
                            shadow = androidx.compose.ui.graphics.Shadow(
                                color = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.6f),
                                offset = androidx.compose.ui.geometry.Offset(1f, 1f),
                                blurRadius = 4f
                            )
                        )
                    )
                }
            }

            // Bottom control actions panel (Clearly visible stylized buttons, 50.dp height for large touch target)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = if (showColorTunePanel) 140.dp else 24.dp)
                    .padding(horizontal = 16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. Timer / Countdown Button (Always first)
                    Button(
                        onClick = {
                            countdownSeconds = if (countdownSeconds <= 0) 300 else countdownSeconds + 300
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White.copy(alpha = 0.12f),
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .height(50.dp)
                            .testTag("big_btn_timer")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Timer",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (countdownSeconds > 0) {
                                val m = countdownSeconds / 60
                                val s = countdownSeconds % 60
                                String.format("%02d:%02d", m, s)
                            } else {
                                if (isEnglish) "5 Min Timer" else "5分钟倒计时"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    if (countdownSeconds > 0) {
                        Button(
                            onClick = { countdownSeconds = 0 },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White.copy(alpha = 0.12f),
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .height(50.dp)
                                .testTag("big_btn_cancel_timer")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cancel Timer",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isEnglish) "Cancel" else "取消倒计时",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }

                    // 2. Open Bound App 1 (Placed second)
                    val slot1Mapping = appMappings.find { it.slot == 1 }
                    val slot1Label = slot1Mapping?.appName ?: (if (isEnglish) "Bound App 1" else "绑定应用1")
                    Button(
                        onClick = { viewModel.triggerAppLaunchBySlot(1) },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White.copy(alpha = 0.12f),
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .height(50.dp)
                            .testTag("big_btn_launch_app")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Launch App 1",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isEnglish) "Open: $slot1Label" else "打开: $slot1Label",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    // 3. Toggle Video Background (Unified to default color)
                    Button(
                        onClick = { viewModel.setShowVideoBackground(!showVideoBackground) },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White.copy(alpha = 0.12f),
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .height(50.dp)
                            .testTag("big_btn_toggle_bg")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Video BG",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (showVideoBackground) {
                                if (isEnglish) "Hide Video BG" else "隐藏视频背景"
                            } else {
                                if (isEnglish) "Show Video BG" else "显示视频背景"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    // 4. Tune Colors Page (Unified active color to subtle transparent white pop)
                    Button(
                        onClick = { showColorTunePanel = !showColorTunePanel },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (showColorTunePanel) Color.White.copy(alpha = 0.28f) else Color.White.copy(alpha = 0.12f),
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .height(50.dp)
                            .testTag("big_btn_tune_colors")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Tune Colors",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isEnglish) "Tune Colors" else "时钟字体调色",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    // 5. Rotate Screen Orientation
                    Button(
                        onClick = onToggleOrientation,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White.copy(alpha = 0.12f),
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .height(50.dp)
                            .testTag("big_btn_rotate_screen")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Rotate Screen",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isEnglish) "Rotate Screen" else "切换横竖屏",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    // 6. Settings Security Unlock Button (Unified to default color)
                    Button(
                        onClick = {
                            passwordInput = ""
                            passwordError = false
                            showPasswordDialog = true
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White.copy(alpha = 0.12f),
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .height(50.dp)
                            .testTag("big_btn_settings")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings Access",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isEnglish) "Console Setup" else "系统设置",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // Fixed password unlock dialog
            if (showPasswordDialog) {
                val passwordFocusRequester = remember { FocusRequester() }
                var passwordVisible by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) {
                    kotlinx.coroutines.delay(150L) // Wait for the dialog to settle
                    try {
                        passwordFocusRequester.requestFocus()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                AlertDialog(
                    onDismissRequest = { showPasswordDialog = false },
                    title = {
                        Text(
                            text = if (isEnglish) "Security Access" else "安全认证",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = if (isEnglish) "To exit to dashboard, please enter password:" else "返回控制台需输入安全密码：",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 14.sp
                            )
                            OutlinedTextField(
                                value = passwordInput,
                                onValueChange = { 
                                    passwordInput = it
                                    passwordError = false
                                },
                                placeholder = { Text(if (isEnglish) "Enter password..." else "请输入安全密码...") },
                                singleLine = true,
                                isError = passwordError,
                                visualTransformation = if (passwordVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                                ),
                                trailingIcon = {
                                    val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                                    val description = if (passwordVisible) {
                                        if (isEnglish) "Hide password" else "隐藏密码"
                                    } else {
                                        if (isEnglish) "Show password" else "显示密码"
                                    }
                                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                        Icon(
                                            imageVector = image,
                                            contentDescription = description,
                                            tint = Color.White.copy(alpha = 0.7f),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedPlaceholderColor = Color.White.copy(alpha = 0.5f),
                                    unfocusedPlaceholderColor = Color.White.copy(alpha = 0.4f),
                                    focusedBorderColor = colors.accent,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                                    errorBorderColor = Color.Red
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(passwordFocusRequester)
                            )
                            if (passwordError) {
                                Text(
                                    text = if (isEnglish) "Incorrect password!" else "安全密码不正确，请重新输入！",
                                    color = Color.Red,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (passwordInput == "12131404") {
                                    showPasswordDialog = false
                                    viewModel.setFullscreen(false)
                                } else {
                                    passwordError = true
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = colors.accent)
                        ) {
                            Text(if (isEnglish) "Confirm" else "确认", color = if (colors.isDark) Color.Black else Color.White)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showPasswordDialog = false }) {
                            Text(if (isEnglish) "Cancel" else "取消", color = Color.White.copy(alpha = 0.6f))
                        }
                    },
                    containerColor = Color(0xFF1E1E1E),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.widthIn(max = 320.dp)
                )
            }

            // Slide up visual tuning panel
            AnimatedVisibility(
                visible = showColorTunePanel,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    // Intercept click to prevent exiting fullscreen when tuning colors
                    .clickable(enabled = false, onClick = {})
            ) {
                Card(
                    modifier = Modifier
                        .padding(bottom = 24.dp, start = 16.dp, end = 16.dp)
                        .widthIn(max = 500.dp)
                        .fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Black.copy(alpha = 0.85f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isEnglish) "Realtime Color Selection" else "大屏背景颜色实时调配",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            IconButton(
                                onClick = { showColorTunePanel = false },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = Color.White.copy(alpha = 0.7f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Divider(color = Color.White.copy(alpha = 0.15f))

                        // Clock Style selector
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = if (isEnglish) "Clock Display Layout" else "大屏时钟展示布局",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(
                                    "vertical" to if (isEnglish) "Vertical Stacked" else "极简竖向叠放", 
                                    "stretched" to if (isEnglish) "Stretched Tall" else "行高拉伸样式",
                                    "horizontal" to if (isEnglish) "Classic Horizontal" else "经典横向单行"
                                ).forEach { (styleKey, styleName) ->
                                    val isSelected = clockStyle == styleKey
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .background(
                                                if (isSelected) Color.White.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.06f),
                                                RoundedCornerShape(8.dp)
                                            )
                                            .border(
                                                1.dp,
                                                if (isSelected) Color.White.copy(alpha = 0.6f) else Color.Transparent,
                                                RoundedCornerShape(8.dp)
                                            )
                                            .clickable { viewModel.setClockStyle(styleKey) }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = styleName,
                                            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.6f),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        // Clock Font Face selector
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = if (isEnglish) "Clock Typography Style" else "大屏数码字形样式",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf(
                                    "serif" to if (isEnglish) "Graceful Serif" else "高雅经典衬线", 
                                    "sans-serif" to if (isEnglish) "Sleek Sans" else "现代简洁无衬线", 
                                    "monospace" to if (isEnglish) "Digital Mono" else "极客复古等宽"
                                ).forEach { (fontKey, fontName) ->
                                    val isSelected = clockFont == fontKey
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .background(
                                                if (isSelected) Color.White.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.06f),
                                                RoundedCornerShape(8.dp)
                                            )
                                            .border(
                                                1.dp,
                                                if (isSelected) Color.White.copy(alpha = 0.6f) else Color.Transparent,
                                                RoundedCornerShape(8.dp)
                                            )
                                            .clickable { viewModel.setClockFont(fontKey) }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = fontName,
                                            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.6f),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        ColorPickerRow(
                            title = if (isEnglish) "Clock Numbers Color" else "大屏时分秒数字色系",
                            selectedColorInt = clockColorInt,
                            onColorSelected = { viewModel.setClockColor(it) },
                            colors = DarkThemeColors,
                            isEnglish = isEnglish
                        )

                        ColorPickerRow(
                            title = if (isEnglish) "Date & Day Label Color" else "大屏年月日星期色系",
                            selectedColorInt = dateColorInt,
                            onColorSelected = { viewModel.setDateColor(it) },
                            colors = DarkThemeColors,
                            isEnglish = isEnglish
                        )
                    }
                }
            }

            // Timed Alarm Alert Dialog Overlay
            if (activeRingtone != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.85f))
                        .clickable(enabled = false, onClick = {}),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier
                            .padding(24.dp)
                            .widthIn(max = 400.dp)
                            .fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF212121)),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(2.dp, Color.Red.copy(alpha = 0.8f))
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color.Red,
                                modifier = Modifier.size(64.dp)
                            )
                            
                            Text(
                                text = if (isEnglish) "Timer Finished!" else "定时提醒时间到！",
                                color = Color.White,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            
                            Text(
                                text = if (isEnglish) "Your countdown timer has expired." else "设置的倒计时提醒时间已达到。",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Button(
                                onClick = {
                                    try {
                                        activeRingtone?.stop()
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                    activeRingtone = null
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = if (isEnglish) "Stop Sound" else "我知道了 / 停止声音",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    } else {
        // Manual offset time calibration state dialog
        var showManualTimePicker by remember { mutableStateOf(false) }

        // Desktop Dashboard with side-by-side management specs
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(colors.background)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .padding(top = 12.dp, bottom = 12.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isEnglish) "INTEGRATED CONTROL" else "语音交互底座",
                            color = colors.accent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isEnglish) "System settings" else "调试工作台",
                            color = colors.textPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Floating fast configuration bar
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // English/Chinese Lang Toggle
                        IconButton(
                            onClick = { viewModel.setEnglish(!isEnglish) },
                            modifier = Modifier
                                .background(colors.surface, CircleShape)
                                .border(1.dp, colors.border, CircleShape)
                                .size(36.dp)
                        ) {
                            Text(
                                text = if (isEnglish) "EN" else "中",
                                color = colors.accent,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        // Theme Toggle
                        IconButton(
                            onClick = { viewModel.setDarkMode(!isDarkMode) },
                            modifier = Modifier
                                .background(colors.surface, CircleShape)
                                .border(1.dp, colors.border, CircleShape)
                                .size(36.dp)
                        ) {
                            Canvas(modifier = Modifier.size(16.dp)) {
                                drawArc(
                                    color = colors.accent,
                                    startAngle = 90f,
                                    sweepAngle = 180f,
                                    useCenter = true
                                )
                                drawCircle(
                                    color = colors.accent,
                                    radius = size.minDimension / 2f,
                                    style = Stroke(width = 1.5.dp.toPx())
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        // Fullscreen Toggle
                        Button(
                            onClick = { viewModel.setFullscreen(true) },
                            colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                            modifier = Modifier.height(38.dp).testTag("enter_fullscreen_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Clock",
                                tint = if (colors.isDark) Color.Black else Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = getLocalizedText(isEnglish, "clock_screen"),
                                color = if (colors.isDark) Color.Black else Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Workspace Panes
                if (!isPortrait) {
                    // Landscape side-by-side split screen
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Left Pane: Settings Groups (Scrollable List of Parameters)
                        Column(
                            modifier = Modifier
                                .weight(1.1f)
                                .fillMaxHeight()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // 1. Clock Info Summary
                            ClockInfoCard(timeStr, dateStr, weekStr, colors, isEnglish, clockColor = clockColor, dateColor = dateColor)

                            // 2. Display & Screen Rotation (Isolated prominent rotate switch!)
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = colors.surface),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, colors.border)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Refresh, null, tint = colors.accent, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isEnglish) "Display & Screen Rotation" else "显示屏方向与大屏设定",
                                            color = colors.textPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    HorizontalDivider(color = colors.border.copy(alpha = 0.5f), thickness = 1.dp)
                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Standalone prominent Rotate Button
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                            Text(
                                                text = if (isEnglish) "Switch Placement Orientation" else "一键切换屏幕方向 (横 / 竖屏)",
                                                color = colors.textPrimary,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                text = if (isEnglish) "Rotate layout to fit physical board mounting orientation" else "根据您的显示面板架设方式，一键旋转系统画布方向",
                                                color = colors.textSecondary,
                                                fontSize = 10.sp
                                            )
                                        }
                                        Button(
                                            onClick = onToggleOrientation,
                                            colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 14.dp),
                                            modifier = Modifier.height(36.dp).testTag("settings_rotate_screen_btn")
                                        ) {
                                            Icon(Icons.Default.Refresh, null, tint = if (colors.isDark) Color.Black else Color.White, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = if (isPortrait) (if (isEnglish) "Landscape" else "切至横屏") else (if (isEnglish) "Portrait" else "切至竖屏"),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (colors.isDark) Color.Black else Color.White
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    // Show seconds option row
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = if (isEnglish) "Display clock seconds" else "全屏时钟大屏显示秒数",
                                                color = colors.textPrimary,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                text = if (isEnglish) "High precision realtime ticking indicator" else "开关以控制全屏大屏上是否运行秒钟计数",
                                                color = colors.textSecondary,
                                                fontSize = 10.sp
                                            )
                                        }
                                        Switch(
                                            checked = showSeconds,
                                            onCheckedChange = { viewModel.setShowSeconds(it) },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = colors.success,
                                                checkedTrackColor = colors.success.copy(alpha = 0.5f)
                                            )
                                        )
                                    }
                                }
                            }

                            // 3. Live Video Ambient backgrounds
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = colors.surface),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, colors.border)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.PlayArrow, null, tint = colors.accent, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isEnglish) "Dynamic Video Backgrounds" else "时钟大屏动态视频背景",
                                            color = colors.textPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    HorizontalDivider(color = colors.border.copy(alpha = 0.5f), thickness = 1.dp)
                                    Spacer(modifier = Modifier.height(10.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = if (isEnglish) "Enable Video Backgrounds" else "启用大屏视频背景",
                                                color = colors.textPrimary,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                text = if (isEnglish) "Use selected mp4 background on full screen clock" else "开启时，全屏时钟下渲染自定义mp4动效",
                                                color = colors.textSecondary,
                                                fontSize = 10.sp
                                            )
                                        }
                                        Switch(
                                            checked = showVideoBackground,
                                            onCheckedChange = { viewModel.setShowVideoBackground(it) },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = colors.success,
                                                checkedTrackColor = colors.success.copy(alpha = 0.5f)
                                            )
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Portrait video path pick
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(colors.cardBackground, RoundedCornerShape(10.dp))
                                            .padding(10.dp)
                                    ) {
                                        Text(
                                            text = if (isEnglish) "Portrait MP4 Background File" else "📱 竖屏态渲染背景视频",
                                            color = colors.textPrimary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = videoUriPortraitStr ?: (if (isEnglish) "(Default pitch black background)" else "(未配置 - 默认使用工业级纯深邃黑色背景)"),
                                            color = if (videoUriPortraitStr != null) colors.accentSecondary else colors.textSecondary.copy(alpha = 0.5f),
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Button(
                                                onClick = {
                                                    targetPickerIsPortrait = true
                                                    try {
                                                        videoPickerLauncher.launch(arrayOf("video/*"))
                                                    } catch (e: Exception) {
                                                        e.printStackTrace()
                                                    }
                                                },
                                                shape = RoundedCornerShape(6.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = colors.border),
                                                modifier = Modifier.height(28.dp).weight(1f),
                                                contentPadding = PaddingValues(0.dp)
                                            ) {
                                                Text(if (isEnglish) "Choose Video" else "选择本地视频", fontSize = 10.sp, color = colors.textPrimary)
                                            }
                                            if (videoUriPortraitStr != null) {
                                                Button(
                                                    onClick = { viewModel.setVideoUriPortrait(null) },
                                                    shape = RoundedCornerShape(6.dp),
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                                                    modifier = Modifier.height(28.dp).weight(1f),
                                                    contentPadding = PaddingValues(0.dp)
                                                ) {
                                                    Text(if (isEnglish) "Reset" else "恢复默认", fontSize = 10.sp, color = Color.White)
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Landscape video path pick
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(colors.cardBackground, RoundedCornerShape(10.dp))
                                            .padding(10.dp)
                                    ) {
                                        Text(
                                            text = if (isEnglish) "Landscape MP4 Background File" else "🖥️ 横屏态渲染背景视频",
                                            color = colors.textPrimary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = videoUriLandscapeStr ?: (if (isEnglish) "(Default tech environment background)" else "(未配置 - 默认使用硬核技术粒子星空动效)"),
                                            color = if (videoUriLandscapeStr != null) colors.accentSecondary else colors.textSecondary.copy(alpha = 0.5f),
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Button(
                                                onClick = {
                                                    targetPickerIsPortrait = false
                                                    try {
                                                        videoPickerLauncher.launch(arrayOf("video/*"))
                                                    } catch (e: Exception) {
                                                        e.printStackTrace()
                                                    }
                                                },
                                                shape = RoundedCornerShape(6.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = colors.border),
                                                modifier = Modifier.height(28.dp).weight(1f),
                                                contentPadding = PaddingValues(0.dp)
                                            ) {
                                                Text(if (isEnglish) "Choose Video" else "选择本地视频", fontSize = 10.sp, color = colors.textPrimary)
                                            }
                                            if (videoUriLandscapeStr != null) {
                                                Button(
                                                    onClick = { viewModel.setVideoUriLandscape(null) },
                                                    shape = RoundedCornerShape(6.dp),
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                                                    modifier = Modifier.height(28.dp).weight(1f),
                                                    contentPadding = PaddingValues(0.dp)
                                                ) {
                                                    Text(if (isEnglish) "Reset" else "恢复默认", fontSize = 10.sp, color = Color.White)
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // 4. Voice Interaction card
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = colors.surface),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, colors.border)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Info, null, tint = colors.accent, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isEnglish) "Voice Wake-up & Interaction" else "声学拾音及语音交互唤醒",
                                            color = colors.textPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    HorizontalDivider(color = colors.border.copy(alpha = 0.5f), thickness = 1.dp)
                                    Spacer(modifier = Modifier.height(10.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = if (isEnglish) "Edge speech recognition" else "开启板载拾音控制",
                                                color = colors.textPrimary,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                text = if (isEnglish) "Listen to vocal 'Open X' wake-ups" else "是否启用边缘语音交互识别通道（关闭大幅省能）",
                                                color = colors.textSecondary,
                                                fontSize = 10.sp
                                            )
                                        }
                                        Switch(
                                            checked = voiceControlEnabled,
                                            onCheckedChange = { viewModel.setVoiceControlEnabled(it) },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = colors.success,
                                                checkedTrackColor = colors.success.copy(alpha = 0.5f)
                                            )
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Render voice monitor info
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(colors.cardBackground, RoundedCornerShape(10.dp))
                                            .padding(10.dp)
                                    ) {
                                        Text(
                                            text = if (isEnglish) "Vocal Signal wave & State Monitor:" else "🔬 物理拾音状态与分贝能量流：",
                                            color = colors.textPrimary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text(
                                                text = if (voiceControlEnabled) {
                                                    if (voiceState is VoiceState.Listening) (if (isEnglish) "Microphone: SPEAKING" else "音频状态: 【拾音中】")
                                                    else (if (isEnglish) "Microphone: IDLE" else "音频状态: 【冷空静默检测】")
                                                } else {
                                                    if (isEnglish) "Microphone: POWER OFF" else "音频状态: 【服务被玩家关闭】"
                                                },
                                                color = if (voiceControlEnabled) colors.accentSecondary else colors.textSecondary,
                                                fontSize = 10.sp
                                            )
                                            Text(
                                                text = String.format("RMS: %.1f dB", rmsLevel),
                                                color = colors.accent,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                        
                                        Spacer(modifier = Modifier.height(6.dp))
                                        
                                        // Visual progress gauge for sound level
                                        LinearProgressIndicator(
                                            progress = { (rmsLevel.coerceIn(0f, 30f) / 30f) },
                                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                            color = if (voiceState is VoiceState.Listening) colors.success else colors.accent.copy(alpha = 0.4f),
                                            trackColor = colors.border
                                        )

                                        Spacer(modifier = Modifier.height(6.dp))

                                        Text(
                                            text = if (isEnglish) "Speech Match Log:" else "📋 声控行为寻得日志：",
                                            color = colors.textPrimary,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = actionLog.ifEmpty { (if (isEnglish) "Waiting for speech signal..." else "尚未拦截到有效的打开语音控制流。") },
                                            color = if (actionLog.contains("寻径") || actionLog.contains("Launch")) colors.success else colors.textSecondary,
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }

                            // 5. Time Calibration setting Card
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = colors.surface),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, colors.border)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Settings, null, tint = colors.accent, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isEnglish) "Clock Time Correction" else "系统时钟同步与手动校准",
                                            color = colors.textPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    HorizontalDivider(color = colors.border.copy(alpha = 0.5f), thickness = 1.dp)
                                    Spacer(modifier = Modifier.height(10.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = if (isEnglish) "Network NTP Auto-Sync" else "自动云端同步时间",
                                                color = colors.textPrimary,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                text = if (isEnglish) "Auto fetch standard network baseline" else "自动通过联网对齐标准网卡时间基准线",
                                                color = colors.textSecondary,
                                                fontSize = 10.sp
                                            )
                                        }
                                        Switch(
                                            checked = useSystemTime,
                                            onCheckedChange = { viewModel.setUseSystemTime(it) },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = colors.success,
                                                checkedTrackColor = colors.success.copy(alpha = 0.5f)
                                            )
                                        )
                                    }

                                    if (!useSystemTime) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = if (isEnglish) "Manual Calibrate Hours" else "手动定点时差微调",
                                                    color = colors.textPrimary,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                                Text(
                                                    text = if (isEnglish) "Specify offset time to display custom dial" else "自定义一个固定差值，满足内网无网络需求",
                                                    color = colors.textSecondary,
                                                    fontSize = 10.sp
                                                )
                                            }
                                            Button(
                                                onClick = { showManualTimePicker = true },
                                                colors = ButtonDefaults.buttonColors(containerColor = colors.border),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.height(36.dp)
                                            ) {
                                                Text(if (isEnglish) "Set Time" else "手动校置时间", fontSize = 11.sp, color = colors.textPrimary)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Right Pane: Separate Bindings & Hardware Diagnostic tabs
                        TabsContainerCard(
                            appMappings = appMappings,
                            viewModel = viewModel,
                            selectedSlotForMapping = selectedSlotForMapping,
                            showAppSelector = showAppSelector,
                            onOpenSelector = { selectedSlotForMapping = it; showAppSelector = true },
                            colors = colors,
                            isEnglish = isEnglish,
                            context = context,
                            modifier = Modifier.weight(1.3f).fillMaxHeight()
                        )
                    }
                } else {
                    // Portrait stacked list screen (highly prioritized vertically)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ClockInfoCard(timeStr, dateStr, weekStr, colors, isEnglish, clockColor = clockColor, dateColor = dateColor)

                        // Standalone Option: Display Direction Rotation
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = colors.surface),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, colors.border)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                    Text(
                                        text = if (isEnglish) "Device Layout Rotation" else "屏幕显示方向：切换横竖屏",
                                        color = colors.textPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (isEnglish) "Rotates layout canvas immediately" else "一键重设横板/竖板绘图适配",
                                        color = colors.textSecondary,
                                        fontSize = 10.sp
                                    )
                                }
                                Button(
                                    onClick = onToggleOrientation,
                                    colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Refresh, null, tint = if (colors.isDark) Color.Black else Color.White, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isEnglish) "Toggle Screen Orientation" else "一键旋转",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = if (colors.isDark) Color.Black else Color.White
                                    )
                                }
                            }
                        }

                        // Compact Switch Options (Seconds, Video backing, Auto Time, Voice Toggle) in a single card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = colors.surface),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, colors.border)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(
                                    text = if (isEnglish) "Feature Configuration Switches" else "基础板载功能项开关",
                                    color = colors.textPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                HorizontalDivider(color = colors.border.copy(alpha = 0.5f), thickness = 1.dp)

                                // Seconds
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(if (isEnglish) "Show Clock Seconds" else "时钟大屏显示秒数", color = colors.textPrimary, fontSize = 12.sp)
                                    Switch(
                                        checked = showSeconds,
                                        onCheckedChange = { viewModel.setShowSeconds(it) },
                                        colors = SwitchDefaults.colors(checkedThumbColor = colors.success, checkedTrackColor = colors.success.copy(alpha = 0.5f))
                                    )
                                }

                                // Video backing
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(if (isEnglish) "Enable Video Wallpaper" else "启用大屏动效背景", color = colors.textPrimary, fontSize = 12.sp)
                                    Switch(
                                        checked = showVideoBackground,
                                        onCheckedChange = { viewModel.setShowVideoBackground(it) },
                                        colors = SwitchDefaults.colors(checkedThumbColor = colors.success, checkedTrackColor = colors.success.copy(alpha = 0.5f))
                                    )
                                }

                                // Voice Control toggle
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(if (isEnglish) "Speech Service Wake-up" else "开启板卡语音交互唤醒", color = colors.textPrimary, fontSize = 12.sp)
                                    Switch(
                                        checked = voiceControlEnabled,
                                        onCheckedChange = { viewModel.setVoiceControlEnabled(it) },
                                        colors = SwitchDefaults.colors(checkedThumbColor = colors.success, checkedTrackColor = colors.success.copy(alpha = 0.5f))
                                    )
                                }

                                // NTP sync
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(if (isEnglish) "Network Sync NTP" else "自适应在线时间同步", color = colors.textPrimary, fontSize = 12.sp)
                                    Switch(
                                        checked = useSystemTime,
                                        onCheckedChange = { viewModel.setUseSystemTime(it) },
                                        colors = SwitchDefaults.colors(checkedThumbColor = colors.success, checkedTrackColor = colors.success.copy(alpha = 0.5f))
                                    )
                                }

                                if (!useSystemTime) {
                                    Button(
                                        onClick = { showManualTimePicker = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = colors.border),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(if (isEnglish) "Manual Calibrate Time" else "手动点击定时差校准时间", fontSize = 11.sp, color = colors.textPrimary)
                                    }
                                }
                            }
                        }

                        // Background MP4 Video File Setup Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = colors.surface),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, colors.border)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    text = if (isEnglish) "Live MP4 video mappings" else "横视 / 竖视频动态背景路径设定",
                                    color = colors.textPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                HorizontalDivider(color = colors.border.copy(alpha = 0.5f), thickness = 1.dp)

                                Text(if (isEnglish) "📱 Portrait Video:" else "📱 竖直大屏运行背景：", fontSize = 11.sp, color = colors.textPrimary, fontWeight = FontWeight.Bold)
                                Text(
                                    text = videoUriPortraitStr ?: (if (isEnglish) "Pure black" else "默认深纯黑色节能背景"),
                                    fontSize = 9.sp,
                                    color = colors.textSecondary,
                                    fontFamily = FontFamily.Monospace,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = {
                                            targetPickerIsPortrait = true
                                            try { videoPickerLauncher.launch(arrayOf("video/*")) } catch (e: Exception) { e.printStackTrace() }
                                        },
                                        shape = RoundedCornerShape(6.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = colors.border),
                                        modifier = Modifier.weight(1f).height(30.dp),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text(if (isEnglish) "Choose File" else "自选MP4", fontSize = 10.sp, color = colors.textPrimary)
                                    }
                                    if (videoUriPortraitStr != null) {
                                        Button(
                                            onClick = { viewModel.setVideoUriPortrait(null) },
                                            shape = RoundedCornerShape(6.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                                            modifier = Modifier.weight(1f).height(30.dp),
                                            contentPadding = PaddingValues(0.dp)
                                        ) {
                                            Text(if (isEnglish) "Reset" else "恢复默认", fontSize = 10.sp, color = Color.White)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(if (isEnglish) "🖥️ Landscape Video:" else "🖥️ 横置大屏运行背景：", fontSize = 11.sp, color = colors.textPrimary, fontWeight = FontWeight.Bold)
                                Text(
                                    text = videoUriLandscapeStr ?: (if (isEnglish) "System visualizer" else "系统技术粒子动态太空背景"),
                                    fontSize = 9.sp,
                                    color = colors.textSecondary,
                                    fontFamily = FontFamily.Monospace,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = {
                                            targetPickerIsPortrait = false
                                            try { videoPickerLauncher.launch(arrayOf("video/*")) } catch (e: Exception) { e.printStackTrace() }
                                        },
                                        shape = RoundedCornerShape(6.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = colors.border),
                                        modifier = Modifier.weight(1f).height(30.dp),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text(if (isEnglish) "Choose File" else "自选MP4", fontSize = 10.sp, color = colors.textPrimary)
                                    }
                                    if (videoUriLandscapeStr != null) {
                                        Button(
                                            onClick = { viewModel.setVideoUriLandscape(null) },
                                            shape = RoundedCornerShape(6.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                                            modifier = Modifier.weight(1f).height(30.dp),
                                            contentPadding = PaddingValues(0.dp)
                                        ) {
                                            Text(if (isEnglish) "Reset" else "恢复默认", fontSize = 10.sp, color = Color.White)
                                        }
                                    }
                                }
                            }
                        }

                        // Combined application packaging mapping list & motherboard sensor diagnostics
                        TabsContainerCard(
                            appMappings = appMappings,
                            viewModel = viewModel,
                            selectedSlotForMapping = selectedSlotForMapping,
                            showAppSelector = showAppSelector,
                            onOpenSelector = { selectedSlotForMapping = it; showAppSelector = true },
                            colors = colors,
                            isEnglish = isEnglish,
                            context = context,
                            modifier = Modifier.fillMaxWidth().height(480.dp)
                        )
                    }
                }
            }
        }

        // Show manual calibrator if requested
        if (showManualTimePicker) {
            ManualSystemTimePickerDialog(
                isEnglish = isEnglish,
                colors = colors,
                onSaveTime = { h, m, s -> viewModel.setCustomTime(h, m, s) },
                onDismiss = { showManualTimePicker = false }
            )
        }
    }

    // Modal dialog to select app activity for trigger
    if (showAppSelector) {
        AppSelectorDialog(
            viewModel = viewModel,
            isEnglish = isEnglish,
            colors = colors,
            onDismiss = { showAppSelector = false },
            onAppSelected = { packageName, appLabel ->
                viewModel.bindAppToSlot(selectedSlotForMapping, packageName, appLabel)
                showAppSelector = false
            }
        )
    }

    // Modal verification dialog for app lock (accidental clicks / child lock)
    pendingAppLaunch?.let { mapping ->
        var passwordInput by remember { mutableStateOf("") }
        var passwordError by remember { mutableStateOf(false) }
        var passwordVisible by remember { mutableStateOf(false) }
        val focusRequester = remember { FocusRequester() }

        LaunchedEffect(mapping) {
            kotlinx.coroutines.delay(150L) // Settle the dialog
            try {
                focusRequester.requestFocus()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        AlertDialog(
            onDismissRequest = { viewModel.clearPendingAppLaunch() },
            title = {
                Text(
                    text = if (isEnglish) "App Launch Verification" else "开启应用认证",
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (isEnglish) {
                            "To launch ${mapping.appName}, please enter its secure password:"
                        } else {
                            "启动指代应用【${mapping.appName}】需要验证密码，以防止误触或儿童使用："
                        },
                        color = colors.textSecondary,
                        fontSize = 14.sp
                    )
                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = {
                            passwordInput = it
                            passwordError = false
                        },
                        placeholder = { 
                            Text(
                                if (isEnglish) "Enter app password..." else "请输入应用安全密码...",
                                color = colors.textSecondary.copy(alpha = 0.5f)
                            ) 
                        },
                        singleLine = true,
                        isError = passwordError,
                        visualTransformation = if (passwordVisible) {
                            androidx.compose.ui.text.input.VisualTransformation.None
                        } else {
                            androidx.compose.ui.text.input.PasswordVisualTransformation()
                        },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        ),
                        trailingIcon = {
                            val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                            val description = if (passwordVisible) {
                                if (isEnglish) "Hide password" else "隐藏密码"
                            } else {
                                if (isEnglish) "Show password" else "显示密码"
                            }
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = image,
                                    contentDescription = description,
                                    tint = colors.textSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            errorTextColor = Color.Red,
                            focusedBorderColor = colors.accent,
                            unfocusedBorderColor = colors.border,
                            errorBorderColor = Color.Red
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                    )
                    if (passwordError) {
                        Text(
                            text = if (isEnglish) "Incorrect password!" else "安全密码不正确，请重新输入！",
                            color = Color.Red,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val verified = viewModel.checkAndLaunchApp(mapping, passwordInput)
                        if (!verified) {
                            passwordError = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.accent)
                ) {
                    Text(
                        if (isEnglish) "Confirm" else "确认", 
                        color = if (colors.isDark) Color.Black else Color.White
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.clearPendingAppLaunch() }) {
                    Text(
                        if (isEnglish) "Cancel" else "取消", 
                        color = colors.textSecondary
                    )
                }
            },
            containerColor = colors.surface,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.widthIn(max = 320.dp)
        )
    }
}

// ---------------------- SUB-COMPOSABLES ----------------------

@Composable
fun ClockInfoCard(
    timeStr: String,
    dateStr: String,
    weekStr: String,
    colors: ThemeColors,
    isEnglish: Boolean,
    clockColor: Color,
    dateColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        shape = RoundedCornerShape(16.dp),
        border = BoxBorder(borderColor = colors.border)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Highly prioritized maximized hours:minutes:seconds
            Text(
                text = timeStr,
                color = clockColor,
                fontSize = 42.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif,
                maxLines = 1,
                softWrap = false,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Other details row / column strictly below the time display
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = dateStr,
                    color = dateColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = weekStr,
                    color = dateColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = getLocalizedText(isEnglish, "timezone"),
                color = colors.textSecondary,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun VoiceControlCard(
    voiceState: VoiceState,
    actionLog: String,
    rmsLevel: Float,
    colors: ThemeColors,
    isEnglish: Boolean,
    voiceControlEnabled: Boolean,
    onToggleVoice: (Boolean) -> Unit,
    onToggleVoiceControl: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        shape = RoundedCornerShape(16.dp),
        border = BoxBorder(borderColor = colors.border)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = getLocalizedText(isEnglish, "voice_status"),
                    color = colors.textPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                
                if (!voiceControlEnabled) {
                    Box(
                        modifier = Modifier
                            .background(colors.success.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (isEnglish) "Power Saved" else "极速省电中",
                            color = colors.success,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Active listening indicator and log
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.cardBackground, RoundedCornerShape(8.dp))
                    .padding(10.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (!voiceControlEnabled) Color.Gray
                                    else if (voiceState is VoiceState.Listening) colors.success 
                                    else colors.accentSecondary
                                )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (!voiceControlEnabled) {
                                if (isEnglish) "Microphone Idle (Power Saving)" else "麦克风已闭锁关停 (省电中)"
                            } else {
                                when (voiceState) {
                                    is VoiceState.Idle -> if (isEnglish) "Microphone Idle" else "话筒休眠中"
                                    is VoiceState.Listening -> if (isEnglish) "Listening • Say \"Open X\"" else "话筒持续监听中 • 唤醒词 [打开 X]"
                                    is VoiceState.PartialText -> if (isEnglish) "Converting speech..." else "抓包音波转换中"
                                    is VoiceState.Success -> if (isEnglish) "Match Succeeded" else "匹配成功"
                                    is VoiceState.Ready -> if (isEnglish) "Waiting for input..." else "等待声音输入"
                                    is VoiceState.Error -> if (isEnglish) "Muted / Error" else "监听出错已静默"
                                }
                            },
                            color = colors.textSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = if (!voiceControlEnabled) {
                            if (isEnglish) "Voice features are turned off. Enable to listen." else "语音后台拾音已全部断开，轻触下方一键开启"
                        } else {
                            actionLog
                        },
                        color = if (!voiceControlEnabled) colors.textSecondary.copy(alpha = 0.5f) else getVoiceLogColor(voiceState),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Real-time Wave Animation
            Text(
                text = if (isEnglish) "Rms Energy Spectrum :" else "拾音动能波谱 :",
                color = colors.textSecondary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(colors.cardBackground, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (voiceControlEnabled) {
                        RmsWaveIndicator(rmsLevel = rmsLevel, state = voiceState)
                    } else {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            repeat(7) {
                                Box(
                                    modifier = Modifier
                                        .size(width = 3.dp, height = 4.dp)
                                        .clip(CircleShape)
                                        .background(colors.textSecondary.copy(alpha = 0.2f))
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (voiceControlEnabled) "Rms: ${String.format("%.1f", rmsLevel)} dB" else "Offline / 0.0 dB",
                        color = colors.textSecondary.copy(alpha = 0.5f),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Quick restart buttons inside card bottom
            if (voiceControlEnabled) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onToggleVoice(true) },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.success),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "start",
                            tint = Color.Black,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(getLocalizedText(isEnglish, "start_voice"), fontSize = 11.sp, color = Color.Black)
                    }

                    OutlinedButton(
                        onClick = { onToggleVoice(false) },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                        border = BoxBorder(borderColor = Color.Red.copy(alpha = 0.3f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "stop",
                            tint = Color.Red,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(getLocalizedText(isEnglish, "stop_voice"), fontSize = 11.sp, color = Color.Red)
                    }
                }
            } else {
                Button(
                    onClick = { onToggleVoiceControl(true) },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "enable voice control",
                        tint = if (colors.isDark) Color.Black else Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isEnglish) "Enable Background Voice Control" else "一键启用语音控制后台",
                        fontSize = 11.sp,
                        color = if (colors.isDark) Color.Black else Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun VoiceControlCompactCard(
    voiceState: VoiceState,
    actionLog: String,
    colors: ThemeColors,
    isEnglish: Boolean,
    voiceControlEnabled: Boolean,
    onToggleVoice: (Boolean) -> Unit,
    onToggleVoiceControl: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        shape = RoundedCornerShape(16.dp),
        border = BoxBorder(borderColor = colors.border)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                if (!voiceControlEnabled) Color.Gray
                                else if (voiceState is VoiceState.Listening) colors.success 
                                else colors.accentSecondary
                            )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isEnglish) "VOICE STATUS" else "麦克风状态",
                        color = colors.textSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Small quick toggle button
                if (voiceControlEnabled) {
                    IconButton(
                        onClick = { 
                            if (voiceState is VoiceState.Listening) onToggleVoice(false) else onToggleVoice(true) 
                        },
                        modifier = Modifier
                            .background(colors.cardBackground, CircleShape)
                            .size(30.dp)
                    ) {
                        Icon(
                            imageVector = if (voiceState is VoiceState.Listening) Icons.Default.Close else Icons.Default.PlayArrow,
                            contentDescription = "mic icon",
                            tint = if (voiceState is VoiceState.Listening) Color.Red else colors.accent,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                } else {
                    IconButton(
                        onClick = { onToggleVoiceControl(true) },
                        modifier = Modifier
                            .background(colors.success.copy(alpha = 0.15f), CircleShape)
                            .size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "enable icon",
                            tint = colors.success,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (!voiceControlEnabled) {
                    if (isEnglish) "Voice control disabled (battery saved)" else "语音控制关闭中 (后台不监听，省电)"
                } else {
                    actionLog
                },
                color = if (!voiceControlEnabled) colors.textSecondary.copy(alpha = 0.6f) else getVoiceLogColor(voiceState),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun TabsContainerCard(
    appMappings: List<com.example.data.AppMapping>,
    viewModel: MainViewModel,
    selectedSlotForMapping: Int,
    showAppSelector: Boolean,
    onOpenSelector: (Int) -> Unit,
    colors: ThemeColors,
    isEnglish: Boolean,
    context: Context,
    modifier: Modifier
) {
    var slotForPasswordConfig by remember { mutableStateOf<Int?>(null) }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        shape = RoundedCornerShape(16.dp),
        border = BoxBorder(borderColor = colors.border)
    ) {
        var activeTab by remember { mutableStateOf(0) }
        val tabs = listOf(
            getLocalizedText(isEnglish, "app_mappings"),
            getLocalizedText(isEnglish, "hw_detect"),
            getLocalizedText(isEnglish, "sys_detect")
        )

        Column(modifier = Modifier.fillMaxSize()) {
            TabRow(
                selectedTabIndex = activeTab,
                containerColor = colors.cardBackground,
                contentColor = colors.accent,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[activeTab]),
                        color = colors.accent
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = activeTab == index,
                        onClick = { activeTab = index },
                        text = {
                            Text(
                                text = title,
                                fontSize = 11.sp,
                                fontWeight = if (activeTab == index) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(12.dp)
            ) {
                when (activeTab) {
                    0 -> BindingsTab(
                        mappings = appMappings,
                        isEnglish = isEnglish,
                        colors = colors,
                        onBindClick = onOpenSelector,
                        onUnbindClick = { slot ->
                            viewModel.unbindSlot(slot)
                        },
                        onSetPasswordClick = { slot ->
                            slotForPasswordConfig = slot
                        }
                    )
                    1 -> HardwareDetectTab(context = context, isEnglish = isEnglish, colors = colors)
                    2 -> SystemDetectTab(context = context, isEnglish = isEnglish, colors = colors)
                }
            }
        }
    }

    // App Lock custom settings dialog
    slotForPasswordConfig?.let { slot ->
        val mapping = appMappings.find { it.slot == slot }
        if (mapping != null) {
            var newPassword by remember { mutableStateOf(mapping.password) }
            var passwordVisible by remember { mutableStateOf(false) }

            AlertDialog(
                onDismissRequest = { slotForPasswordConfig = null },
                title = {
                    Text(
                        text = if (isEnglish) "App Lock Settings (Slot $slot)" else "应用密码锁设置 (插槽 $slot)",
                        color = colors.textPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = if (isEnglish) {
                                "App: ${mapping.appName}\nSet a safety password for this shortcut to prevent children's accidental clicks."
                            } else {
                                "关联应用: ${mapping.appName}\n为该系统指令设置密码，可有效防止误触及儿童乱点。"
                            },
                            color = colors.textSecondary,
                            fontSize = 13.sp
                        )
                        OutlinedTextField(
                            value = newPassword,
                            onValueChange = { input ->
                                // Numeric only and max 12 chars
                                if (input.all { it.isDigit() } && input.length <= 12) {
                                    newPassword = input
                                }
                            },
                            label = { Text(if (isEnglish) "Password (digits only)" else "安全密码 (仅限数字)") },
                            placeholder = { Text(if (isEnglish) "Empty implies no lock" else "留空表示不设置密码锁") },
                            singleLine = true,
                            visualTransformation = if (passwordVisible) {
                                androidx.compose.ui.text.input.VisualTransformation.None
                            } else {
                                androidx.compose.ui.text.input.PasswordVisualTransformation()
                            },
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                            ),
                            trailingIcon = {
                                val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = image,
                                        contentDescription = null,
                                        tint = colors.textSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = colors.textPrimary,
                                unfocusedTextColor = colors.textPrimary,
                                focusedBorderColor = colors.accent,
                                unfocusedBorderColor = colors.border
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (mapping.password.isNotEmpty()) {
                            TextButton(
                                onClick = {
                                    viewModel.setAppPassword(slot, "")
                                    slotForPasswordConfig = null
                                }
                            ) {
                                Text(
                                    if (isEnglish) "Remove Lock" else "清除限锁",
                                    color = Color.Red,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Button(
                            onClick = {
                                viewModel.setAppPassword(slot, newPassword)
                                slotForPasswordConfig = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = colors.accent)
                        ) {
                            Text(
                                if (isEnglish) "Save" else "保存",
                                color = if (colors.isDark) Color.Black else Color.White
                            )
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { slotForPasswordConfig = null }) {
                        Text(if (isEnglish) "Cancel" else "取消", color = colors.textSecondary)
                    }
                },
                containerColor = colors.surface,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.widthIn(max = 330.dp)
            )
        }
    }
}

// ---------------------- BINDINGS SHORTCUTS TAB ----------------------
@Composable
fun BindingsTab(
    mappings: List<com.example.data.AppMapping>,
    isEnglish: Boolean,
    colors: ThemeColors,
    onBindClick: (Int) -> Unit,
    onUnbindClick: (Int) -> Unit,
    onSetPasswordClick: (Int) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        state = rememberLazyListState()
    ) {
        items((0..9).toList()) { slot ->
            val mapping = mappings.find { it.slot == slot }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("app_slot_card_$slot"),
                colors = CardDefaults.cardColors(
                    containerColor = if (mapping != null) colors.cardBackground else colors.slotEmpty
                ),
                shape = RoundedCornerShape(10.dp),
                border = BoxBorder(
                    borderColor = if (mapping != null) colors.accent.copy(alpha = 0.2f) else colors.border
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        // Slot badge
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (mapping != null) colors.accent else colors.cardBackground
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$slot",
                                color = if (mapping != null && colors.isDark) Color.Black else colors.textPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (isEnglish) "Voice word: " else "语音指令: ",
                                    color = colors.textSecondary,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = if (isEnglish) "Open $slot" else "打开 $slot",
                                    color = colors.accentSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(2.dp))

                            if (mapping != null) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = mapping.appName,
                                        color = colors.textPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    if (mapping.password.isNotEmpty()) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = "Locked",
                                            tint = colors.accent,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text(
                                            text = if (isEnglish) "Locked" else "已锁",
                                            color = colors.accent,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Normal
                                        )
                                    }
                                }
                                Text(
                                    text = mapping.packageName,
                                    color = colors.textSecondary,
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontFamily = FontFamily.Monospace
                                )
                            } else {
                                Text(
                                    text = getLocalizedText(isEnglish, "not_bound"),
                                    color = colors.textSecondary.copy(alpha = 0.5f),
                                    fontSize = 12.sp,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                )
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (mapping != null) {
                            // Password lock icon button
                            IconButton(
                                onClick = { onSetPasswordClick(slot) },
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(
                                        if (mapping.password.isNotEmpty()) {
                                            colors.accent.copy(alpha = 0.15f)
                                        } else {
                                            colors.border.copy(alpha = 0.12f)
                                        },
                                        CircleShape
                                    )
                            ) {
                                Icon(
                                    imageVector = if (mapping.password.isNotEmpty()) {
                                        Icons.Default.Lock
                                    } else {
                                        Icons.Default.LockOpen
                                    },
                                    contentDescription = "Set Password Lock",
                                    tint = if (mapping.password.isNotEmpty()) {
                                        colors.accent
                                    } else {
                                        colors.textSecondary.copy(alpha = 0.6f)
                                    },
                                    modifier = Modifier.size(14.dp)
                                )
                            }

                            IconButton(
                                onClick = { onUnbindClick(slot) },
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(Color.Red.copy(alpha = 0.12f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "unbind",
                                    tint = Color.Red,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        Button(
                            onClick = { onBindClick(slot) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (mapping != null) colors.cardBackground else colors.accent
                            ),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text(
                                text = if (mapping != null) 
                                    (if (isEnglish) "Rebind" else "重绑") 
                                else 
                                    (if (isEnglish) "Bind" else "绑定应用"),
                                fontSize = 11.sp,
                                color = if (mapping == null && colors.isDark) Color.Black else colors.textPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

// ---------------------- HARDWARE DETECTION TAB ----------------------
@Composable
fun HardwareDetectTab(context: Context, isEnglish: Boolean, colors: ThemeColors) {
    val clipboardManager = LocalClipboardManager.current
    val hardwareReport = remember { SystemHardwareDetector.generateHardwareAndSensorsMarkdownReport(context) }
    val specs = remember { SystemHardwareDetector.getHardwareSpecs(context) }
    val sensors = remember { SystemHardwareDetector.getAllSensorsDetail(context) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = getLocalizedText(isEnglish, "device_report"),
                color = colors.textSecondary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )

            // One click copy
            Button(
                onClick = {
                    clipboardManager.setText(AnnotatedString(hardwareReport))
                    Toast.makeText(context, getLocalizedText(isEnglish, "copied"), Toast.LENGTH_LONG).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = colors.success),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.height(28.dp).testTag("copy_hardware_report_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "copy",
                    tint = if (colors.isDark) Color.Black else Color.White,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = getLocalizedText(isEnglish, "copy_report"), 
                    fontSize = 11.sp, 
                    color = if (colors.isDark) Color.Black else Color.White, 
                    fontWeight = FontWeight.Bold
                )
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Summary card of specs
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = if (isEnglish) "📡 Screen & Device Summary" else "📡 屏幕及基础设备总览", 
                            color = colors.accent, 
                            fontSize = 12.sp, 
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        specs.forEach { spec ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(spec.key, color = colors.textSecondary, fontSize = 11.sp)
                                Text(spec.value, color = colors.textPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Divider(color = colors.border.copy(alpha = 0.5f))
                        }
                    }
                }
            }

            item {
                Text(
                    text = if (isEnglish) "🔍 Board Detected Sensors (${sensors.size}):" else "🔍 开发板已感知传感器明细 (${sensors.size} 个):",
                    color = colors.accentSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 6.dp, bottom = 4.dp)
                )
            }

            // Direct sensor list
            if (sensors.isEmpty()) {
                item {
                    Text(
                        text = if (isEnglish) "No sensors detected." else "未检测到物理传感器(在开发板调试请重启硬链接线缆)",
                        color = colors.textSecondary.copy(alpha = 0.5f),
                        fontSize = 11.sp,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }
            } else {
                items(sensors) { sensor ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = sensor.name,
                                    color = colors.textPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .background(colors.accent.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = sensor.typeStr,
                                        color = colors.accent,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(2.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (isEnglish) "Vendor: ${sensor.vendor}" else "芯片品牌: ${sensor.vendor}",
                                    color = colors.textSecondary,
                                    fontSize = 10.sp
                                )
                                Text(
                                    text = if (isEnglish) "Power: ${sensor.power}mA" else "功耗: ${sensor.power}mA",
                                    color = colors.textSecondary,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------- SYSTEM SPECIFICATIONS TAB ----------------------
@Composable
fun SystemDetectTab(context: Context, isEnglish: Boolean, colors: ThemeColors) {
    val clipboardManager = LocalClipboardManager.current
    val systemReport = remember { SystemHardwareDetector.generateSystemMarkdownReport(context) }
    val specs = remember { SystemHardwareDetector.getSystemSpecs(context) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = getLocalizedText(isEnglish, "system_report"),
                color = colors.textSecondary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )

            // Copy to clipboard
            Button(
                onClick = {
                    clipboardManager.setText(AnnotatedString(systemReport))
                    Toast.makeText(context, getLocalizedText(isEnglish, "copied"), Toast.LENGTH_LONG).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = colors.success),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.height(28.dp).testTag("copy_system_report_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "copy",
                    tint = if (colors.isDark) Color.Black else Color.White,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = getLocalizedText(isEnglish, "copy_report"), 
                    fontSize = 11.sp, 
                    color = if (colors.isDark) Color.Black else Color.White, 
                    fontWeight = FontWeight.Bold
                )
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(specs) { spec ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = spec.key,
                            color = colors.textSecondary,
                            fontSize = 11.sp
                        )
                        Text(
                            text = spec.value,
                            color = if (spec.value.contains("支持") || spec.value.contains("YES") || spec.value.contains("Supported")) colors.success else colors.textPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// ---------------------- DIALOG: APP SELECTOR ----------------------
@Composable
fun AppSelectorDialog(
    viewModel: MainViewModel,
    isEnglish: Boolean,
    colors: ThemeColors,
    onDismiss: () -> Unit,
    onAppSelected: (packageName: String, appLabel: String) -> Unit
) {
    val installedApps by viewModel.installedApps.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    val filteredApps = remember(installedApps, searchQuery) {
        installedApps.filter {
            it.label.contains(searchQuery, ignoreCase = true) ||
            it.packageName.contains(searchQuery, ignoreCase = true)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f)
                .testTag("app_selector_dialog"),
            colors = CardDefaults.cardColors(containerColor = colors.surface),
            shape = RoundedCornerShape(16.dp),
            border = BoxBorder(borderColor = colors.accent.copy(alpha = 0.2f))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                // Title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isEnglish) "Link App Shortcuts" else "关联本地系统应用",
                        color = colors.textPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(30.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "close", tint = colors.textPrimary)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Search field
                TextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { 
                        Text(
                            text = if (isEnglish) "Search local apps / packages..." else "智能搜索本板应用名称 / 包名...", 
                            color = colors.textSecondary.copy(alpha = 0.5f), 
                            fontSize = 12.sp
                        ) 
                    },
                    leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "search", tint = colors.textSecondary) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "clear", tint = colors.textPrimary)
                            }
                        }
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = colors.cardBackground,
                        unfocusedContainerColor = colors.cardBackground,
                        focusedIndicatorColor = colors.accent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary
                    ),
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // App list
                if (filteredApps.isEmpty()) {
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isEnglish) "No matching apps found" else "未寻找到可用的程序包 ⚠️",
                            color = colors.textSecondary,
                            fontSize = 13.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(filteredApps) { app ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(colors.cardBackground, RoundedCornerShape(8.dp))
                                    .clickable { onAppSelected(app.packageName, app.label) }
                                    .padding(8.dp)
                                    .testTag("app_select_item_${app.packageName}"),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // App Icon Wrap (Safely supporting raw drawables out of context)
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(colors.textSecondary.copy(alpha = 0.08f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    AppIconRenderer(drawable = app.icon, label = app.label, colors = colors)
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = app.label,
                                        color = colors.textPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = app.packageName,
                                        color = colors.textSecondary,
                                        fontSize = 10.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }

                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "select",
                                    tint = colors.success,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------- VIEW INTEROP APP ICON RENDERER ----------------------
@Composable
fun AppIconRenderer(drawable: Drawable?, label: String, colors: ThemeColors) {
    if (drawable != null) {
        AndroidView(
            modifier = Modifier.size(24.dp),
            factory = { ctx ->
                android.widget.ImageView(ctx).apply {
                    setImageDrawable(drawable)
                    scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
                }
            }
        )
    } else {
        val initial = if (label.isNotEmpty()) label[0].toString() else "?"
        Text(
            text = initial,
            color = colors.accent,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

// ---------------------- DIALOG: TIME SETTINGS & VIDEO PICKER HELPERS ----------------------
@Composable
fun VideoSelectorItem(
    title: String,
    subtitle: String,
    videoUri: String?,
    isEnglish: Boolean,
    colors: ThemeColors,
    onChoose: () -> Unit,
    onClear: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colors.cardBackground.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = title,
                color = colors.textPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                color = colors.textSecondary.copy(alpha = 0.8f),
                fontSize = 10.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (!videoUri.isNullOrEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isEnglish) "✓ Set" else "✓ 已设置",
                        color = colors.success,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = onChoose,
                        colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.height(28.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                    ) {
                        Text(
                            text = if (isEnglish) "Replace" else "更换",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (colors.isDark) Color.Black else Color.White
                        )
                    }
                    Button(
                        onClick = onClear,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.8f)),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.height(28.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                    ) {
                        Text(
                            text = if (isEnglish) "Delete" else "清除",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            } else {
                Button(
                    onClick = onChoose,
                    colors = ButtonDefaults.buttonColors(containerColor = colors.border),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().height(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "play video",
                        tint = colors.textPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isEnglish) "Select video" else "选择视频",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.textPrimary
                    )
                }
            }
        }
    }
}

// ---------------------- DIALOG: TIME SETTINGS ----------------------
@Composable
fun TimeSettingsDialog(
    isEnglish: Boolean,
    colors: ThemeColors,
    useSystemTime: Boolean,
    onToggleAuto: (Boolean) -> Unit,
    showSeconds: Boolean,
    onToggleSeconds: (Boolean) -> Unit,
    onSaveTime: (Int, Int, Int) -> Unit,
    videoUriPortrait: String?,
    videoUriLandscape: String?,
    onChooseVideoPortrait: () -> Unit,
    onClearVideoPortrait: () -> Unit,
    onChooseVideoLandscape: () -> Unit,
    onClearVideoLandscape: () -> Unit,
    selectedClockColor: Int,
    onSaveClockColor: (Int) -> Unit,
    selectedDateColor: Int,
    onSaveDateColor: (Int) -> Unit,
    voiceControlEnabled: Boolean,
    onToggleVoiceControl: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var hour by remember { mutableStateOf(12) }
    var minute by remember { mutableStateOf(0) }
    var second by remember { mutableStateOf(0) }
    val scrollState = rememberScrollState()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .testTag("time_settings_dialog"),
            colors = CardDefaults.cardColors(containerColor = colors.surface),
            shape = RoundedCornerShape(16.dp),
            border = BoxBorder(borderColor = colors.border)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(scrollState)
                    .padding(16.dp)
            ) {
                Text(
                    text = if (isEnglish) "Time Settings" else "调整校准时间",
                    color = colors.textPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Toggle Auto/Manual
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isEnglish) "Auto Sync Time" else "自动云端同步时间",
                        color = colors.textSecondary,
                        fontSize = 13.sp
                    )
                    Switch(
                        checked = useSystemTime,
                        onCheckedChange = { onToggleAuto(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = colors.success,
                            checkedTrackColor = colors.success.copy(alpha = 0.5f)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Toggle Show Seconds
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isEnglish) "Show Seconds" else "大屏时钟显示秒数",
                        color = colors.textSecondary,
                        fontSize = 13.sp
                    )
                    Switch(
                        checked = showSeconds,
                        onCheckedChange = { onToggleSeconds(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = colors.success,
                            checkedTrackColor = colors.success.copy(alpha = 0.5f)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Toggle Voice Control & Listening (Power saving option)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isEnglish) "Voice Control & Wake-up" else "启用语音唤醒控制",
                            color = colors.textPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (isEnglish) "Disable to release mic & save power" else "关闭将释放麦克风并大幅节省电量",
                            color = colors.textSecondary.copy(alpha = 0.7f),
                            fontSize = 10.sp
                        )
                    }
                    Switch(
                        checked = voiceControlEnabled,
                        onCheckedChange = { onToggleVoiceControl(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = colors.success,
                            checkedTrackColor = colors.success.copy(alpha = 0.5f)
                        )
                    )
                }

                if (!useSystemTime) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (isEnglish) "Manual Adjustment Override :" else "手动设定时间数值 :",
                        color = colors.accentSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Hour adjusts
                        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("HH", color = colors.textSecondary, fontSize = 10.sp)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { if (hour > 0) hour-- }, modifier = Modifier.size(24.dp)) {
                                    Text("-", color = colors.textPrimary, fontSize = 16.sp)
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(String.format("%02d", hour), color = colors.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(4.dp))
                                IconButton(onClick = { if (hour < 23) hour++ }, modifier = Modifier.size(24.dp)) {
                                    Text("+", color = colors.textPrimary, fontSize = 16.sp)
                                }
                            }
                        }

                        // Minute adjusts
                        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("MM", color = colors.textSecondary, fontSize = 10.sp)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { if (minute > 0) minute-- }, modifier = Modifier.size(24.dp)) {
                                    Text("-", color = colors.textPrimary, fontSize = 16.sp)
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(String.format("%02d", minute), color = colors.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(4.dp))
                                IconButton(onClick = { if (minute < 59) minute++ }, modifier = Modifier.size(24.dp)) {
                                    Text("+", color = colors.textPrimary, fontSize = 16.sp)
                                }
                            }
                        }

                        // Second adjusts
                        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("SS", color = colors.textSecondary, fontSize = 10.sp)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { if (second > 0) second-- }, modifier = Modifier.size(24.dp)) {
                                    Text("-", color = colors.textPrimary, fontSize = 16.sp)
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(String.format("%02d", second), color = colors.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(4.dp))
                                IconButton(onClick = { if (second < 59) second++ }, modifier = Modifier.size(24.dp)) {
                                    Text("+", color = colors.textPrimary, fontSize = 16.sp)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = colors.border.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(12.dp))

                // DYNAMIC VIDEO BACKGROUND SECTION
                Text(
                    text = if (isEnglish) "Video Background" else "视频炫酷动态背景",
                    color = colors.textPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Portrait select block
                VideoSelectorItem(
                    title = if (isEnglish) "Portrait Background Video" else "竖屏动态视频背景",
                    subtitle = if (isEnglish) "Played only when screen is Portrait" else "仅在屏幕处于竖屏方向时载入并播放",
                    videoUri = videoUriPortrait,
                    isEnglish = isEnglish,
                    colors = colors,
                    onChoose = onChooseVideoPortrait,
                    onClear = onClearVideoPortrait
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Landscape select block
                VideoSelectorItem(
                    title = if (isEnglish) "Landscape Background Video" else "横屏动态视频背景",
                    subtitle = if (isEnglish) "Played only when screen is Landscape" else "仅在屏幕处于横屏方向时载入并播放",
                    videoUri = videoUriLandscape,
                    isEnglish = isEnglish,
                    colors = colors,
                    onChoose = onChooseVideoLandscape,
                    onClear = onClearVideoLandscape
                )

                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = colors.border.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(12.dp))

                // CUSTOM COLORS SECTION
                Text(
                    text = if (isEnglish) "Clock & Date Colors" else "时钟与细节色彩个性化",
                    color = colors.textPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))

                ColorPickerRow(
                    title = if (isEnglish) "Hours : Minutes : Seconds" else "时分秒数字色系",
                    selectedColorInt = selectedClockColor,
                    onColorSelected = onSaveClockColor,
                    colors = colors,
                    isEnglish = isEnglish
                )

                Spacer(modifier = Modifier.height(14.dp))

                ColorPickerRow(
                    title = if (isEnglish) "Year Month Day Week" else "年月日星期色系",
                    selectedColorInt = selectedDateColor,
                    onColorSelected = onSaveDateColor,
                    colors = colors,
                    isEnglish = isEnglish
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(text = if (isEnglish) "Cancel" else "取消", color = colors.textSecondary, fontSize = 13.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (!useSystemTime) {
                                onSaveTime(hour, minute, second)
                            }
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.accent)
                    ) {
                        Text(
                            text = if (isEnglish) "Apply" else "确认应用",
                            color = if (colors.isDark) Color.Black else Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ColorPickerRow(
    title: String,
    selectedColorInt: Int,
    onColorSelected: (Int) -> Unit,
    colors: ThemeColors,
    isEnglish: Boolean
) {
    val presets = listOf(
        0, // Theme default
        0xFFFFFFFF.toInt(),
        0xFF000000.toInt(),
        0xFF00F0FF.toInt(),
        0xFF00FF87.toInt(),
        0xFFFFD700.toInt(),
        0xFFFF9900.toInt(),
        0xFFFF3B30.toInt(),
        0xFF38BDF8.toInt(),
        0xFFFFB7B2.toInt(),
        0xFFE2CBF7.toInt()
    )

    // Calculate current HSV values for sliders
    val hsv = remember(selectedColorInt) {
        val arr = FloatArray(3)
        if (selectedColorInt == 0) {
            // Default fallbacks for slider reference (White)
            android.graphics.Color.colorToHSV(0xFFFFFFFF.toInt(), arr)
        } else {
            android.graphics.Color.colorToHSV(selectedColorInt, arr)
        }
        arr
    }

    val currentHue = hsv[0]
    val currentSaturation = hsv[1]
    val currentValue = hsv[2]

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            color = colors.textSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(8.dp))
        
        // Horizontal line of preset colors
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            presets.forEach { colorVal ->
                val isSelected = selectedColorInt == colorVal
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(
                            if (colorVal == 0) {
                                Color.Transparent
                            } else {
                                Color(colorVal)
                            }
                        )
                        .border(
                            width = if (isSelected) 3.dp else 1.dp,
                            color = if (isSelected) {
                                if (colors.isDark) Color.White else Color.Black
                            } else {
                                colors.border
                            },
                            shape = CircleShape
                        )
                        .clickable { onColorSelected(colorVal) },
                    contentAlignment = Alignment.Center
                ) {
                    if (colorVal == 0) {
                        Text(
                            text = if (isEnglish) "Def" else "默认",
                            color = colors.textSecondary,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else if (colorVal == 0xFFFFFFFF.toInt() && !colors.isDark) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.White)
                                .border(1.dp, colors.border, CircleShape)
                        )
                    } else if (colorVal == 0xFF000000.toInt() && colors.isDark) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black)
                                .border(1.dp, Color.DarkGray, CircleShape)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // FINE-TUNE UNIVERSAL SPECTRUM CONTROLS
        Text(
            text = if (isEnglish) "🎨 Universal Color Rainbow Slider" else "🎨 支持无级渐变滑动主色:",
            color = colors.textSecondary.copy(alpha = 0.8f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(6.dp))

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(18.dp)
        ) {
            val widthPx = constraints.maxWidth.toFloat()
            val gradientBrush = remember {
                androidx.compose.ui.graphics.Brush.horizontalGradient(
                    colors = listOf(
                        Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red
                    )
                )
            }

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(9.dp))
                    .pointerInput(currentSaturation, currentValue) {
                        detectTapGestures(
                            onPress = { offset ->
                                val x = offset.x.coerceIn(0f, widthPx)
                                val fraction = x / widthPx
                                val hueVal = fraction * 360f
                                val newColor = android.graphics.Color.HSVToColor(floatArrayOf(hueVal, currentSaturation, currentValue))
                                onColorSelected(newColor)
                            }
                        )
                    }
                    .pointerInput(currentSaturation, currentValue) {
                        detectDragGestures { change, _ ->
                            change.consume()
                            val x = change.position.x.coerceIn(0f, widthPx)
                            val fraction = x / widthPx
                            val hueVal = fraction * 360f
                            val newColor = android.graphics.Color.HSVToColor(floatArrayOf(hueVal, currentSaturation, currentValue))
                            onColorSelected(newColor)
                        }
                    }
            ) {
                drawRect(brush = gradientBrush)
                // Draw current position indicator circle
                val indicatorX = (currentHue / 360f) * size.width
                drawCircle(
                    color = Color.White,
                    radius = 7.dp.toPx(),
                    center = androidx.compose.ui.geometry.Offset(indicatorX, size.height / 2f),
                    style = Stroke(width = 2.dp.toPx())
                )
                drawCircle(
                    color = Color.Black,
                    radius = 8.dp.toPx(),
                    center = androidx.compose.ui.geometry.Offset(indicatorX, size.height / 2f),
                    style = Stroke(width = 1.dp.toPx())
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Value/Brightness and Saturation slider
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Brightness (Value) Slider
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isEnglish) "Brightness" else "亮度",
                    color = colors.textSecondary.copy(alpha = 0.8f),
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(16.dp)
                ) {
                    val widthPx = constraints.maxWidth.toFloat()
                    val baseColorWithHue = android.graphics.Color.HSVToColor(floatArrayOf(currentHue, 1f, 1f))
                    val brightnessBrush = remember(currentHue) {
                        androidx.compose.ui.graphics.Brush.horizontalGradient(
                            colors = listOf(Color.Black, Color(baseColorWithHue), Color.White)
                        )
                    }

                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(8.dp))
                            .pointerInput(currentHue, currentSaturation) {
                                detectTapGestures(
                                    onPress = { offset ->
                                        val x = offset.x.coerceIn(0f, widthPx)
                                        val fraction = x / widthPx
                                        val newColor = android.graphics.Color.HSVToColor(floatArrayOf(currentHue, currentSaturation, fraction))
                                        onColorSelected(newColor)
                                    }
                                )
                            }
                            .pointerInput(currentHue, currentSaturation) {
                                detectDragGestures { change, _ ->
                                    change.consume()
                                    val x = change.position.x.coerceIn(0f, widthPx)
                                    val fraction = x / widthPx
                                    val newColor = android.graphics.Color.HSVToColor(floatArrayOf(currentHue, currentSaturation, fraction))
                                    onColorSelected(newColor)
                                }
                            }
                    ) {
                        drawRect(brush = brightnessBrush)
                        val indicatorX = currentValue * size.width
                        drawCircle(
                            color = Color.White,
                            radius = 6.dp.toPx(),
                            center = androidx.compose.ui.geometry.Offset(indicatorX, size.height / 2f),
                            style = Stroke(width = 1.5.dp.toPx())
                        )
                    }
                }
            }

            // Saturation Slider
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isEnglish) "Saturation" else "饱和度",
                    color = colors.textSecondary.copy(alpha = 0.8f),
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(16.dp)
                ) {
                    val widthPx = constraints.maxWidth.toFloat()
                    val saturationBrush = remember(currentHue, currentValue) {
                        val startColor = android.graphics.Color.HSVToColor(floatArrayOf(currentHue, 0f, currentValue))
                        val endColor = android.graphics.Color.HSVToColor(floatArrayOf(currentHue, 1f, currentValue))
                        androidx.compose.ui.graphics.Brush.horizontalGradient(
                            colors = listOf(Color(startColor), Color(endColor))
                        )
                    }

                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(8.dp))
                            .pointerInput(currentHue, currentValue) {
                                detectTapGestures(
                                    onPress = { offset ->
                                        val x = offset.x.coerceIn(0f, widthPx)
                                        val fraction = x / widthPx
                                        val newColor = android.graphics.Color.HSVToColor(floatArrayOf(currentHue, fraction, currentValue))
                                        onColorSelected(newColor)
                                    }
                                )
                            }
                            .pointerInput(currentHue, currentValue) {
                                detectDragGestures { change, _ ->
                                    change.consume()
                                    val x = change.position.x.coerceIn(0f, widthPx)
                                    val fraction = x / widthPx
                                    val newColor = android.graphics.Color.HSVToColor(floatArrayOf(currentHue, fraction, currentValue))
                                    onColorSelected(newColor)
                                }
                            }
                    ) {
                        drawRect(brush = saturationBrush)
                        val indicatorX = currentSaturation * size.width
                        drawCircle(
                            color = Color.White,
                            radius = 6.dp.toPx(),
                            center = androidx.compose.ui.geometry.Offset(indicatorX, size.height / 2f),
                            style = Stroke(width = 1.5.dp.toPx())
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))

        // Custom Hex design color input box
        var hexInputText by remember(selectedColorInt) {
            mutableStateOf(if (selectedColorInt == 0) "" else String.format("%06X", selectedColorInt and 0xFFFFFF))
        }

        Row(
            modifier = Modifier.fillMaxWidth().height(52.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = hexInputText,
                onValueChange = { newValue ->
                    // Limit length and only allow relevant hexadecimal characters and '#'
                    val filtered = newValue.filter { it.isLetterOrDigit() || it == '#' }.take(9)
                    hexInputText = filtered
                    
                    val clean = filtered.trim().replace("#", "").uppercase()
                    if ((clean.length == 6 || clean.length == 8) && clean.all { it.isDigit() || it in 'A'..'F' }) {
                        val parsed = try {
                            if (clean.length == 6) {
                                ("FF" + clean).toLong(16).toInt()
                            } else {
                                clean.toLong(16).toInt()
                            }
                        } catch (e: Exception) {
                            null
                        }
                        if (parsed != null) {
                            onColorSelected(parsed)
                        }
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                placeholder = {
                    Text(
                        text = if (isEnglish) "Go to Hex (e.g. #325929)" else "自定义输入十六进制色值 (加不加#均可)",
                        fontSize = 10.sp,
                        color = colors.textSecondary.copy(alpha = 0.5f)
                    )
                },
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = if (colors.isDark) Color.White else colors.textPrimary
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.accent,
                    unfocusedBorderColor = colors.border.copy(alpha = 0.5f),
                    focusedContainerColor = if (colors.isDark) Color.Black.copy(alpha = 0.4f) else colors.cardBackground,
                    unfocusedContainerColor = if (colors.isDark) Color.Black.copy(alpha = 0.2f) else colors.cardBackground,
                    focusedTextColor = if (colors.isDark) Color.White else colors.textPrimary,
                    unfocusedTextColor = if (colors.isDark) Color.White else colors.textPrimary
                ),
                shape = RoundedCornerShape(8.dp)
            )

            // Dynamic color preview square, clicking it resets back to default (0)
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (selectedColorInt == 0) Color.Transparent else Color(selectedColorInt)
                    )
                    .border(
                        width = 1.5.dp,
                        color = if (selectedColorInt == 0) colors.border else Color.White.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clickable { onColorSelected(0) },
                contentAlignment = Alignment.Center
            ) {
                if (selectedColorInt == 0) {
                    Text(
                        text = if (isEnglish) "Def" else "默认",
                        color = colors.textSecondary,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    val r = (selectedColorInt shr 16) and 0xFF
                    val g = (selectedColorInt shr 8) and 0xFF
                    val b = selectedColorInt and 0xFF
                    val isLightColor = (r * 0.299 + g * 0.587 + b * 0.114) > 186
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "reset color",
                        tint = if (isLightColor) Color.Black else Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        if (selectedColorInt != 0) {
            Spacer(modifier = Modifier.height(4.dp))
            val hexString = String.format("#%08X", selectedColorInt)
            Text(
                text = if (isEnglish) "Selected hex value: $hexString" else "当前选中色值: $hexString (点击右侧 ✕ 重置)",
                color = colors.textSecondary.copy(alpha = 0.7f),
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}

// ---------------------- ANIMATED RMS SPECTRA INDICATOR ----------------------
@Composable
fun RmsWaveIndicator(rmsLevel: Float, state: VoiceState) {
    val isListening = state is VoiceState.Listening || state is VoiceState.PartialText
    val mappedDb = if (isListening) (rmsLevel + 2f).coerceIn(1f, 15f) else 1f
    
    val sizeScales = (0 until 5).map { index ->
        val anim = animateFloatAsState(
            targetValue = mappedDb * (0.4f + (index % 3) * 0.15f),
            animationSpec = spring(dampingRatio = 0.6f, stiffness = 150f),
            label = "bar_$index"
        )
        anim.value
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 4.dp).height(24.dp)
    ) {
        sizeScales.forEach { scale ->
            val barHeight = (4.dp + (scale * 2f).dp).coerceAtMost(24.dp)
            val barColor = if (isListening) Color(0xFF00F0FF) else Color.White.copy(alpha = 0.25f)
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(barHeight)
                    .clip(CircleShape)
                    .background(barColor)
            )
        }
    }
}

@Composable
fun getVoiceLogColor(state: VoiceState): Color {
    return when (state) {
        is VoiceState.Listening -> Color(0xFF00F0FF)
        is VoiceState.PartialText -> Color(0xFFFFCC00)
        is VoiceState.Success -> Color(0xFF00FF87)
        is VoiceState.Error -> Color(0xFFFF453A)
        else -> Color.White.copy(alpha = 0.7f)
    }
}

@Composable
fun BoxBorder(borderColor: Color): BorderStroke {
    return BorderStroke(1.dp, SolidColor(borderColor))
}

@Composable
fun QuickActionsRow(
    slot1Mapping: com.example.data.AppMapping?,
    slot2Mapping: com.example.data.AppMapping?,
    isEnglish: Boolean,
    colors: ThemeColors,
    isPortrait: Boolean,
    onLaunchSlot: (Int) -> Unit,
    onBindSlot: (Int) -> Unit,
    onToggleOrientation: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        shape = RoundedCornerShape(12.dp),
        border = BoxBorder(borderColor = colors.border)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Section Title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Quick Actions",
                        tint = colors.accent,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isEnglish) "Manual Controller Node" else "手动控制工作台",
                        color = colors.textPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Text(
                    text = if (isEnglish) "Quick Launch Slots" else "快捷唤启插槽手控板",
                    color = colors.textSecondary,
                    fontSize = 10.sp
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Button 1
                Button(
                    onClick = {
                        if (slot1Mapping != null) {
                            onLaunchSlot(1)
                        } else {
                            onBindSlot(1)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (slot1Mapping != null) colors.accent.copy(alpha = 0.15f) else colors.cardBackground
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .border(
                            1.dp,
                            if (slot1Mapping != null) colors.accent.copy(alpha = 0.4f) else colors.border,
                            RoundedCornerShape(8.dp)
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Slot 1",
                        tint = if (slot1Mapping != null) colors.accent else colors.textSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (slot1Mapping != null) {
                            "[1] ${slot1Mapping.appName}"
                        } else {
                            if (isEnglish) "[1] Bind App" else "[1] 绑定应用"
                        },
                        fontSize = 11.sp,
                        color = if (slot1Mapping != null) colors.textPrimary else colors.textSecondary,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Button 2
                Button(
                    onClick = {
                        if (slot2Mapping != null) {
                            onLaunchSlot(2)
                        } else {
                            onBindSlot(2)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (slot2Mapping != null) colors.accent.copy(alpha = 0.15f) else colors.cardBackground
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .border(
                            1.dp,
                            if (slot2Mapping != null) colors.accent.copy(alpha = 0.4f) else colors.border,
                            RoundedCornerShape(8.dp)
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Slot 2",
                        tint = if (slot2Mapping != null) colors.accent else colors.textSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (slot2Mapping != null) {
                            "[2] ${slot2Mapping.appName}"
                        } else {
                            if (isEnglish) "[2] Bind App" else "[2] 绑定应用"
                        },
                        fontSize = 11.sp,
                        color = if (slot2Mapping != null) colors.textPrimary else colors.textSecondary,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Orientation Toggle Button
                Button(
                    onClick = onToggleOrientation,
                    colors = ButtonDefaults.buttonColors(containerColor = colors.accentSecondary.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier
                        .weight(0.9f)
                        .height(42.dp)
                        .border(1.dp, colors.accentSecondary.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Rotate",
                        tint = colors.accentSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isEnglish) "Rotate" else if (isPortrait) "切至横屏" else "切至竖屏",
                        fontSize = 11.sp,
                        color = colors.textPrimary,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun VideoBackgroundPlayer(
    uriString: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    AndroidView(
        factory = { ctx ->
            VideoView(ctx).apply {
                setOnPreparedListener { mp ->
                    mp.isLooping = true
                    mp.setVolume(0f, 0f) // Mute audio
                    mp.start()
                }
                setOnErrorListener { _, _, _ ->
                    // Fail gracefully and avoid a crash popup
                    true
                }
            }
        },
        update = { videoView ->
            try {
                videoView.setVideoURI(Uri.parse(uriString))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        },
        modifier = modifier
    )
}

@Composable
fun ManualSystemTimePickerDialog(
    isEnglish: Boolean,
    colors: ThemeColors,
    onSaveTime: (Int, Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var hour by remember { mutableStateOf(12) }
    var minute by remember { mutableStateOf(0) }
    var second by remember { mutableStateOf(0) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .widthIn(max = 400.dp)
                .fillMaxWidth()
                .padding(24.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surface),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, colors.border)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isEnglish) "Manual Clock Calibration" else "时分秒定点时间偏差微调",
                    color = colors.textPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (isEnglish) "Calibrate standard time on standalone devices" else "在无外网NTP同步的独立运行设备下微调本地时显",
                    color = colors.textSecondary,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(if (isEnglish) "Hours" else "时", color = colors.accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { hour = if (hour > 0) hour - 1 else 23 }) {
                                Text("-", fontSize = 20.sp, color = colors.textPrimary)
                            }
                            Text(
                                text = String.format("%02d", hour),
                                color = colors.textPrimary,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            IconButton(onClick = { hour = if (hour < 23) hour + 1 else 0 }) {
                                Text("+", fontSize = 20.sp, color = colors.textPrimary)
                            }
                        }
                    }

                    Text(":", fontSize = 24.sp, color = colors.textSecondary, fontWeight = FontWeight.Bold)

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(if (isEnglish) "Minutes" else "分", color = colors.accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { minute = if (minute > 0) minute - 1 else 59 }) {
                                Text("-", fontSize = 20.sp, color = colors.textPrimary)
                            }
                            Text(
                                text = String.format("%02d", minute),
                                color = colors.textPrimary,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            IconButton(onClick = { minute = if (minute < 59) minute + 1 else 0 }) {
                                Text("+", fontSize = 20.sp, color = colors.textPrimary)
                            }
                        }
                    }

                    Text(":", fontSize = 24.sp, color = colors.textSecondary, fontWeight = FontWeight.Bold)

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(if (isEnglish) "Seconds" else "秒", color = colors.accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { second = if (second > 0) second - 1 else 59 }) {
                                Text("-", fontSize = 20.sp, color = colors.textPrimary)
                            }
                            Text(
                                text = String.format("%02d", second),
                                color = colors.textPrimary,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            IconButton(onClick = { second = if (second < 59) second + 1 else 0 }) {
                                Text("+", fontSize = 20.sp, color = colors.textPrimary)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = colors.cardBackground),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(if (isEnglish) "Cancel" else "取消放弃", color = colors.textPrimary, fontSize = 13.sp)
                    }
                    Button(
                        onClick = {
                            onSaveTime(hour, minute, second)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = if (isEnglish) "Apply" else "应用时偏校准",
                            color = if (colors.isDark) Color.Black else Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
