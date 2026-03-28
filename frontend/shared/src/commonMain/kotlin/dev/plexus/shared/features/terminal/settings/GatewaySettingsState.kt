package dev.plexus.shared.features.terminal.settings

import dev.plexus.shared.core.settings.AppTheme

/**
 * Gateway設定画面のUI状態
 *
 * @property inputGatewayUrl 入力されたGateway URL
 * @property inputApiKey 入力されたAPI Key
 * @property selectedTheme 選択中のテーマ
 * @property isSaving 保存処理中かどうか
 */

data class GatewaySettingsState(
    val inputGatewayUrl: String = "",
    val inputApiKey: String = "",
    val selectedTheme: AppTheme = AppTheme.DARK,
    val isSaving: Boolean = false,
) {
    val canSave: Boolean
        get() = inputGatewayUrl.isNotBlank() && inputApiKey.isNotBlank()
}
