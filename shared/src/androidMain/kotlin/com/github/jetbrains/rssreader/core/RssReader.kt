package com.github.jetbrains.rssreader.core

import android.content.Context
import com.github.jetbrains.rssreader.core.datasource.network.FeedLoaderImpl
import com.github.jetbrains.rssreader.core.datasource.storage.FeedStorageImpl
import com.russhwolf.settings.AndroidSettings
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import kotlinx.serialization.json.Json

/**
 * Android向けRssReader作成
 */
fun RssReader.Companion.create(ctx: Context, withLog: Boolean) = RssReader(
    FeedLoaderImpl(
        // OkHttpを使ったHTTPクライアント
        AndroidHttpClient(withLog),
        // Android SDKを使ったXMLパーサ
        AndroidFeedParser()
    ),
    FeedStorageImpl(
        // AndroidはSharedPreferences
        AndroidSettings(ctx.getSharedPreferences("rss_reader_pref", Context.MODE_PRIVATE)),
        Json {
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = false
        }
    )
).also {
    if (withLog) Napier.base(DebugAntilog())
}