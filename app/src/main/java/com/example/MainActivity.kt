package com.example

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.ui.DuskGameViewModel
import com.example.ui.GameScreen
import com.example.ui.theme.ApexDuskTheme

class MainActivity : ComponentActivity() {

    private val gameViewModel: DuskGameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        setContent {
            ApexDuskTheme {
                GameScreen(viewModel = gameViewModel)
            }
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val isDown = event.action == KeyEvent.ACTION_DOWN
        val isUp = event.action == KeyEvent.ACTION_UP
        if (isDown || isUp) {
            val handled = gameViewModel.handleHardwareKey(
                keyCode = event.keyCode,
                isDown = isDown,
                repeatCount = event.repeatCount,
            )
            if (handled) return true
        }
        return super.dispatchKeyEvent(event)
    }
}
