package com.example.utils

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log

class VoiceRecognizerManager(
    private val context: Context,
    private val onStateChanged: (VoiceState) -> Unit,
    onRmsChanged: (Float) -> Unit,
    private val onCommandMatched: (Int) -> Unit
) {
    private val onRmsChangedCallback = onRmsChanged
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private var continuousMode = true

    fun init() {
        try {
            if (SpeechRecognizer.isRecognitionAvailable(context)) {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(object : RecognitionListener {
                        override fun onReadyForSpeech(params: Bundle?) {
                            isListening = true
                            onStateChanged(VoiceState.Listening)
                        }

                        override fun onBeginningOfSpeech() {
                            onStateChanged(VoiceState.Listening)
                        }

                        override fun onRmsChanged(rmsdB: Float) {
                            onRmsChangedCallback(rmsdB)
                        }

                        override fun onBufferReceived(buffer: ByteArray?) {}

                        override fun onEndOfSpeech() {
                            onStateChanged(VoiceState.Ready)
                        }

                        override fun onError(error: Int) {
                            val message = when (error) {
                                SpeechRecognizer.ERROR_AUDIO -> "音频录制错误"
                                SpeechRecognizer.ERROR_CLIENT -> "客户端连接异常"
                                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "需要麦克风录音权限"
                                SpeechRecognizer.ERROR_NETWORK -> "网络通信出现异常"
                                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "网络库响应超时"
                                SpeechRecognizer.ERROR_NO_MATCH -> "未匹配到清晰语音"
                                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "语音引擎被占用或繁忙"
                                SpeechRecognizer.ERROR_SERVER -> "服务端发生未知差错"
                                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "未听到有效人声"
                                else -> "引擎状态异常 (代码: $error)"
                            }
                            
                            Log.e("VoiceRecognizer", "Error code: $error, value: $message")
                            onStateChanged(VoiceState.Error(message))
                            isListening = false
                            
                            // Delay auto-restart if continuous mode is enabled and speechrecognizer is available
                            if (continuousMode && speechRecognizer != null) {
                                startContinuousWithDelay()
                            }
                        }

                        override fun onResults(results: Bundle?) {
                            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            val text = matches?.firstOrNull() ?: ""
                            Log.d("VoiceRecognizer", "Final result received: $text")
                            
                            if (text.isNotEmpty()) {
                                onStateChanged(VoiceState.Success(text))
                                val slot = parseCommandAndGetSlot(text)
                                if (slot != null) {
                                    onCommandMatched(slot)
                                }
                            } else {
                                onStateChanged(VoiceState.Idle)
                            }
                            
                            isListening = false
                            if (continuousMode && speechRecognizer != null) {
                                startContinuousWithDelay()
                            }
                        }

                        override fun onPartialResults(partialResults: Bundle?) {
                            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            val text = matches?.firstOrNull() ?: ""
                            if (text.isNotEmpty()) {
                                onStateChanged(VoiceState.PartialText(text))
                            }
                        }

                        override fun onEvent(eventType: Int, params: Bundle?) {}
                    })
                }
            } else {
                onStateChanged(VoiceState.Error("系统不支持SpeechRecognizer，可能由于宿主底座缺乏中文TTS/语音库引擎。建议手动触屏控制或安装Google语音助手。"))
            }
        } catch (e: Exception) {
            Log.e("VoiceRecognizer", "SpeechRecognizer creation failed: ${e.message}")
            onStateChanged(VoiceState.Error("语音库组件实例化失败，主控版无引擎包支持。已拦截闪退。"))
            speechRecognizer = null
        }
    }

    private fun startContinuousWithDelay() {
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            if (continuousMode && !isListening) {
                startListening()
            }
        }, 800)
    }

    fun startListening() {
        if (speechRecognizer == null) {
            init()
        }
        
        if (speechRecognizer != null && !isListening) {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-CN")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            }
            try {
                speechRecognizer?.startListening(intent)
                isListening = true
                onStateChanged(VoiceState.Listening)
            } catch (e: Exception) {
                onStateChanged(VoiceState.Error("启动失败: ${e.message}"))
                isListening = false
            }
        }
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
        isListening = false
        onStateChanged(VoiceState.Idle)
    }

    fun toggleContinuous(enabled: Boolean) {
        continuousMode = enabled
        if (!enabled) {
            stopListening()
        } else {
            startListening()
        }
    }

    fun destroy() {
        speechRecognizer?.destroy()
        speechRecognizer = null
        isListening = false
    }

    private fun parseCommandAndGetSlot(text: String): Int? {
        val clean = text.replace(" ", "").lowercase()
        if (clean.contains("打开")) {
            val parts = clean.split("打开")
            if (parts.size > 1) {
                val afterOpen = parts[1]
                if (afterOpen.isNotEmpty()) {
                    val firstChar = afterOpen[0].toString()
                    return when {
                        firstChar == "0" || firstChar == "零" -> 0
                        firstChar == "1" || firstChar == "一" -> 1
                        firstChar == "2" || firstChar == "二" -> 2
                        firstChar == "3" || firstChar == "三" -> 3
                        firstChar == "4" || firstChar == "四" -> 4
                        firstChar == "5" || firstChar == "五" -> 5
                        firstChar == "6" || firstChar == "六" -> 6
                        firstChar == "7" || firstChar == "七" -> 7
                        firstChar == "8" || firstChar == "八" -> 8
                        firstChar == "9" || firstChar == "九" -> 9
                        else -> null
                    }
                }
            }
        }
        return null
    }
}
