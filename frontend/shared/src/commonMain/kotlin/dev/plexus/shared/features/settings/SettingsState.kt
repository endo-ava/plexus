package dev.plexus.shared.features.settings

import dev.plexus.shared.core.settings.AppTheme

/**
 * アプリケーション設定画面のUI状態
 *
 * @property selectedTheme 選択中のテーマ（LIGHT/DARK/SYSTEM）
 * @property inputUrl 入力されたAPI URL
 * @property inputKey 入力されたAPI Key
 * @property isSaving 保存処理中かどうか
 */

data class SettingsState(
    val selectedTheme: AppTheme = AppTheme.SYSTEM,
    val inputUrl: String = "",
    val inputKey: String = "",
    val isSaving: Boolean = false,
)
