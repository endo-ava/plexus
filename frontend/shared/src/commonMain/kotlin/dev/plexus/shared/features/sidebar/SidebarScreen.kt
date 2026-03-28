package dev.plexus.shared.features.sidebar

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import cafe.adriel.voyager.core.screen.Screen
import dev.plexus.shared.core.platform.PlatformPreferences
import dev.plexus.shared.core.platform.PlatformPrefsDefaults
import dev.plexus.shared.core.platform.PlatformPrefsKeys
import dev.plexus.shared.core.ui.theme.PlexusThemeTokens
import dev.plexus.shared.features.navigation.MainNavigationHost
import dev.plexus.shared.features.navigation.MainView
import dev.plexus.shared.features.settings.SettingsScreen
import dev.plexus.shared.features.systemprompt.SystemPromptEditorScreen
import dev.plexus.shared.features.terminal.agentlist.AgentListScreen
import dev.plexus.shared.features.terminal.session.TerminalScreen
import dev.plexus.shared.features.terminal.settings.GatewaySettingsScreen
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * サイドバー画面
 *
 * ターミナル中心のナビゲーションと画面コンテンツを管理するメイン画面。
 */
class SidebarScreen : Screen {
    @Composable
    override fun Content() {
        val dimens = PlexusThemeTokens.dimens
        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        val scope = rememberCoroutineScope()
        var activeView by rememberSaveable { mutableStateOf(MainView.Terminal) }
        val keyboardController = LocalSoftwareKeyboardController.current
        val focusManager = LocalFocusManager.current

        val dismissKeyboard = {
            keyboardController?.hide()
            focusManager.clearFocus(force = true)
        }

        LaunchedEffect(drawerState) {
            snapshotFlow { drawerState.targetValue }
                .collect { targetValue ->
                    if (targetValue == DrawerValue.Open) {
                        dismissKeyboard()
                    }
                }
        }

        val preferences = koinInject<PlatformPreferences>()

        val agentListScreen =
            remember {
                AgentListScreen(
                    onSessionSelected = {
                        activeView = MainView.TerminalSession
                    },
                    onOpenGatewaySettings = {
                        activeView = MainView.GatewaySettings
                    },
                )
            }

        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet {
                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = dimens.space16, vertical = dimens.space12),
                    ) {
                        Text(
                            text = "Plexus",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(dimens.space4))
                        Text(
                            text = "tmux-centered mobile terminal runtime",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    HorizontalDivider()

                    Spacer(modifier = Modifier.weight(1f))

                    SidebarFooter(
                        onSettingsClick = {
                            activeView = MainView.Settings
                            scope.launch { drawerState.close() }
                        },
                        onTerminalClick = {
                            activeView = MainView.Terminal
                            scope.launch { drawerState.close() }
                        },
                        onSystemPromptClick = {
                            activeView = MainView.SystemPrompt
                            scope.launch { drawerState.close() }
                        },
                    )

                    Spacer(modifier = Modifier.height(dimens.space12))
                }
            },
            gesturesEnabled = activeView == MainView.Terminal || activeView == MainView.TerminalSession,
        ) {
            MainNavigationHost(
                activeView = activeView,
                onSwipeToSidebar = {
                    dismissKeyboard()
                    scope.launch { drawerState.open() }
                },
                onSwipeToTerminal = {
                    activeView = MainView.Terminal
                },
            ) { targetView ->
                when (targetView) {
                    MainView.SystemPrompt -> {
                        val promptScreen =
                            remember {
                                SystemPromptEditorScreen(
                                    onBack = { activeView = MainView.Terminal },
                                )
                            }
                        promptScreen.Content()
                    }

                    MainView.Settings -> {
                        val settingsScreen =
                            remember {
                                SettingsScreen(
                                    onBack = { activeView = MainView.Terminal },
                                )
                            }
                        settingsScreen.Content()
                    }

                    MainView.Terminal -> agentListScreen.Content()
                    MainView.GatewaySettings -> {
                        val gatewaySettingsScreen =
                            remember {
                                GatewaySettingsScreen(
                                    onBack = { activeView = MainView.Terminal },
                                )
                            }
                        gatewaySettingsScreen.Content()
                    }

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
                            LaunchedEffect(lastSessionId) {
                                activeView = MainView.Terminal
                            }
                        }
                    }
                }
            }
        }
    }
}
