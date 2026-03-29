package dev.plexus.shared.features.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.screen.Screen
import dev.plexus.shared.core.platform.PlatformPreferences
import dev.plexus.shared.core.platform.PlatformPrefsDefaults
import dev.plexus.shared.core.platform.PlatformPrefsKeys
import dev.plexus.shared.features.terminal.agentlist.AgentListScreen
import dev.plexus.shared.features.terminal.session.TerminalScreen
import org.koin.compose.koinInject

/**
 * ターミナルナビゲーション画面
 *
 * セッション一覧とターミナルセッションの2画面を切り替えるルート画面。
 */
class TerminalNavigationScreen : Screen {
    @Composable
    override fun Content() {
        var activeView by rememberSaveable { mutableStateOf(MainView.Terminal) }

        val preferences = koinInject<PlatformPreferences>()

        val agentListScreen =
            remember {
                AgentListScreen(
                    onSessionSelected = {
                        activeView = MainView.TerminalSession
                    },
                )
            }

        MainNavigationHost(
            activeView = activeView,
            onSwipeToTerminal = {
                activeView = MainView.Terminal
            },
        ) { targetView ->
            when (targetView) {
                MainView.Terminal -> agentListScreen.Content()
                MainView.TerminalSession -> {
                    val lastSessionId =
                        preferences.getString(
                            PlatformPrefsKeys.KEY_LAST_TERMINAL_SESSION,
                            PlatformPrefsDefaults.DEFAULT_LAST_TERMINAL_SESSION,
                        )
                    if (lastSessionId.isNotBlank()) {
                        val terminalScreen =
                            remember(lastSessionId) {
                                TerminalScreen(
                                    agentId = lastSessionId,
                                    onClose = { activeView = MainView.Terminal },
                                )
                            }
                        terminalScreen.Content()
                    } else {
                        activeView = MainView.Terminal
                    }
                }
            }
        }
    }
}
