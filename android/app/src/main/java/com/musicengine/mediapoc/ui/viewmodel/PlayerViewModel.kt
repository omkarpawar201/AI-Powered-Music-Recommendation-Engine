package com.musicengine.mediapoc.ui.viewmodel

import android.app.Application
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.musicengine.mediapoc.model.PlaybackTelemetryState
import com.musicengine.mediapoc.model.TelemetryEvent
import com.musicengine.mediapoc.model.TrackMetadata
import com.musicengine.mediapoc.service.MediaNotificationListenerService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.musicengine.mediapoc.db.entity.SkipPenaltyEntity
import com.musicengine.mediapoc.db.entity.TrackEntity
import com.musicengine.mediapoc.db.entity.TransitionEntity
import com.musicengine.mediapoc.repository.MusicDatabaseRepository

class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    val nowPlaying: StateFlow<TrackMetadata?> = MediaNotificationListenerService.nowPlayingFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val playbackState: StateFlow<PlaybackTelemetryState> = MediaNotificationListenerService.playbackStateFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PlaybackTelemetryState())

    val events: StateFlow<List<TelemetryEvent>> = MediaNotificationListenerService.eventsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeApp: StateFlow<String> = MediaNotificationListenerService.activeAppFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "No Active Music App")

    val isServiceConnected: StateFlow<Boolean> = MediaNotificationListenerService.isServiceConnectedFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _isMediaPermissionGranted = MutableStateFlow(checkNotificationListenerPermission())
    val isMediaPermissionGranted: StateFlow<Boolean> = _isMediaPermissionGranted.asStateFlow()

    private val _isBatteryOptimized = MutableStateFlow(!isIgnoringBatteryOptimizations())
    val isBatteryOptimized: StateFlow<Boolean> = _isBatteryOptimized.asStateFlow()

    private val repository = MusicDatabaseRepository.getInstance(application)

    val topTracks: StateFlow<List<TrackEntity>> = repository.getTopTracksFlow(50)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalTrackCount: StateFlow<Int> = repository.getTotalTrackCountFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val transitions: StateFlow<List<TransitionEntity>> = repository.getAllTransitionsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activePenalties: StateFlow<List<SkipPenaltyEntity>> = repository.getAllPenaltiesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun calculateEffectivePenalty(penalty: SkipPenaltyEntity): Float {
        return repository.calculateEffectivePenalty(
            initialPenalty = penalty.initialPenalty,
            skipTimestamp = penalty.skipTimestamp,
            halfLifeHours = penalty.halfLifeHours
        )
    }

    fun refreshPermissions() {
        _isMediaPermissionGranted.value = checkNotificationListenerPermission()
        _isBatteryOptimized.value = !isIgnoringBatteryOptimizations()
        if (_isMediaPermissionGranted.value) {
            MediaNotificationListenerService.refreshSessions()
        }
    }

    fun refreshSessions() {
        if (_isMediaPermissionGranted.value) {
            MediaNotificationListenerService.refreshSessions()
        }
    }

    fun clearEventLog() {
        MediaNotificationListenerService.clearEventLog()
    }

    fun requestBatteryOptimizationExemption() {
        val context = getApplication<Application>()
        try {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                context.startActivity(intent)
            } catch (_: Exception) {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
                context.startActivity(intent)
            }
        }
    }

    fun openNotificationListenerSettings() {
        val context = getApplication<Application>()
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        context.startActivity(intent)
    }

    private fun checkNotificationListenerPermission(): Boolean {
        val context = getApplication<Application>()
        val pkgName = context.packageName
        val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        return flat != null && flat.contains(pkgName)
    }

    private fun isIgnoringBatteryOptimizations(): Boolean {
        val context = getApplication<Application>()
        val powerManager = context.getSystemService(Application.POWER_SERVICE) as? PowerManager
        return powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: true
    }
}
