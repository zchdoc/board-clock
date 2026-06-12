package com.example.utils

sealed class VoiceState {
    object Idle : VoiceState()
    object Ready : VoiceState()
    object Listening : VoiceState()
    data class PartialText(val text: String) : VoiceState()
    data class Success(val matchValue: String) : VoiceState()
    data class Error(val message: String) : VoiceState()
}
