package com.github.jetbrains.rssreader.core.datasource.network

import com.github.jetbrains.rssreader.core.entity.Feed
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*

interface FeedLoader {
    suspend fun getFeed(url: String, isDefault: Boolean): Feed
}

/**
 * WebからRSSをとってくる担当
 *
 * @param httpClient HTTPクライアント(実装はiOSとAndroidで違う)
 * @param parser XMLパーサ(実装はiOSとAndroidで違う)
 */
internal class FeedLoaderImpl(
    private val httpClient: HttpClient,
    private val parser: FeedParser
) : FeedLoader {
    override suspend fun getFeed(url: String, isDefault: Boolean): Feed {
        // RSSのXMLをとってくる
        val xml = httpClient.get(url).bodyAsText()
        // XMLをパースする
        return parser.parse(url, xml, isDefault)
    }
}