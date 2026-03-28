package dev.plexus.shared.di

import dev.plexus.shared.cache.DiskCache
import dev.plexus.shared.cache.DiskCacheContext
import dev.plexus.shared.core.data.repository.SystemPromptRepositoryImpl
import dev.plexus.shared.core.data.repository.internal.InMemoryCache
import dev.plexus.shared.core.data.repository.internal.RepositoryClient
import dev.plexus.shared.core.domain.repository.SystemPromptRepository
import dev.plexus.shared.core.network.HttpClientConfig
import dev.plexus.shared.core.network.HttpClientConfigProvider
import dev.plexus.shared.core.network.provideHttpClient
import dev.plexus.shared.core.platform.PlatformPreferences
import dev.plexus.shared.core.platform.PlatformPrefsDefaults
import dev.plexus.shared.core.platform.PlatformPrefsKeys
import dev.plexus.shared.core.platform.getDefaultBaseUrl
import dev.plexus.shared.core.platform.normalizeBaseUrl
import dev.plexus.shared.core.settings.ThemeRepository
import dev.plexus.shared.core.settings.ThemeRepositoryImpl
import dev.plexus.shared.features.systemprompt.SystemPromptEditorScreenModel
import dev.plexus.shared.features.terminal.agentlist.AgentListScreenModel
import dev.plexus.shared.features.terminal.settings.GatewaySettingsScreenModel
import io.ktor.client.HttpClient
import org.koin.core.qualifier.named
import org.koin.dsl.module

/**
 * Application-wide DI module
 *
 * Provides all application dependencies using Koin's traditional module definition.
 */
val appModule =
    module {
        // === Configuration ===
        single<String>(qualifier = named("BaseUrl")) {
            val preferences = getOrNull<PlatformPreferences>()
            val savedUrl = preferences?.getString(PlatformPrefsKeys.KEY_API_URL, PlatformPrefsDefaults.DEFAULT_API_URL)
            val rawUrl = if (savedUrl.isNullOrBlank()) getDefaultBaseUrl() else savedUrl
            try {
                normalizeBaseUrl(rawUrl)
            } catch (e: IllegalArgumentException) {
                getDefaultBaseUrl()
            }
        }

        single<String>(qualifier = named("ApiKey")) {
            val preferences = getOrNull<PlatformPreferences>()
            preferences?.getString(PlatformPrefsKeys.KEY_API_KEY, PlatformPrefsDefaults.DEFAULT_API_KEY) ?: ""
        }

        // === HTTP Client Configuration ===
        single<HttpClientConfigProvider> { HttpClientConfigProvider() }

        single<HttpClientConfig> {
            get<HttpClientConfigProvider>().getConfig()
        }

        // === Core ===
        single<HttpClient> {
            provideHttpClient(get<HttpClientConfig>())
        }

        single<DiskCache?> {
            val context = getOrNull<DiskCacheContext>()
            context?.let { DiskCache(it) }
        }

        // === RepositoryClient (Backend API) ===
        single<RepositoryClient>(qualifier = named("BackendClient")) {
            RepositoryClient(
                httpClient = get(),
                baseUrl = get(qualifier = named("BaseUrl")),
                apiKey = get(qualifier = named("ApiKey")),
            )
        }

        // === Cache ===
        single<InMemoryCache<String, Any>> { InMemoryCache() }

        // === Repositories ===
        single<SystemPromptRepository> {
            SystemPromptRepositoryImpl(
                repositoryClient = get(qualifier = named("BackendClient")),
                diskCache = getOrNull(),
            )
        }

        single<ThemeRepository> {
            ThemeRepositoryImpl(preferences = get())
        }

        // === ScreenModels ===
        factory {
            AgentListScreenModel(
                terminalRepository = get(),
                preferences = get(),
            )
        }

        factory {
            GatewaySettingsScreenModel(
                preferences = get(),
                themeRepository = get(),
            )
        }

        factory {
            SystemPromptEditorScreenModel(
                repository = get(),
            )
        }
    }
