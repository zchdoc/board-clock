package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BaseApplication
import com.example.data.AppMapping
import com.example.data.AppMappingRepository
import com.example.utils.VoiceState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AppDetailsDetail(
    val packageName: String,
    val label: String,
    val icon: Drawable?
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository: AppMappingRepository = (application as BaseApplication).repository
    private val prefs = application.getSharedPreferences("board_clock_prefs", Context.MODE_PRIVATE)

    // Preference states
    private val _videoUriPortrait = MutableStateFlow(prefs.getString("video_uri_portrait", prefs.getString("video_uri", null)))
    val videoUriPortrait: StateFlow<String?> = _videoUriPortrait.asStateFlow()

    private val _videoUriLandscape = MutableStateFlow(prefs.getString("video_uri_landscape", prefs.getString("video_uri", null)))
    val videoUriLandscape: StateFlow<String?> = _videoUriLandscape.asStateFlow()

    private val _clockColor = MutableStateFlow(prefs.getInt("clock_color_int", 0))
    val clockColor: StateFlow<Int> = _clockColor.asStateFlow()

    private val _dateColor = MutableStateFlow(prefs.getInt("date_color_int", 0))
    val dateColor: StateFlow<Int> = _dateColor.asStateFlow()

    fun setClockColor(color: Int) {
        _clockColor.value = color
        prefs.edit().putInt("clock_color_int", color).apply()
    }

    fun setDateColor(color: Int) {
        _dateColor.value = color
        prefs.edit().putInt("date_color_int", color).apply()
    }

    fun setVideoUriPortrait(uri: String?) {
        _videoUriPortrait.value = uri
        if (uri != null) {
            prefs.edit().putString("video_uri_portrait", uri).apply()
        } else {
            prefs.edit().remove("video_uri_portrait").apply()
        }
    }

    fun setVideoUriLandscape(uri: String?) {
        _videoUriLandscape.value = uri
        if (uri != null) {
            prefs.edit().putString("video_uri_landscape", uri).apply()
        } else {
            prefs.edit().remove("video_uri_landscape").apply()
        }
    }

    private val _isEnglish = MutableStateFlow(prefs.getBoolean("is_english", false))
    val isEnglish: StateFlow<Boolean> = _isEnglish.asStateFlow()

    private val _isDarkMode = MutableStateFlow(prefs.getBoolean("is_dark_mode", true))
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _useSystemTime = MutableStateFlow(prefs.getBoolean("use_system_time", true))
    val useSystemTime: StateFlow<Boolean> = _useSystemTime.asStateFlow()

    private val _showSeconds = MutableStateFlow(prefs.getBoolean("show_seconds", true))
    val showSeconds: StateFlow<Boolean> = _showSeconds.asStateFlow()

    private val _timeOffsetMs = MutableStateFlow(prefs.getLong("time_offset_ms", 0L))
    val timeOffsetMs: StateFlow<Long> = _timeOffsetMs.asStateFlow()

    // State flow of app mappings from database
    val appMappings: StateFlow<List<AppMapping>> = repository.allMappings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Voice recognition states
    private val _voiceControlEnabled = MutableStateFlow(prefs.getBoolean("voice_control_enabled", true))
    val voiceControlEnabled: StateFlow<Boolean> = _voiceControlEnabled.asStateFlow()

    fun setVoiceControlEnabled(enabled: Boolean) {
        _voiceControlEnabled.value = enabled
        prefs.edit().putBoolean("voice_control_enabled", enabled).apply()
        if (!enabled) {
            _voiceState.value = VoiceState.Idle
            if (_isEnglish.value) {
                _lastActionLog.value = "Voice control is disabled."
            } else {
                _lastActionLog.value = "语音控制已关闭 (后台监听已释放)"
            }
        } else {
            if (_isEnglish.value) {
                _lastActionLog.value = "Voice listener ready."
            } else {
                _lastActionLog.value = "声学拾音识别引擎已就绪..."
            }
        }
    }

    private val _voiceState = MutableStateFlow<VoiceState>(VoiceState.Idle)
    val voiceState: StateFlow<VoiceState> = _voiceState.asStateFlow()

    private val _rmsLevel = MutableStateFlow(0f)
    val rmsLevel: StateFlow<Float> = _rmsLevel.asStateFlow()

    // Last activation action log
    private val _lastActionLog = MutableStateFlow("麦克风话筒准备完毕...")
    val lastActionLog: StateFlow<String> = _lastActionLog.asStateFlow()

    // Screen display configurations (fullscreen immersive style)
    private val _isFullscreen = MutableStateFlow(true) // Default to immersive clock face on startup
    val isFullscreen: StateFlow<Boolean> = _isFullscreen.asStateFlow()

    // System launchable app listings
    private val _installedApps = MutableStateFlow<List<AppDetailsDetail>>(emptyList())
    val installedApps: StateFlow<List<AppDetailsDetail>> = _installedApps.asStateFlow()

    // Search query for the app selection sheet
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    init {
        loadInstalledApps()
        // Initialize log message according to preference on load
        if (_isEnglish.value) {
            _lastActionLog.value = "Microphone is standing by..."
        }
    }

    fun setEnglish(enabled: Boolean) {
        _isEnglish.value = enabled
        prefs.edit().putBoolean("is_english", enabled).apply()
        // Update language-specific initial logs
        if (enabled) {
            _lastActionLog.value = "Microphone is standing by..."
        } else {
            _lastActionLog.value = "麦克风话筒准备完毕..."
        }
    }

    fun setDarkMode(enabled: Boolean) {
        _isDarkMode.value = enabled
        prefs.edit().putBoolean("is_dark_mode", enabled).apply()
    }

    fun setUseSystemTime(enabled: Boolean) {
        _useSystemTime.value = enabled
        prefs.edit().putBoolean("use_system_time", enabled).apply()
    }

    fun setShowSeconds(enabled: Boolean) {
        _showSeconds.value = enabled
        prefs.edit().putBoolean("show_seconds", enabled).apply()
    }

    fun setCustomTime(hour: Int, minute: Int, second: Int) {
        val calendar = java.util.Calendar.getInstance()
        calendar.set(java.util.Calendar.HOUR_OF_DAY, hour)
        calendar.set(java.util.Calendar.MINUTE, minute)
        calendar.set(java.util.Calendar.SECOND, second)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        
        val customMs = calendar.timeInMillis
        val elapsedMs = android.os.SystemClock.elapsedRealtime()
        val offset = customMs - elapsedMs
        _timeOffsetMs.value = offset
        prefs.edit().putLong("time_offset_ms", offset).apply()
        setUseSystemTime(false)
    }

    fun loadInstalledApps() {
        viewModelScope.launch {
            val context = getApplication<Application>().applicationContext
            val pm = context.packageManager
            val intent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolves = pm.queryIntentActivities(intent, 0)
            val apps = resolves.mapNotNull { resolve ->
                val packageName = resolve.activityInfo.packageName
                val label = resolve.loadLabel(pm).toString()
                
                // Exclude ourselves
                if (packageName == context.packageName) return@mapNotNull null
                
                val icon = try {
                    resolve.loadIcon(pm)
                } catch (e: Exception) {
                    null
                }
                AppDetailsDetail(packageName, label, icon)
            }.sortedBy { it.label }
            
            _installedApps.value = apps
        }
    }

    fun updateVoiceState(state: VoiceState) {
        _voiceState.value = state
        when (state) {
            is VoiceState.Listening -> {
                _rmsLevel.value = 0f
            }
            is VoiceState.PartialText -> {
                _lastActionLog.value = "监听到: \"${state.text}\""
            }
            is VoiceState.Success -> {
                _lastActionLog.value = "匹配文本: \"${state.matchValue}\""
            }
            is VoiceState.Error -> {
                // Keep it clean in action logs
                _lastActionLog.value = "话筒阻断: ${state.message}"
            }
            else -> {}
        }
    }

    fun updateRmsLevel(level: Float) {
        _rmsLevel.value = level
    }

    fun setFullscreen(fullscreen: Boolean) {
        _isFullscreen.value = fullscreen
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun bindAppToSlot(slot: Int, packageName: String, appName: String) {
        viewModelScope.launch {
            repository.insertMapping(AppMapping(slot = slot, packageName = packageName, appName = appName))
            _lastActionLog.value = "配置完成! 已将 [打开 $slot] 关联到: $appName"
        }
    }

    fun unbindSlot(slot: Int) {
        viewModelScope.launch {
            repository.deleteMapping(slot)
            _lastActionLog.value = "已解除关联插槽 [打开 $slot] 的映射"
        }
    }

    fun triggerAppLaunchBySlot(slot: Int) {
        viewModelScope.launch {
            val mappings = appMappings.value
            val mapping = mappings.find { it.slot == slot }
            if (mapping != null) {
                launchApp(mapping.packageName, mapping.appName)
            } else {
                _lastActionLog.value = "唤醒失败！系统快响槽 [打开 $slot] 尚未指代任何应用。"
            }
        }
    }

    private fun launchApp(packageName: String, appName: String) {
        val context = getApplication<Application>().applicationContext
        val pm = context.packageManager
        val launchIntent = pm.getLaunchIntentForPackage(packageName)
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                context.startActivity(launchIntent)
                _lastActionLog.value = "语音触发成功！正在后台调起: $appName"
            } catch (e: Exception) {
                _lastActionLog.value = "未能执行调起 $appName : ${e.message}"
            }
        } else {
            _lastActionLog.value = "寻径失败：无法定位 $packageName 口子。"
        }
    }
}
