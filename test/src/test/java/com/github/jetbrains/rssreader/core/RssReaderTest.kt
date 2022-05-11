package com.github.jetbrains.rssreader.core

import com.github.jetbrains.rssreader.core.datasource.network.FeedLoader
import com.github.jetbrains.rssreader.core.datasource.storage.FeedStorage
import com.github.jetbrains.rssreader.core.entity.Feed
import io.kotest.common.runBlocking
import io.mockk.*
import io.mockk.impl.annotations.MockK
import org.junit.Before
import org.junit.Test

class RssReaderTest {

    @MockK(relaxUnitFun = true)
    private lateinit var feedLoader: FeedLoader

    @MockK(relaxUnitFun = true)
    private lateinit var feedStorage: FeedStorage

    @MockK(relaxUnitFun = true)
    private lateinit var settings: Settings

    private lateinit var reader: RssReader

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        reader = RssReader(feedLoader, feedStorage, settings)
    }

    @Test
    fun addFeed() = runBlocking {
        val feed = mockk<Feed>()
        val url = "https://example.com/feed"
        every {
            settings.isDefault(url)
        } returns false
        coEvery {
            feedLoader.getFeed(url, false)
        } returns feed
        reader.addFeed(url)
        coVerifySequence {
            feedLoader.getFeed(url, false)
            feedStorage.saveFeed(feed)
        }
    }
}
