package com.github.jetbrains.rssreader.core

import com.github.jetbrains.rssreader.core.datasource.network.FeedLoaderImpl
import com.github.jetbrains.rssreader.core.datasource.storage.FeedStorageImpl
import com.russhwolf.settings.AppleSettings
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import kotlinx.serialization.json.Json
import platform.Foundation.NSUserDefaults

/**
 * iOS向けRssReaderの実装
 */
fun RssReader.Companion.create(withLog: Boolean) = RssReader(
    FeedLoaderImpl(
        IosHttpClient(withLog),
        IosFeedParser()
    ),
    FeedStorageImpl(
        // iOSはNSUserDefaults
        AppleSettings(NSUserDefaults.standardUserDefaults()),
        Json {
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = false
        }
    )
).also {
    if (withLog) Napier.base(DebugAntilog())
}