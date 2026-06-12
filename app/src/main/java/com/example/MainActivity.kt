package com.example
 
import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.example.ui.screens.DashboardScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.utils.VoiceRecognizerManager
import com.example.utils.VoiceState
import com.example.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private var voiceRecognizerManager: VoiceRecognizerManager? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            if (viewModel.voiceControlEnabled.value) {
                setupAndStartVoiceListening()
            }
        } else {
            viewModel.updateVoiceState(
                VoiceState.Error("缺乏麦克风权限。请前往底座设置中授予录音控制权。")
            )
            Toast.makeText(this, "需要麦克风录音权限以启用语音唤醒！", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Collect voiceControlEnabled state to turn voice listener on/off reactively
        lifecycleScope.launch {
            viewModel.voiceControlEnabled.collect { enabled ->
                if (enabled) {
                    checkAndRequestMicrophonePermission()
                } else {
                    voiceRecognizerManager?.stopListening()
                }
            }
        }

        setContent {
            val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
            MyApplicationTheme(darkTheme = isDarkMode) {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    DashboardScreen(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding),
                        onToggleVoice = { enabled ->
                            if (viewModel.voiceControlEnabled.value) {
                                voiceRecognizerManager?.toggleContinuous(enabled) ?: run {
                                    if (enabled) checkAndRequestMicrophonePermission()
                                }
                            }
                        },
                        onToggleOrientation = {
                            requestedOrientation = if (resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT) {
                                android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                            } else {
                                android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                            }
                        }
                    )
                }
            }
        }
    }

    private fun checkAndRequestMicrophonePermission() {
        if (!viewModel.voiceControlEnabled.value) return
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED -> {
                setupAndStartVoiceListening()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    private fun setupAndStartVoiceListening() {
        if (!viewModel.voiceControlEnabled.value) return
        if (voiceRecognizerManager == null) {
            voiceRecognizerManager = VoiceRecognizerManager(
                context = this,
                onStateChanged = { state ->
                    viewModel.updateVoiceState(state)
                },
                onRmsChanged = { rms ->
                    viewModel.updateRmsLevel(rms)
                },
                onCommandMatched = { slot ->
                    viewModel.triggerAppLaunchBySlot(slot)
                }
            ).apply {
                init()
            }
        }
        voiceRecognizerManager?.startListening()
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadInstalledApps()
        if (viewModel.voiceControlEnabled.value && ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            voiceRecognizerManager?.startListening()
        }
    }

    override fun onPause() {
        super.onPause()
        voiceRecognizerManager?.stopListening()
    }

    override fun onDestroy() {
        super.onDestroy()
        voiceRecognizerManager?.destroy()
    }
}
