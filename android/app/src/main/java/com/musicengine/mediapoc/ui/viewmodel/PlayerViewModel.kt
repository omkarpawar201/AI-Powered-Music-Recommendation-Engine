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
import com.musicengine.mediapoc.model.UserRating
import com.musicengine.mediapoc.service.MediaNotificationListenerService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.musicengine.mediapoc.db.entity.SkipPenaltyEntity
import com.musicengine.mediapoc.db.entity.TrackEntity
import com.musicengine.mediapoc.db.entity.TransitionEntity
import com.musicengine.mediapoc.repository.MusicDatabaseRepository
import android.util.Log
import com.musicengine.mediapoc.model.CandidateTrack
import com.musicengine.mediapoc.model.RecommendationResult
import com.musicengine.mediapoc.recommendation.RecommendationEngine

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

    val likedTracks: StateFlow<List<TrackEntity>> = repository.getLikedTracksFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalTrackCount: StateFlow<Int> = repository.getTotalTrackCountFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalListeningTime: StateFlow<Long> = combine(
        repository.getTotalListeningTimeFlow(),
        MediaNotificationListenerService.currentSessionListeningTimeFlow
    ) { dbTotal, liveSessionMs ->
        dbTotal + liveSessionMs
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val totalListeningTimeFormatted: StateFlow<String> = totalListeningTime
        .map { ms ->
            val totalSecs = ms / 1000
            val hours = totalSecs / 3600
            val mins = (totalSecs % 3600) / 60
            val secs = totalSecs % 60
            when {
                hours > 0 -> "${hours}h ${mins}m"
                mins > 0 -> "${mins}m"
                secs > 0 -> "${secs}s"
                else -> "0m"
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "0m")

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

    private val recommendationEngine = RecommendationEngine.getInstance(application)
    private val catalogClient = com.musicengine.mediapoc.network.ITunesCatalogClient.getInstance()

    private val _recommendationResult = MutableStateFlow<RecommendationResult?>(null)
    val recommendationResult: StateFlow<RecommendationResult?> = _recommendationResult.asStateFlow()

    val upNextTrack: StateFlow<CandidateTrack?> = _recommendationResult
        .map { it?.topCandidate?.track }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _isRecommending = MutableStateFlow(false)
    val isRecommending: StateFlow<Boolean> = _isRecommending.asStateFlow()

    private val _recommendationError = MutableStateFlow<String?>(null)
    val recommendationError: StateFlow<String?> = _recommendationError.asStateFlow()

    // ─── Unified Search ─────────────────────────────────────────────────────
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _searchResults = MutableStateFlow<List<CandidateTrack>>(emptyList())
    val searchResults: StateFlow<List<CandidateTrack>> = _searchResults.asStateFlow()

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }
        viewModelScope.launch {
            _isSearching.value = true
            try {
                val localMatches = repository.searchTracks(query).map { entity ->
                    CandidateTrack(
                        title = entity.title,
                        artist = entity.artist,
                        album = entity.album,
                        durationMs = entity.durationMs,
                        artworkUri = entity.artworkUri,
                        source = com.musicengine.mediapoc.model.CandidateSource.PERSONAL_LIBRARY
                    )
                }
                val catalogMatches = catalogClient.searchRelatedTracks(query, limit = 15)
                val combined = (localMatches + catalogMatches).distinctBy { it.trackKey }
                _searchResults.value = combined
            } catch (e: Exception) {
                Log.e("PlayerViewModel", "Search error: ${e.message}")
            } finally {
                _isSearching.value = false
            }
        }
    }

    fun generateRecommendations() {
        val current = nowPlaying.value ?: return
        viewModelScope.launch {
            _isRecommending.value = true
            _recommendationError.value = null
            try {
                val result = recommendationEngine.getRecommendations(current, limit = 15)
                if (result.rankedCandidates.isEmpty()) {
                    _recommendationResult.value = null
                    _recommendationError.value = "No recommendations found. Keep listening to build up your library, then try again."
                } else {
                    _recommendationResult.value = result
                }
            } catch (e: Exception) {
                Log.e("PlayerViewModel", "Recommendation error: ${e.message}", e)
                _recommendationResult.value = null
                _recommendationError.value = "Could not generate recommendations. ${e.message ?: "Unknown error"}"
            } finally {
                _isRecommending.value = false
            }
        }
    }

    fun clearRecommendationError() {
        _recommendationError.value = null
        _recommendationResult.value = null
    }

    fun playCandidate(candidate: CandidateTrack): Boolean {
        return recommendationEngine.playCandidate(candidate)
    }

    fun playTrack(track: TrackEntity): Boolean {
        return playCandidate(
            CandidateTrack(
                title = track.title,
                artist = track.artist,
                album = track.album,
                durationMs = track.durationMs,
                artworkUri = track.artworkUri,
                source = com.musicengine.mediapoc.model.CandidateSource.PERSONAL_LIBRARY
            )
        )
    }

    fun rateTrack(rating: UserRating) {
        val handled = MediaNotificationListenerService.setRating(rating)
        if (!handled) {
            val current = nowPlaying.value ?: return
            viewModelScope.launch {
                repository.updateUserRating(current.trackKey, rating)
            }
        }
    }

    fun rateSpecificTrack(trackKey: String, rating: UserRating) {
        if (nowPlaying.value?.trackKey == trackKey) {
            MediaNotificationListenerService.setRating(rating)
        } else {
            viewModelScope.launch {
                repository.updateUserRating(trackKey, rating)
            }
        }
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
