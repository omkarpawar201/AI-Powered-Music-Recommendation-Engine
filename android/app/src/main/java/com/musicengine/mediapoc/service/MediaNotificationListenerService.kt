package com.musicengine.mediapoc.service

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.service.notification.NotificationListenerService
import android.util.Log
import com.musicengine.mediapoc.model.PlaybackStateEnum
import com.musicengine.mediapoc.model.PlaybackTelemetryState
import com.musicengine.mediapoc.model.TelemetryEvent
import com.musicengine.mediapoc.model.TelemetryEventType
import com.musicengine.mediapoc.model.TrackMetadata
import com.musicengine.mediapoc.model.UserRating
import com.musicengine.mediapoc.model.formatTime
import kotlinx.coroutines.CoroutineScope
import com.musicengine.mediapoc.repository.MusicDatabaseRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MediaNotificationListenerService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var tickerJob: Job? = null
    private var repository: MusicDatabaseRepository? = null
    private var lastTrackKey: String? = null
    private var mediaSessionManager: MediaSessionManager? = null
    private var activeController: MediaController? = null

    // Telemetry tracking state
    private var currentTrack: TrackMetadata? = null
    private var lastReportedState: PlaybackState? = null
    private var trackStartRealtime: Long = 0L
    private var accumulatedPlayTimeMs: Long = 0L
    private var lastPlayTimestamp: Long = 0L
    private var lastPositionMs: Long = 0L
    private var maxObservedPositionMs: Long = 0L
    private var previousCompletedTrackKey: String? = null

    companion object {
        private const val TAG = "MediaSessionService"

        val TARGET_PACKAGES = setOf(
            "com.apple.android.music",
            "com.google.android.apps.youtube.music",
            "app.morphe.android.apps.youtube.music",
            "app.revanced.android.apps.youtube.music",
            "com.google.android.youtube",
            "com.spotify.music",
            "com.jio.media.jiobeats",
            "com.amazon.mp3"
        )

        private val _nowPlayingFlow = MutableStateFlow<TrackMetadata?>(null)
        val nowPlayingFlow: StateFlow<TrackMetadata?> = _nowPlayingFlow.asStateFlow()

        private val _playbackStateFlow = MutableStateFlow(PlaybackTelemetryState())
        val playbackStateFlow: StateFlow<PlaybackTelemetryState> = _playbackStateFlow.asStateFlow()

        private val _eventsFlow = MutableStateFlow<List<TelemetryEvent>>(emptyList())
        val eventsFlow: StateFlow<List<TelemetryEvent>> = _eventsFlow.asStateFlow()

        private val _activeAppFlow = MutableStateFlow("No Active Music App")
        val activeAppFlow: StateFlow<String> = _activeAppFlow.asStateFlow()

        private val _isServiceConnectedFlow = MutableStateFlow(false)
        val isServiceConnectedFlow: StateFlow<Boolean> = _isServiceConnectedFlow.asStateFlow()

        private val _currentSessionListeningTimeFlow = MutableStateFlow(0L)
        val currentSessionListeningTimeFlow: StateFlow<Long> = _currentSessionListeningTimeFlow.asStateFlow()

        private var instance: MediaNotificationListenerService? = null

        fun clearEventLog() {
            _eventsFlow.value = emptyList()
        }

        fun refreshSessions() {
            instance?.let { service ->
                val component = ComponentName(service, MediaNotificationListenerService::class.java)
                try {
                    val controllers = service.mediaSessionManager?.getActiveSessions(component)
                    service.evaluateControllers(controllers)
                } catch (e: Exception) {
                    Log.e(TAG, "Error refreshing sessions", e)
                }
            }
        }

        // ================= POC 2 Transport Controls =================

        fun play(): Boolean {
            val controller = instance?.activeController ?: return false
            return try {
                controller.transportControls.play()
                instance?.emitTelemetry(TelemetryEventType.RESUMED, "Command: TransportControls.play()")
                true
            } catch (e: Exception) {
                Log.e(TAG, "play() error", e)
                false
            }
        }

        fun pause(): Boolean {
            val controller = instance?.activeController ?: return false
            return try {
                controller.transportControls.pause()
                instance?.emitTelemetry(TelemetryEventType.PAUSED, "Command: TransportControls.pause()")
                true
            } catch (e: Exception) {
                Log.e(TAG, "pause() error", e)
                false
            }
        }

        fun skipToNext(): Boolean {
            val controller = instance?.activeController ?: return false
            return try {
                controller.transportControls.skipToNext()
                instance?.emitTelemetry(TelemetryEventType.SEEKED, "Command: TransportControls.skipToNext()")
                true
            } catch (e: Exception) {
                Log.e(TAG, "skipToNext() error", e)
                false
            }
        }

        fun skipToPrevious(): Boolean {
            val controller = instance?.activeController ?: return false
            return try {
                controller.transportControls.skipToPrevious()
                instance?.emitTelemetry(TelemetryEventType.SEEKED, "Command: TransportControls.skipToPrevious()")
                true
            } catch (e: Exception) {
                Log.e(TAG, "skipToPrevious() error", e)
                false
            }
        }

        fun seekTo(positionMs: Long): Boolean {
            val controller = instance?.activeController ?: return false
            return try {
                controller.transportControls.seekTo(positionMs)
                instance?.emitTelemetry(TelemetryEventType.SEEKED, "Command: TransportControls.seekTo(${positionMs / 1000}s)")
                true
            } catch (e: Exception) {
                Log.e(TAG, "seekTo() error", e)
                false
            }
        }

        fun playFromSearch(query: String, extras: Bundle? = null): Boolean {
            val controller = instance?.activeController ?: return false
            return try {
                controller.transportControls.playFromSearch(query, extras ?: Bundle.EMPTY)
                instance?.emitTelemetry(
                    TelemetryEventType.TRACK_STARTED,
                    "Command: playFromSearch(\"$query\") [extras: ${if (extras != null) "Yes" else "None"}]"
                )
                true
            } catch (e: Exception) {
                Log.e(TAG, "playFromSearch() error", e)
                false
            }
        }

        fun playFromUri(uri: Uri, extras: Bundle? = null): Boolean {
            val controller = instance?.activeController ?: return false
            return try {
                controller.transportControls.playFromUri(uri, extras ?: Bundle.EMPTY)
                instance?.emitTelemetry(
                    TelemetryEventType.TRACK_STARTED,
                    "Command: playFromUri(\"$uri\")"
                )
                true
            } catch (e: Exception) {
                Log.e(TAG, "playFromUri() error", e)
                false
            }
        }

        fun setRating(newRating: UserRating): Boolean {
            val service = instance ?: return false
            val track = service.currentTrack ?: return false
            val updatedTrack = track.copy(userRating = newRating)
            service.currentTrack = updatedTrack
            _nowPlayingFlow.value = updatedTrack

            service.serviceScope.launch(Dispatchers.IO) {
                service.repository?.updateUserRating(track.trackKey, newRating)
            }

            if (newRating == UserRating.LIKED) {
                service.emitTelemetry(
                    TelemetryEventType.USER_LIKE,
                    "Liked: \"${track.title}\" by ${track.artist} (Strong positive signal recorded)"
                )
                service.activeController?.let { controller ->
                    try {
                        controller.transportControls.sendCustomAction("Action: Favourite", null)
                        controller.transportControls.sendCustomAction("Favourite", null)
                        controller.transportControls.sendCustomAction("Like", null)
                    } catch (_: Exception) {}
                }
            } else if (newRating == UserRating.DISLIKED) {
                service.emitTelemetry(
                    TelemetryEventType.USER_DISLIKE,
                    "Disliked: \"${track.title}\" by ${track.artist} (Strong negative signal recorded)"
                )
                service.activeController?.let { controller ->
                    try {
                        controller.transportControls.sendCustomAction("Dislike", null)
                    } catch (_: Exception) {}
                }
            } else {
                service.emitTelemetry(
                    TelemetryEventType.USER_LIKE,
                    "Removed rating for \"${track.title}\""
                )
            }
            return true
        }

        fun toggleLike(): Boolean {
            val track = instance?.currentTrack ?: return false
            val newRating = if (track.userRating == UserRating.LIKED) {
                UserRating.NONE
            } else {
                UserRating.LIKED
            }
            return setRating(newRating)
        }

        fun toggleDislike(): Boolean {
            val track = instance?.currentTrack ?: return false
            val newRating = if (track.userRating == UserRating.DISLIKED) {
                UserRating.NONE
            } else {
                UserRating.DISLIKED
            }
            return setRating(newRating)
        }

        fun getActivePackageName(): String {
            return instance?.activeController?.packageName ?: ""
        }
    }

    private val controllerCallback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) {
            Log.d(TAG, "onMetadataChanged for package: ${activeController?.packageName}")
            handleMetadataChanged(metadata)
        }

        override fun onPlaybackStateChanged(state: PlaybackState?) {
            Log.d(TAG, "onPlaybackStateChanged: state=${state?.state}, pos=${state?.position}")
            handlePlaybackStateChanged(state)
        }

        override fun onSessionDestroyed() {
            Log.d(TAG, "onSessionDestroyed for package: ${activeController?.packageName}")
            handleSessionDestroyed()
        }
    }

    private val sessionsChangedListener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
        Log.d(TAG, "Active sessions changed. Found ${controllers?.size ?: 0} sessions.")
        evaluateControllers(controllers)
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.i(TAG, "NotificationListenerService CONNECTED")
        instance = this
        repository = MusicDatabaseRepository.getInstance(applicationContext)
        _isServiceConnectedFlow.value = true

        mediaSessionManager = getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager
        val component = ComponentName(this, MediaNotificationListenerService::class.java)

        try {
            mediaSessionManager?.addOnActiveSessionsChangedListener(sessionsChangedListener, component)
            val controllers = mediaSessionManager?.getActiveSessions(component)
            evaluateControllers(controllers)
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException getting active sessions: ${e.message}")
        }

        startPositionTicker()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.i(TAG, "NotificationListenerService DISCONNECTED")
        flushCurrentTrackPlayTime()
        if (instance == this) instance = null
        _isServiceConnectedFlow.value = false
        stopPositionTicker()
        activeController?.unregisterCallback(controllerCallback)
        activeController = null
        // Always remove the session listener on disconnect (reconnect re-adds it)
        try {
            mediaSessionManager?.removeOnActiveSessionsChangedListener(sessionsChangedListener)
        } catch (_: Exception) {}
    }

    override fun onDestroy() {
        super.onDestroy()
        flushCurrentTrackPlayTime()
        if (instance == this) instance = null
        stopPositionTicker()
        activeController?.unregisterCallback(controllerCallback)
        try {
            mediaSessionManager?.removeOnActiveSessionsChangedListener(sessionsChangedListener)
        } catch (_: Exception) {}
    }

    override fun onNotificationPosted(sbn: android.service.notification.StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        val pkg = sbn?.packageName ?: return
        if (pkg in TARGET_PACKAGES || pkg.contains("music") || pkg.contains("youtube") || pkg.contains("audio")) {
            Log.d(TAG, "Music notification detected from: $pkg -> refreshing sessions")
            refreshSessions()
        }
    }

    override fun onNotificationRemoved(sbn: android.service.notification.StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        val pkg = sbn?.packageName ?: return
        if (pkg in TARGET_PACKAGES || pkg.contains("music") || pkg.contains("youtube")) {
            refreshSessions()
        }
    }

    private fun evaluateControllers(controllers: List<MediaController>?) {
        if (controllers.isNullOrEmpty()) {
            if (activeController != null) {
                activeController?.unregisterCallback(controllerCallback)
                activeController = null
                _activeAppFlow.value = "No Active App"
                _nowPlayingFlow.value = null
                _playbackStateFlow.value = PlaybackTelemetryState()
            }
            return
        }

        // Tier 1: Target music app actively PLAYING
        val playingTarget = controllers.firstOrNull {
            it.packageName in TARGET_PACKAGES && it.playbackState?.state == PlaybackState.STATE_PLAYING
        }

        // Tier 2: Any music app actively PLAYING
        val anyPlaying = controllers.firstOrNull {
            it.playbackState?.state == PlaybackState.STATE_PLAYING
        }

        // Tier 3: Target music app with valid metadata
        val targetWithMeta = controllers.firstOrNull {
            it.packageName in TARGET_PACKAGES && it.metadata != null
        }

        // Tier 4: Any controller with metadata
        val anyWithMeta = controllers.firstOrNull {
            it.metadata != null
        }

        val candidate = playingTarget
            ?: anyPlaying
            ?: targetWithMeta
            ?: anyWithMeta
            ?: controllers.firstOrNull()

        if (candidate != null) {
            if (candidate.sessionToken != activeController?.sessionToken || activeController == null) {
                bindController(candidate)
            } else {
                // Same session, refresh its state snapshot
                handleMetadataChanged(candidate.metadata)
                handlePlaybackStateChanged(candidate.playbackState)
            }
        }
    }

    private fun bindController(controller: MediaController) {
        activeController?.unregisterCallback(controllerCallback)
        activeController = controller
        controller.registerCallback(controllerCallback)

        val appName = getAppDisplayName(controller.packageName)
        _activeAppFlow.value = appName
        Log.i(TAG, "Bound to MediaController: $appName (${controller.packageName})")

        startPositionTicker()
        handleMetadataChanged(controller.metadata)
        handlePlaybackStateChanged(controller.playbackState)
    }

    private fun handleMetadataChanged(metadata: MediaMetadata?) {
        if (metadata == null) return

        val pkg = activeController?.packageName ?: ""
        val title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE)
            ?: metadata.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
            ?: "Unknown Title"

        val artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: metadata.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
            ?: metadata.getString(MediaMetadata.METADATA_KEY_AUTHOR)
            ?: "Unknown Artist"

        val album = metadata.getString(MediaMetadata.METADATA_KEY_ALBUM) ?: "Unknown Album"
        val duration = metadata.getLong(MediaMetadata.METADATA_KEY_DURATION).coerceAtLeast(0L)
        val genre = metadata.getString(MediaMetadata.METADATA_KEY_GENRE) ?: ""
        val artBitmap = metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: metadata.getBitmap(MediaMetadata.METADATA_KEY_ART)
        val artUriStr = metadata.getString(MediaMetadata.METADATA_KEY_ALBUM_ART_URI)
            ?: metadata.getString(MediaMetadata.METADATA_KEY_ART_URI)
        val artUri = artUriStr?.let { Uri.parse(it) }
        val mediaId = metadata.getString(MediaMetadata.METADATA_KEY_MEDIA_ID)
        val mediaUriStr = metadata.getString(MediaMetadata.METADATA_KEY_MEDIA_URI)
        val mediaUri = mediaUriStr?.let { Uri.parse(it) }

        val newTrack = TrackMetadata(
            title = title,
            artist = artist,
            album = album,
            durationMs = duration,
            genre = genre,
            artBitmap = artBitmap,
            artUri = artUri,
            mediaId = mediaId,
            mediaUri = mediaUri,
            packageName = pkg,
            appDisplayName = getAppDisplayName(pkg)
        )

        // Track transition check
        if (currentTrack == null || currentTrack?.trackKey != newTrack.trackKey) {
            onTrackChanged(oldTrack = currentTrack, newTrack = newTrack)
            currentTrack = newTrack
            _nowPlayingFlow.value = newTrack
        } else {
            // Same track: preserve user rating and update mutable fields only
            val preservedRating = currentTrack?.userRating ?: UserRating.NONE
            val updatedTrack = newTrack.copy(userRating = preservedRating)
            currentTrack = updatedTrack
            _nowPlayingFlow.value = updatedTrack
        }
    }

    private fun handlePlaybackStateChanged(state: PlaybackState?) {
        if (state == null) return
        val prev = lastReportedState
        lastReportedState = state
        val now = SystemClock.elapsedRealtime()

        // 1. Accurately accumulate elapsed playback time whenever we were previously playing
        if (prev?.state == PlaybackState.STATE_PLAYING && lastPlayTimestamp > 0L) {
            val delta = (now - lastPlayTimestamp).coerceAtLeast(0L)
            accumulatedPlayTimeMs += delta
        }

        // 2. State transition telemetry & seek detection
        if (prev != null && currentTrack != null) {
            if (prev.state != PlaybackState.STATE_PLAYING && state.state == PlaybackState.STATE_PLAYING) {
                emitTelemetry(TelemetryEventType.RESUMED, "Resumed playback at ${formatTime(state.position)}")
            } else if (prev.state == PlaybackState.STATE_PLAYING && state.state != PlaybackState.STATE_PLAYING) {
                emitTelemetry(TelemetryEventType.PAUSED, "Paused playback at ${formatTime(state.position)}")
            }

            // Seek detection (jump > 3.5 seconds) - ONLY if both states are actively PLAYING
            if (prev.state == PlaybackState.STATE_PLAYING && state.state == PlaybackState.STATE_PLAYING) {
                val expectedPos = lastPositionMs + (now - lastPlayTimestamp)
                val posJump = kotlin.math.abs(state.position - expectedPos)
                if (posJump > 3500L) {
                    emitTelemetry(
                        TelemetryEventType.SEEKED,
                        "Seeked from ${formatTime(lastPositionMs)} to ${formatTime(state.position)}"
                    )
                }
            }
        }

        // 3. Update play timestamp and positions
        lastPlayTimestamp = if (state.state == PlaybackState.STATE_PLAYING) now else 0L
        lastPositionMs = state.position

        val trackDuration = currentTrack?.durationMs ?: 0L
        if (trackDuration > 0L && state.position in 1..trackDuration) {
            if (state.position > maxObservedPositionMs) {
                maxObservedPositionMs = state.position
            }
        } else if (trackDuration == 0L && state.position > maxObservedPositionMs) {
            maxObservedPositionMs = state.position
        }

        updatePlaybackStateSnapshot(state)
    }

    private fun onTrackChanged(oldTrack: TrackMetadata?, newTrack: TrackMetadata) {
        val now = SystemClock.elapsedRealtime()
        if (lastPlayTimestamp > 0L) {
            accumulatedPlayTimeMs += (now - lastPlayTimestamp).coerceAtLeast(0L)
            lastPlayTimestamp = 0L
        }

        if (oldTrack != null) {
            val duration = oldTrack.durationMs
            val timeRatio = if (duration > 0L) (accumulatedPlayTimeMs.toFloat() / duration.toFloat()) else 0f
            val posRatio = if (duration > 0L) (maxObservedPositionMs.toFloat() / duration.toFloat()) else 0f
            val effectiveRatio = maxOf(timeRatio, posRatio).coerceAtLeast(0f)
            val completionPct = (effectiveRatio * 100f).coerceAtMost(100f)
            val prevKey = lastTrackKey

            // Natural completion criteria:
            // 1. Position reached within 10s of duration or >= 85% of track
            // 2. OR Total listened time >= 80% of track
            // 3. OR listened >= 45s if track duration is unknown (0L)
            val isNaturalCompletion = if (duration > 0L) {
                maxObservedPositionMs >= (duration - 10_000L).coerceAtLeast(0L) ||
                effectiveRatio >= 0.85f ||
                accumulatedPlayTimeMs >= (duration * 0.80f).toLong()
            } else {
                accumulatedPlayTimeMs >= 45_000L
            }

            // Replay detection
            if (newTrack.trackKey == previousCompletedTrackKey) {
                emitTelemetry(
                    TelemetryEventType.REPLAY,
                    "Replayed ${newTrack.title} after completion",
                    track = oldTrack,
                    listenedMs = accumulatedPlayTimeMs,
                    pct = completionPct
                )
                serviceScope.launch(Dispatchers.IO) {
                    repository?.incrementReplay(newTrack.trackKey)
                    repository?.logPlayEvent(
                        trackKey = oldTrack.trackKey,
                        startedAt = trackStartRealtime,
                        durationListenedMs = accumulatedPlayTimeMs,
                        completionRatio = effectiveRatio,
                        eventType = TelemetryEventType.REPLAY,
                        previousTrackKey = prevKey
                    )
                    prevKey?.let { prev ->
                        repository?.recordTransitionEvent(prev, newTrack.trackKey, isSuccess = true, isEarlySkip = false, isLateSkip = false)
                    }
                }
            } else if (isNaturalCompletion) {
                previousCompletedTrackKey = oldTrack.trackKey
                emitTelemetry(
                    TelemetryEventType.NATURAL_COMPLETION,
                    "Completed ${oldTrack.title} (listened ${formatTime(accumulatedPlayTimeMs)} / ${formatTime(oldTrack.durationMs)})",
                    track = oldTrack,
                    listenedMs = accumulatedPlayTimeMs,
                    pct = completionPct
                )
                serviceScope.launch(Dispatchers.IO) {
                    repository?.incrementCompletion(oldTrack.trackKey)
                    repository?.logPlayEvent(
                        trackKey = oldTrack.trackKey,
                        startedAt = trackStartRealtime,
                        durationListenedMs = accumulatedPlayTimeMs,
                        completionRatio = effectiveRatio,
                        eventType = TelemetryEventType.NATURAL_COMPLETION,
                        previousTrackKey = prevKey
                    )
                    prevKey?.let { prev ->
                        repository?.recordTransitionEvent(prev, oldTrack.trackKey, isSuccess = true, isEarlySkip = false, isLateSkip = false)
                    }
                }
            } else if (accumulatedPlayTimeMs < 20_000L && maxObservedPositionMs < 20_000L) {
                emitTelemetry(
                    TelemetryEventType.SKIP_EARLY,
                    "Early skip on ${oldTrack.title} after only ${formatTime(accumulatedPlayTimeMs)} (${String.format("%.1f", completionPct)}%)",
                    track = oldTrack,
                    listenedMs = accumulatedPlayTimeMs,
                    pct = completionPct
                )
                serviceScope.launch(Dispatchers.IO) {
                    repository?.incrementEarlySkip(oldTrack.trackKey)
                    repository?.applySkipPenalty(oldTrack.trackKey, initialPenalty = 40.0f, halfLifeHours = 4.0f)
                    repository?.logPlayEvent(
                        trackKey = oldTrack.trackKey,
                        startedAt = trackStartRealtime,
                        durationListenedMs = accumulatedPlayTimeMs,
                        completionRatio = effectiveRatio,
                        eventType = TelemetryEventType.SKIP_EARLY,
                        previousTrackKey = prevKey
                    )
                    prevKey?.let { prev ->
                        repository?.recordTransitionEvent(prev, oldTrack.trackKey, isSuccess = false, isEarlySkip = true, isLateSkip = false)
                    }
                }
            } else {
                emitTelemetry(
                    TelemetryEventType.SKIP_LATE,
                    "Late skip on ${oldTrack.title} at ${formatTime(accumulatedPlayTimeMs)} (${String.format("%.1f", completionPct)}%)",
                    track = oldTrack,
                    listenedMs = accumulatedPlayTimeMs,
                    pct = completionPct
                )
                serviceScope.launch(Dispatchers.IO) {
                    repository?.incrementLateSkip(oldTrack.trackKey)
                    repository?.applySkipPenalty(oldTrack.trackKey, initialPenalty = 15.0f, halfLifeHours = 2.0f)
                    repository?.logPlayEvent(
                        trackKey = oldTrack.trackKey,
                        startedAt = trackStartRealtime,
                        durationListenedMs = accumulatedPlayTimeMs,
                        completionRatio = effectiveRatio,
                        eventType = TelemetryEventType.SKIP_LATE,
                        previousTrackKey = prevKey
                    )
                    prevKey?.let { prev ->
                        repository?.recordTransitionEvent(prev, oldTrack.trackKey, isSuccess = false, isEarlySkip = false, isLateSkip = true)
                    }
                }
            }
        }

        // Reset for the new track
        trackStartRealtime = now
        accumulatedPlayTimeMs = 0L
        lastPlayTimestamp = if (lastReportedState?.state == PlaybackState.STATE_PLAYING) now else 0L
        lastPositionMs = 0L
        maxObservedPositionMs = 0L

        emitTelemetry(
            TelemetryEventType.TRACK_STARTED,
            "Started: ${newTrack.title} by ${newTrack.artist} (${newTrack.appDisplayName})",
            track = newTrack
        )

        // Upsert new track in Room and restore any existing user rating
        serviceScope.launch(Dispatchers.IO) {
            val saved = repository?.recordTrackStart(newTrack)
            if (saved != null && saved.userRating != UserRating.NONE) {
                val updated = currentTrack?.copy(userRating = saved.userRating)
                currentTrack = updated
                _nowPlayingFlow.value = updated
            }
        }
        lastTrackKey = oldTrack?.trackKey
    }

    private fun flushCurrentTrackPlayTime() {
        val oldTrack = currentTrack ?: return
        val now = SystemClock.elapsedRealtime()
        if (lastPlayTimestamp > 0L) {
            accumulatedPlayTimeMs += (now - lastPlayTimestamp).coerceAtLeast(0L)
            lastPlayTimestamp = 0L
        }
        val playDuration = accumulatedPlayTimeMs
        if (playDuration >= 2000L) {
            val duration = oldTrack.durationMs
            val ratio = if (duration > 0L) (playDuration.toFloat() / duration.toFloat()).coerceIn(0f, 1f) else 0f
            val prevKey = lastTrackKey
            val startRt = trackStartRealtime
            serviceScope.launch(Dispatchers.IO) {
                repository?.logPlayEvent(
                    trackKey = oldTrack.trackKey,
                    startedAt = startRt,
                    durationListenedMs = playDuration,
                    completionRatio = ratio,
                    eventType = if (ratio >= 0.85f) TelemetryEventType.NATURAL_COMPLETION else TelemetryEventType.PAUSED,
                    previousTrackKey = prevKey
                )
            }
        }
        accumulatedPlayTimeMs = 0L
    }

    private fun handleSessionDestroyed() {
        val app = getAppDisplayName(activeController?.packageName ?: "")
        emitTelemetry(TelemetryEventType.PAUSED, "Session destroyed for $app")
        flushCurrentTrackPlayTime()
        currentTrack = null
        _currentSessionListeningTimeFlow.value = 0L
        _nowPlayingFlow.value = null
        _playbackStateFlow.value = PlaybackTelemetryState()
        _activeAppFlow.value = "No Active App"
        activeController = null
        stopPositionTicker()
    }

    private fun startPositionTicker() {
        tickerJob?.cancel()
        tickerJob = serviceScope.launch {
            while (isActive) {
                activeController?.playbackState?.let { state ->
                    updatePlaybackStateSnapshot(state)
                    val duration = currentTrack?.durationMs ?: 0L
                    if (state.state == PlaybackState.STATE_PLAYING) {
                        val delta = SystemClock.elapsedRealtime() - state.lastPositionUpdateTime
                        val livePos = (state.position + (delta * state.playbackSpeed).toLong())
                            .coerceIn(0L, if (duration > 0) duration else Long.MAX_VALUE)
                        if (livePos > maxObservedPositionMs) {
                            maxObservedPositionMs = livePos
                        }

                        val now = SystemClock.elapsedRealtime()
                        val liveDelta = if (lastPlayTimestamp > 0L) (now - lastPlayTimestamp).coerceAtLeast(0L) else 0L
                        _currentSessionListeningTimeFlow.value = accumulatedPlayTimeMs + liveDelta
                    } else {
                        _currentSessionListeningTimeFlow.value = accumulatedPlayTimeMs
                    }
                } ?: run {
                    _currentSessionListeningTimeFlow.value = 0L
                }
                delay(500)
            }
        }
    }

    private fun stopPositionTicker() {
        tickerJob?.cancel()
        tickerJob = null
        _currentSessionListeningTimeFlow.value = 0L
    }

    private fun updatePlaybackStateSnapshot(state: PlaybackState) {
        val duration = currentTrack?.durationMs ?: 0L
        var livePos = state.position

        if (state.state == PlaybackState.STATE_PLAYING) {
            val delta = SystemClock.elapsedRealtime() - state.lastPositionUpdateTime
            livePos += (delta * state.playbackSpeed).toLong()
        }

        livePos = livePos.coerceIn(0L, if (duration > 0) duration else Long.MAX_VALUE)
        val ratio = if (duration > 0) (livePos.toFloat() / duration.toFloat()).coerceIn(0f, 1f) else 0f

        val stateEnum = when (state.state) {
            PlaybackState.STATE_PLAYING -> PlaybackStateEnum.PLAYING
            PlaybackState.STATE_PAUSED -> PlaybackStateEnum.PAUSED
            PlaybackState.STATE_BUFFERING -> PlaybackStateEnum.BUFFERING
            PlaybackState.STATE_NONE -> PlaybackStateEnum.NONE
            else -> PlaybackStateEnum.STOPPED
        }

        _playbackStateFlow.value = PlaybackTelemetryState(
            positionMs = livePos,
            durationMs = duration,
            state = stateEnum,
            speed = state.playbackSpeed,
            completionRatio = ratio
        )
    }

    private fun emitTelemetry(
        type: TelemetryEventType,
        desc: String,
        track: TrackMetadata? = currentTrack,
        listenedMs: Long = accumulatedPlayTimeMs,
        pct: Float = 0f
    ) {
        val formatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val timeStr = formatter.format(Date())

        val event = TelemetryEvent(
            timestamp = timeStr,
            type = type,
            trackTitle = track?.title ?: "Unknown",
            artist = track?.artist ?: "Unknown",
            listenedDurationMs = listenedMs,
            completionPercentage = pct,
            description = desc
        )

        Log.i(TAG, "[Telemetry] [${event.type}] $desc")
        _eventsFlow.value = listOf(event) + _eventsFlow.value.take(49)
    }

    private fun getAppDisplayName(packageName: String): String {
        return when {
            packageName.contains("apple", ignoreCase = true) -> "Apple Music"
            packageName.contains("youtube.music", ignoreCase = true) -> "YouTube Music"
            packageName.contains("youtube", ignoreCase = true) -> "YouTube"
            packageName.contains("spotify", ignoreCase = true) -> "Spotify"
            packageName.contains("jio", ignoreCase = true) -> "JioSaavn"
            else -> packageName.substringAfterLast('.').replaceFirstChar { it.uppercase() }
        }
    }
}
