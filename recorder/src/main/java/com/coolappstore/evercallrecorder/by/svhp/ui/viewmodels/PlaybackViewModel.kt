package com.coolappstore.evercallrecorder.by.svhp.ui.viewmodels

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class PlaybackViewModel(application: Application) : AndroidViewModel(application) {

    private val notesPrefs = application.getSharedPreferences("recording_notes", Context.MODE_PRIVATE)

    val player: ExoPlayer = ExoPlayer.Builder(application).build()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration

    private val _note = MutableStateFlow("")
    val note: StateFlow<String> = _note

    private var progressJob: Job? = null
    private var currentUri: Uri? = null

    init {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                _isPlaying.value = playing
                if (playing) startProgressTracking() else progressJob?.cancel()
            }
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) {
                    _duration.value = player.duration.coerceAtLeast(0L)
                }
            }
        })
    }

    fun load(uri: Uri) {
        if (uri == currentUri) return
        currentUri = uri
        player.setMediaItem(MediaItem.fromUri(uri))
        player.prepare()
        _currentPosition.value = 0L
        _duration.value = 0L
        _note.value = notesPrefs.getString(uri.toString(), "") ?: ""
    }

    fun togglePlayPause() {
        if (player.isPlaying) player.pause() else player.play()
    }

    fun seekForward() { player.seekTo((player.currentPosition + 5_000).coerceAtMost(player.duration)) }
    fun seekBack() { player.seekTo((player.currentPosition - 5_000).coerceAtLeast(0)) }

    /** Seek to position and immediately update the displayed position so the slider stays put when paused. */
    fun seekTo(ms: Long) {
        player.seekTo(ms)
        val dur = _duration.value
        _currentPosition.value = if (dur > 0) ms.coerceIn(0L, dur) else ms.coerceAtLeast(0L)
    }

    fun updateNote(text: String) {
        _note.value = text
        currentUri?.let { notesPrefs.edit().putString(it.toString(), text).apply() }
    }

    /** Called when leaving the playback screen — pauses, clears buffered media and resets state.
     *  The player instance itself is kept alive (reused on next visit); it is only fully
     *  released in [onCleared] when the ViewModel is destroyed. */
    fun resetOnLeave() {
        progressJob?.cancel()
        player.pause()
        player.clearMediaItems()
        currentUri = null
        _isPlaying.value = false
        _currentPosition.value = 0L
        _duration.value = 0L
    }

    private fun startProgressTracking() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (isActive) {
                _currentPosition.value = player.currentPosition.coerceAtLeast(0L)
                if (player.duration > 0) _duration.value = player.duration
                delay(200)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        player.release()
    }
}
