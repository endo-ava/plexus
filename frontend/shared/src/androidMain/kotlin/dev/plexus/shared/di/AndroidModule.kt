package dev.plexus.shared.di

import android.content.Context
import dev.plexus.shared.cache.DiskCacheContext
import dev.plexus.shared.core.platform.PlatformPreferences
import org.koin.dsl.module

val androidModule =
    module {
        single<DiskCacheContext> {
            DiskCacheContext(get<Context>().cacheDir.absolutePath)
        }

        single<PlatformPreferences> {
            PlatformPreferences(get<Context>())
        }
    }
