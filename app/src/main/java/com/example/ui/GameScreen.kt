package com.example.ui

import android.opengl.GLSurfaceView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.game.model.GamePhase

@Composable
fun GameScreen(
    viewModel: DuskGameViewModel,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var showAboutModal by remember { mutableStateOf(false) }

    val glSurfaceView = remember(context) {
        GLSurfaceView(context).apply {
            setEGLContextClientVersion(2)
            preserveEGLContextOnPause = true
            setRenderer(viewModel.renderer)
            renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
        }
    }

    DisposableEffect(lifecycleOwner, glSurfaceView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    glSurfaceView.onResume()
                    viewModel.onLifecycleResume()
                }
                Lifecycle.Event.ON_PAUSE -> {
                    viewModel.onLifecyclePause()
                    glSurfaceView.onPause()
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // System back button navigation handling
    BackHandler(enabled = showAboutModal || uiState.phase != GamePhase.MENU) {
        if (showAboutModal) {
            showAboutModal = false
        } else {
            when (uiState.phase) {
                GamePhase.PLAYING -> viewModel.pauseGame()
                GamePhase.PAUSED -> viewModel.resumeGame()
                GamePhase.GAME_OVER -> viewModel.returnToGarage()
                GamePhase.MENU -> Unit
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { glSurfaceView },
            modifier = Modifier.fillMaxSize(),
        )

        when (uiState.phase) {
            GamePhase.MENU -> {
                StartScreenOverlay(
                    state = uiState,
                    onRideNow = {
                        showAboutModal = false
                        viewModel.startRide()
                    },
                    onSelectLivery = viewModel::selectLivery,
                    onToggleMute = viewModel::toggleMute,
                    onOpenAbout = { showAboutModal = true },
                )
                if (showAboutModal) {
                    AboutPrivacyModal(
                        onDismiss = { showAboutModal = false },
                    )
                }
            }
            GamePhase.PLAYING -> {
                InGameHud(
                    state = uiState,
                    onPauseClick = viewModel::pauseGame,
                    onToggleMute = viewModel::toggleMute,
                    onSteerLeft = viewModel::updateTouchSteerLeft,
                    onSteerRight = viewModel::updateTouchSteerRight,
                    onAccelerate = viewModel::updateTouchAccelerate,
                    onBrake = viewModel::updateTouchBrake,
                    onNitro = viewModel::updateTouchNitro,
                )
            }
            GamePhase.PAUSED -> {
                InGameHud(
                    state = uiState,
                    onPauseClick = viewModel::resumeGame,
                    onToggleMute = viewModel::toggleMute,
                    onSteerLeft = {},
                    onSteerRight = {},
                    onAccelerate = {},
                    onBrake = {},
                    onNitro = {},
                )
                PauseModal(
                    state = uiState,
                    onResume = viewModel::resumeGame,
                    onRestart = viewModel::startRide,
                    onGarage = viewModel::returnToGarage,
                    onToggleMute = viewModel::toggleMute,
                )
            }
            GamePhase.GAME_OVER -> {
                GameOverModal(
                    state = uiState,
                    onPlayAgain = viewModel::startRide,
                    onGarage = viewModel::returnToGarage,
                )
            }
        }
    }
}
