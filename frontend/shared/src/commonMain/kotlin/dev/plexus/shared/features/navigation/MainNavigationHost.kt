package dev.plexus.shared.features.navigation

import androidx.compose.runtime.Composable

/**
 * メインのナビゲーションHost
 *
 * スワイプジェスチャーと画面遷移を管理する。
 *
 * @param activeView 現在のアクティブなView
 * @param onSwipeToSidebar スワイプでサイドバーへ遷移するコールバック
 * @param onSwipeToTerminal スワイプでターミナル一覧へ遷移するコールバック
 * @param content 表示するコンテンツ
 */
@Composable
fun MainNavigationHost(
    activeView: MainView,
    onSwipeToSidebar: () -> Unit,
    onSwipeToTerminal: () -> Unit,
    content: @Composable (MainView) -> Unit,
) {
    SwipeNavigationContainer(
        activeView = activeView,
        onSwipeToSidebar = onSwipeToSidebar,
        onSwipeToTerminal = onSwipeToTerminal,
    ) {
        MainViewTransition(
            activeView = activeView,
            content = content,
        )
    }
}
