package com.otokana.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.otokana.core.Phase

private enum class Screen { Setup, Play, Results, Settings, Import }

/** The screen of the session's phase, or Settings or Import over it. */
@Composable
fun OtoKanaApp(vm: AppViewModel = viewModel()) {
    val screen = when {
        vm.play.phase == Phase.Playing -> Screen.Play
        vm.overlay == Overlay.Settings -> Screen.Settings
        vm.overlay == Overlay.Import -> Screen.Import
        vm.play.phase == Phase.Results -> Screen.Results
        else -> Screen.Setup
    }
    BackHandler(screen != Screen.Setup) {
        when (screen) {
            Screen.Play -> vm.stop()
            Screen.Settings, Screen.Import -> vm.overlay = Overlay.None
            Screen.Results -> vm.playAgain()
            Screen.Setup -> {}
        }
    }
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        AnimatedContent(screen, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "screen") {
            when (it) {
                Screen.Setup -> SetupScreen(vm)
                Screen.Play -> PlayScreen(vm)
                Screen.Results -> ResultsScreen(vm)
                Screen.Settings -> SettingsScreen(vm)
                Screen.Import -> ImportScreen(vm)
            }
        }
    }
}
