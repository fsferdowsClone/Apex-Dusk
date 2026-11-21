package com.example.ui

import android.app.Application
import android.view.KeyEvent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.DuskAudioEngine
import com.example.data.PlayerPreferencesRepository
import com.example.game.engine.GameSimulation
import com.example.game.engine.SimulationEvents
import com.example.game.gl.ApexDuskRenderer
import com.example.game.model.BikeLivery
import com.example.game.model.DailyMissionsState
import com.example.game.model.GamePhase
import com.example.game.model.GameUiState
import com.example.game.model.InputState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DuskGameViewModel(application: Application) : AndroidViewModel(application) {

    private val preferencesRepository = PlayerPreferencesRepository(application.applicationContext)
    private val simulation = GameSimulation()
    val audioEngine = DuskAudioEngine()

    private val _uiState = MutableStateFlow(simulation.toUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    @Volatile private var touchInput = InputState()
    @Volatile private var keyInput = InputState()

    private val simulationEvents = object : SimulationEvents {
        override fun onNearMiss(combo: Int, bonusPoints: Int) {
            audioEngine.triggerNearMiss(combo)
        }

        override fun onCrash(finalScore: Int, isNewRecord: Boolean) {
            audioEngine.triggerCrash()
            _uiState.value = simulation.toUiState()
            if (isNewRecord || finalScore > 0) {
                viewModelScope.launch {
                    preferencesRepository.saveHighScoreIfHigher(finalScore)
                }
            }
        }

        override fun onDailyMissionsChanged(missions: DailyMissionsState) {
            viewModelScope.launch {
                preferencesRepository.saveDailyMissions(missions)
            }
        }
    }

    val renderer = ApexDuskRenderer(
        simulation = simulation,
        audioEngine = audioEngine,
        simulationEvents = simulationEvents,
        onUiStateSnapshot = { snapshot ->
            _uiState.value = snapshot
        },
    )

    init {
        audioEngine.start()
        viewModelScope.launch {
            preferencesRepository.settingsFlow.collect { saved ->
                renderer.enqueueAction {
                    simulation.applyPersistedHighScore(saved.highScore)
                    simulation.applyPersistedDailyMissions(saved.dailyMissions)
                    simulation.selectedLivery = saved.livery
                    simulation.isMuted = saved.isMuted
                    audioEngine.setMuted(saved.isMuted)
                    _uiState.value = simulation.toUiState()
                }
            }
        }
    }

    fun startRide() {
        clearInputs()
        renderer.enqueueAction {
            simulation.startNewRun()
            _uiState.value = simulation.toUiState()
        }
    }

    fun pauseGame() {
        clearInputs()
        renderer.enqueueAction {
            simulation.pauseGame()
            val missions = simulation.currentDailyMissions()
            _uiState.value = simulation.toUiState()
            viewModelScope.launch {
                preferencesRepository.saveDailyMissions(missions)
            }
        }
    }

    fun resumeGame() {
        clearInputs()
        renderer.enqueueAction {
            simulation.resumeGame()
            _uiState.value = simulation.toUiState()
        }
    }

    fun returnToGarage() {
        clearInputs()
        renderer.enqueueAction {
            val missions = simulation.currentDailyMissions()
            simulation.returnToMenu()
            _uiState.value = simulation.toUiState()
            viewModelScope.launch {
                preferencesRepository.saveDailyMissions(missions)
            }
        }
    }

    fun selectLivery(livery: BikeLivery) {
        renderer.enqueueAction {
            simulation.selectedLivery = livery
            _uiState.value = simulation.toUiState()
        }
        viewModelScope.launch {
            preferencesRepository.saveSelectedLivery(livery)
        }
    }

    fun toggleMute() {
        val nextMuted = !_uiState.value.isMuted
        audioEngine.setMuted(nextMuted)
        renderer.enqueueAction {
            simulation.isMuted = nextMuted
            _uiState.value = simulation.toUiState()
        }
        viewModelScope.launch {
            preferencesRepository.saveMuted(nextMuted)
        }
    }

    fun updateTouchSteerLeft(pressed: Boolean) {
        touchInput = touchInput.copy(steerLeft = pressed)
        syncCombinedInput()
    }

    fun updateTouchSteerRight(pressed: Boolean) {
        touchInput = touchInput.copy(steerRight = pressed)
        syncCombinedInput()
    }

    fun updateTouchAccelerate(pressed: Boolean) {
        touchInput = touchInput.copy(accelerate = pressed)
        syncCombinedInput()
    }

    fun updateTouchBrake(pressed: Boolean) {
        touchInput = touchInput.copy(brake = pressed)
        syncCombinedInput()
    }

    fun updateTouchNitro(pressed: Boolean) {
        touchInput = touchInput.copy(nitro = pressed)
        syncCombinedInput()
    }

    fun handleHardwareKey(keyCode: Int, isDown: Boolean, repeatCount: Int): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_A, KeyEvent.KEYCODE_DPAD_LEFT -> {
                keyInput = keyInput.copy(steerLeft = isDown)
                syncCombinedInput()
                return true
            }
            KeyEvent.KEYCODE_D, KeyEvent.KEYCODE_DPAD_RIGHT -> {
                keyInput = keyInput.copy(steerRight = isDown)
                syncCombinedInput()
                return true
            }
            KeyEvent.KEYCODE_W, KeyEvent.KEYCODE_DPAD_UP -> {
                keyInput = keyInput.copy(accelerate = isDown)
                syncCombinedInput()
                return true
            }
            KeyEvent.KEYCODE_S, KeyEvent.KEYCODE_DPAD_DOWN -> {
                keyInput = keyInput.copy(brake = isDown)
                syncCombinedInput()
                return true
            }
            KeyEvent.KEYCODE_SPACE, KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.KEYCODE_SHIFT_RIGHT -> {
                keyInput = keyInput.copy(nitro = isDown)
                syncCombinedInput()
                return true
            }
            KeyEvent.KEYCODE_P, KeyEvent.KEYCODE_ESCAPE -> {
                if (isDown && repeatCount == 0) {
                    when (_uiState.value.phase) {
                        GamePhase.PLAYING -> pauseGame()
                        GamePhase.PAUSED -> resumeGame()
                        else -> return false
                    }
                }
                return true
            }
            else -> return false
        }
    }

    fun onLifecyclePause() {
        if (_uiState.value.phase == GamePhase.PLAYING) {
            pauseGame()
        }
        audioEngine.pause()
    }

    fun onLifecycleResume() {
        audioEngine.resume()
    }

    private fun clearInputs() {
        touchInput = InputState()
        keyInput = InputState()
        syncCombinedInput()
    }

    private fun syncCombinedInput() {
        renderer.currentInput = InputState(
            steerLeft = touchInput.steerLeft || keyInput.steerLeft,
            steerRight = touchInput.steerRight || keyInput.steerRight,
            accelerate = touchInput.accelerate || keyInput.accelerate,
            brake = touchInput.brake || keyInput.brake,
            nitro = touchInput.nitro || keyInput.nitro,
        )
    }

    override fun onCleared() {
        super.onCleared()
        audioEngine.release()
    }
}
