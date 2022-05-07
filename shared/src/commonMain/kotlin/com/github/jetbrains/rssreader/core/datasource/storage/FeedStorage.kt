package com.github.jetbrains.rssreader.core.datasource.storage

import com.github.jetbrains.rssreader.core.entity.Feed
import com.russhwolf.settings.Settings
import com.russhwolf.settings.set
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * ローカル保存担当
 *
 * @param settings Multiplatform Settingsライブラリ。Key-Valueを保存できる。
 * @param json JSON serialization担当
 */
class FeedStorage(
    private val settings: Settings,
    private val json: Json
) {
    private companion object {
        private const val KEY_FEED_CACHE = "key_feed_cache"
    }

    /**
     * 単一のMapを取得および保存する担当
     */
    private var diskCache: Map<String, Feed>
        get() {
            return settings.getStringOrNull(KEY_FEED_CACHE)?.let { str ->
                json.decodeFromString(ListSerializer(Feed.serializer()), str)
                    .associate { it.sourceUrl to it }
            } ?: mutableMapOf()
        }
        set(value) {
            val list = value.map { it.value }
            settings[KEY_FEED_CACHE] =
                json.encodeToString(ListSerializer(Feed.serializer()), list)
        }

    /**
     * 編集用メモリ
     */
    private val memCache: MutableMap<String, Feed> by lazy { diskCache.toMutableMap() }

    suspend fun getFeed(url: String): Feed? = memCache[url]

    suspend fun saveFeed(feed: Feed) {
        // メモリに追加して
        memCache[feed.sourceUrl] = feed
        // 全部保存する
        diskCache = memCache
    }

    suspend fun deleteFeed(url: String) {
        // メモリから削除して
        memCache.remove(url)
        // 全部保存する
        diskCache = memCache
    }

    suspend fun getAllFeeds(): List<Feed> = memCache.values.toList()
}