package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
        "title" to if (isEnglish) "Board Clock & Hardware Debug Workshop" else "开发板边缘时钟及软硬件调试工作台",
        "title_desc_sub" to if (isEnglish) "BOARD CLOCK INTEGRATED SYSTEM" else "边缘自适应工业级显示及语音交互底座",
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
    val videoUriStr by viewModel.videoUri.collectAsStateWithLifecycle()
    val clockColorInt by viewModel.clockColor.collectAsStateWithLifecycle()
    val dateColorInt by viewModel.dateColor.collectAsStateWithLifecycle()
    val voiceControlEnabled by viewModel.voiceControlEnabled.collectAsStateWithLifecycle()

    val colors = if (isDarkMode) DarkThemeColors else LightThemeColors
    val clockColor = if (clockColorInt != 0) Color(clockColorInt) else colors.accent
    val dateColor = if (dateColorInt != 0) Color(dateColorInt) else colors.textPrimary

    // Screen height and width parameters to adjust orientation beautifully
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isPortrait = configuration.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT

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
            viewModel.setVideoUri(uri.toString())
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
    val timeStr = remember(currentTimeMillis, locale) {
        SimpleDateFormat("HH:mm:ss", locale).format(Date(currentTimeMillis))
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

    if (isFullscreen) {
        var showColorTunePanel by remember { mutableStateOf(false) }

        // Fullscreen Clock display - Maximized hours, minutes, and seconds, with everything else below
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.background)
                .testTag("fullscreen_clock_view"),
            contentAlignment = Alignment.Center
        ) {
            // Render video dynamic background if configured
            if (!videoUriStr.isNullOrEmpty()) {
                VideoBackgroundPlayer(
                    uriString = videoUriStr!!,
                    modifier = Modifier.fillMaxSize()
                )
                // Half-black dim overlay to make white clock text pop perfectly!
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.55f))
                )
            }

            // Clickable background layer (tapping anywhere on the empty background exits fullscreen)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { viewModel.setFullscreen(false) }
            )

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                val maxW = maxWidth.value
                val maxH = maxHeight.value
                
                // Dynamically calculate font size to fill horizontal and vertical space safely
                val dynamicFontSize = remember(maxW, maxH) {
                    val scaleByW = maxW / 5.2f
                    val scaleByH = maxH / 2.8f
                    minOf(scaleByW, scaleByH)
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // 1. Hours:Minutes:Seconds (Centered, super maximized)
                    Text(
                        text = timeStr,
                        color = clockColor,
                        fontSize = dynamicFontSize.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        softWrap = false
                    )
                    
                    Spacer(modifier = Modifier.height((maxH * 0.05f).dp))

                    // 2. Date & Day (Directly below timeStr)
                    Text(
                        text = "$dateStr   $weekStr",
                        color = dateColor,
                        fontSize = maxOf(14f, dynamicFontSize * 0.25f).sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height((maxH * 0.02f).dp))
                    
                    // 3. Timezone/Calibrator message (Below day)
                    Text(
                        text = getLocalizedText(isEnglish, "timezone"),
                        color = colors.textSecondary,
                        fontSize = maxOf(10f, dynamicFontSize * 0.15f).sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height((maxH * 0.08f).dp))

                    // 4. Mode hint
                    Text(
                        text = if (isEnglish) "• TAP ANYWHERE TO EXIT •" else "• 轻触屏幕任意位置返回控制面板 •",
                        color = colors.textSecondary.copy(alpha = 0.5f),
                        fontSize = maxOf(9f, dynamicFontSize * 0.11f).sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Real-time custom color tuner button in top corner
            IconButton(
                onClick = { showColorTunePanel = !showColorTunePanel },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(20.dp)
                    .background(Color.Black.copy(alpha = 0.55f), CircleShape)
                    .size(44.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Tune Colors",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
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
        }
    } else {
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
                    .padding(top = 16.dp, bottom = 12.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = getLocalizedText(isEnglish, "title_desc_sub"),
                            color = colors.accent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = getLocalizedText(isEnglish, "title"),
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
                                // Draw left half filled
                                drawArc(
                                    color = colors.accent,
                                    startAngle = 90f,
                                    sweepAngle = 180f,
                                    useCenter = true
                                )
                                // Draw outer circle outline
                                drawCircle(
                                    color = colors.accent,
                                    radius = size.minDimension / 2f,
                                    style = Stroke(width = 1.5.dp.toPx())
                                )
                            }
                        }

                        // Time adjust triggering Dialog
                        var showTimeDialog by remember { mutableStateOf(false) }
                        IconButton(
                            onClick = { showTimeDialog = true },
                            modifier = Modifier
                                .background(colors.surface, CircleShape)
                                .border(1.dp, colors.border, CircleShape)
                                .size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Adjust Time",
                                tint = colors.accent,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        if (showTimeDialog) {
                            TimeSettingsDialog(
                                isEnglish = isEnglish,
                                colors = colors,
                                useSystemTime = useSystemTime,
                                onToggleAuto = { viewModel.setUseSystemTime(it) },
                                onSaveTime = { h, m, s -> viewModel.setCustomTime(h, m, s) },
                                videoUri = videoUriStr,
                                onChooseVideo = {
                                    try {
                                        videoPickerLauncher.launch(arrayOf("video/*"))
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                },
                                onClearVideo = { viewModel.setVideoUri(null) },
                                selectedClockColor = clockColorInt,
                                onSaveClockColor = { viewModel.setClockColor(it) },
                                selectedDateColor = dateColorInt,
                                onSaveDateColor = { viewModel.setDateColor(it) },
                                voiceControlEnabled = voiceControlEnabled,
                                onToggleVoiceControl = { viewModel.setVoiceControlEnabled(it) },
                                onDismiss = { showTimeDialog = false }
                            )
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
                                tint = if (isDarkMode) Color.Black else Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = getLocalizedText(isEnglish, "clock_screen"),
                                color = if (isDarkMode) Color.Black else Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Quick launch slot 1 & 2 controls and screen orientation toggle
                val slot1Mapping = appMappings.find { it.slot == 1 }
                val slot2Mapping = appMappings.find { it.slot == 2 }

                QuickActionsRow(
                    slot1Mapping = slot1Mapping,
                    slot2Mapping = slot2Mapping,
                    isEnglish = isEnglish,
                    colors = colors,
                    isPortrait = isPortrait,
                    onLaunchSlot = { slot ->
                        viewModel.triggerAppLaunchBySlot(slot)
                    },
                    onBindSlot = { slot ->
                        selectedSlotForMapping = slot
                        showAppSelector = true
                    },
                    onToggleOrientation = onToggleOrientation
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Workspace Panes
                if (!isPortrait) {
                    // Landscape side-by-side split screen
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Left Pane: Clock & Voice Level Analyzer
                        Column(
                            modifier = Modifier
                                .weight(1.2f)
                                .fillMaxHeight(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            ClockInfoCard(timeStr, dateStr, weekStr, colors, isEnglish, clockColor = clockColor, dateColor = dateColor)
                            VoiceControlCard(
                                voiceState = voiceState,
                                actionLog = actionLog,
                                rmsLevel = rmsLevel,
                                colors = colors,
                                isEnglish = isEnglish,
                                voiceControlEnabled = voiceControlEnabled,
                                onToggleVoice = onToggleVoice,
                                onToggleVoiceControl = { viewModel.setVoiceControlEnabled(it) }
                            )
                        }

                        // Right Pane: Control Tabs with binding, hardware info, system specs
                        TabsContainerCard(
                            appMappings, viewModel, selectedSlotForMapping, showAppSelector,
                            { selectedSlotForMapping = it; showAppSelector = true },
                            colors, isEnglish, context, modifier = Modifier.weight(1.8f).fillMaxHeight()
                        )
                    }
                } else {
                    // Portrait stacked list screen
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Stacking clock and voice level analyzer compactly as two separate columns/rows
                        ClockInfoCard(timeStr, dateStr, weekStr, colors, isEnglish, clockColor = clockColor, dateColor = dateColor)
                        VoiceControlCompactCard(
                            voiceState = voiceState,
                            actionLog = actionLog,
                            colors = colors,
                            isEnglish = isEnglish,
                            voiceControlEnabled = voiceControlEnabled,
                            onToggleVoice = onToggleVoice,
                            onToggleVoiceControl = { viewModel.setVoiceControlEnabled(it) }
                        )

                        // Bottom Workspace Tabs filling up the remaining room
                        TabsContainerCard(
                            appMappings, viewModel, selectedSlotForMapping, showAppSelector,
                            { selectedSlotForMapping = it; showAppSelector = true },
                            colors, isEnglish, context, modifier = Modifier.fillMaxWidth().weight(1f)
                        )
                    }
                }
            }
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
                        }
                    )
                    1 -> HardwareDetectTab(context = context, isEnglish = isEnglish, colors = colors)
                    2 -> SystemDetectTab(context = context, isEnglish = isEnglish, colors = colors)
                }
            }
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
    onUnbindClick: (Int) -> Unit
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

                        Column {
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
                                Text(
                                    text = mapping.appName,
                                    color = colors.textPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
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

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (mapping != null) {
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

// ---------------------- DIALOG: TIME SETTINGS ----------------------
@Composable
fun TimeSettingsDialog(
    isEnglish: Boolean,
    colors: ThemeColors,
    useSystemTime: Boolean,
    onToggleAuto: (Boolean) -> Unit,
    onSaveTime: (Int, Int, Int) -> Unit,
    videoUri: String?,
    onChooseVideo: () -> Unit,
    onClearVideo: () -> Unit,
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

                if (!videoUri.isNullOrEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = if (isEnglish) "✓ Custom video theme selected" else "✓ 已成功载入本地视频背景",
                                color = colors.success,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = onChooseVideo,
                                    colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.weight(1f).height(32.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text(
                                        text = if (isEnglish) "Replace" else "更换视频",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (colors.isDark) Color.Black else Color.White
                                    )
                                }
                                Button(
                                    onClick = onClearVideo,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.8f)),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.weight(1f).height(32.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text(
                                        text = if (isEnglish) "Delete" else "删除卸载",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Button(
                        onClick = onChooseVideo,
                        colors = ButtonDefaults.buttonColors(containerColor = colors.border),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().height(42.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "play video",
                            tint = colors.textPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isEnglish) "Choose Video Background" else "选择本地视频作为时钟背景",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.textPrimary
                        )
                    }
                }

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
