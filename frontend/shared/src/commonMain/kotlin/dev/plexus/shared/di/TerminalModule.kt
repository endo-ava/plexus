package dev.plexus.shared.di

import dev.plexus.shared.core.data.repository.TerminalRepositoryImpl
import dev.plexus.shared.core.data.repository.internal.RepositoryClient
import dev.plexus.shared.core.domain.repository.TerminalRepository
import dev.plexus.shared.core.platform.PlatformPreferences
import dev.plexus.shared.core.platform.PlatformPrefsDefaults
import dev.plexus.shared.core.platform.PlatformPrefsKeys
import dev.plexus.shared.core.platform.getDefaultGatewayBaseUrl
import dev.plexus.shared.core.platform.normalizeBaseUrl
import org.koin.core.qualifier.named
import org.koin.dsl.module

val terminalModule =
    module {
        // === Gateway Configuration ===
        single<String>(qualifier = named("GatewayBaseUrl")) {
            val preferences = getOrNull<PlatformPreferences>()
            val savedUrl = preferences?.getString(PlatformPrefsKeys.KEY_GATEWAY_API_URL, PlatformPrefsDefaults.DEFAULT_GATEWAY_API_URL)
            val rawUrl = if (savedUrl.isNullOrBlank()) getDefaultGatewayBaseUrl() else savedUrl
            try {
                normalizeBaseUrl(rawUrl)
            } catch (e: IllegalArgumentException) {
                getDefaultGatewayBaseUrl()
            }
        }

        single<String>(qualifier = named("GatewayApiKey")) {
            val preferences = getOrNull<PlatformPreferences>()
            preferences?.getString(PlatformPrefsKeys.KEY_GATEWAY_API_KEY, PlatformPrefsDefaults.DEFAULT_GATEWAY_API_KEY) ?: ""
        }

        // === RepositoryClient (Gateway API) ===
        single<RepositoryClient>(qualifier = named("GatewayClient")) {
            RepositoryClient(
                httpClient = get(),
                baseUrl = get(qualifier = named("GatewayBaseUrl")),
                apiKey = get(qualifier = named("GatewayApiKey")),
            )
        }

        // === Repository ===
        single<TerminalRepository> {
            TerminalRepositoryImpl(
                repositoryClient = get(qualifier = named("GatewayClient")),
            )
        }
    }
