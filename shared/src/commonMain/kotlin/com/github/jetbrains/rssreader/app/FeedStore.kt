package com.github.jetbrains.rssreader.app

import com.github.jetbrains.rssreader.core.RssReader
import com.github.jetbrains.rssreader.core.entity.Feed
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * フィード画面の状態
 *
 * @param progress スワイプロード中
 * @param feeds RSS一覧
 * @param selectedFeed 選択されたRSS
 */
data class FeedState(
    val progress: Boolean,
    val feeds: List<Feed>,
    val selectedFeed: Feed? = null //null means selected all
) : State

fun FeedState.mainFeedPosts() =
    (selectedFeed?.posts ?: feeds.flatMap { it.posts }).sortedByDescending { it.date }

/**
 * ユーザ操作または処理結果
 */
sealed class FeedAction : Action {

    /**
     * 下スワイプリロード
     *
     * @param forceLoad trueの時はキャッシュを使わない
     */
    data class Refresh(val forceLoad: Boolean) : FeedAction()

    /**
     * RSSを追加する
     */
    data class Add(val url: String) : FeedAction()

    /**
     * RSSを削除する
     */
    data class Delete(val url: String) : FeedAction()

    /**
     * 表示RSSを選択する
     */
    data class SelectFeed(val feed: Feed?) : FeedAction()

    /**
     * RSSを表示する
     */
    data class Data(val feeds: List<Feed>) : FeedAction()

    /**
     * エラーを表示する
     */
    data class Error(val error: Exception) : FeedAction()
}

/**
 * 副作用
 */
sealed class FeedSideEffect : Effect {
    /**
     * エラーが発生
     */
    data class Error(val error: Exception) : FeedSideEffect()
}

class FeedStore(
    private val rssReader: RssReader
) : Store<FeedState, FeedAction, FeedSideEffect>,
    /* launchメソッド呼び出しのための継承 */
    CoroutineScope by CoroutineScope(Dispatchers.Main) {

    /**
     * 画面の状態。
     */
    private val state = MutableStateFlow(
        // 初期値
        FeedState(false, emptyList())
    )

    /**
     * 画面の副作用。
     */
    private val sideEffect = MutableSharedFlow<FeedSideEffect>()

    override fun observeState(): StateFlow<FeedState> = state

    override fun observeSideEffect(): Flow<FeedSideEffect> = sideEffect

    override fun dispatch(action: FeedAction) {
        Napier.d(tag = "FeedStore", message = "Action: $action")
        val oldState = state.value

        val newState = when (action) {
            is FeedAction.Refresh -> {
                if (oldState.progress) {
                    // プログレス表示中は、処理中エラーとなる
                    launch { sideEffect.emit(FeedSideEffect.Error(Exception("In progress"))) }
                    oldState
                } else {
                    launch { loadAllFeeds(action.forceLoad) }
                    // プログレスを表示する
                    oldState.copy(progress = true)
                }
            }
            is FeedAction.Add -> {
                if (oldState.progress) {
                    launch { sideEffect.emit(FeedSideEffect.Error(Exception("In progress"))) }
                    oldState
                } else {
                    launch { addFeed(action.url) }
                    FeedState(true, oldState.feeds)
                }
            }
            is FeedAction.Delete -> {
                if (oldState.progress) {
                    launch { sideEffect.emit(FeedSideEffect.Error(Exception("In progress"))) }
                    oldState
                } else {
                    launch { deleteFeed(action.url) }
                    FeedState(true, oldState.feeds)
                }
            }
            is FeedAction.SelectFeed -> {
                if (action.feed == null || oldState.feeds.contains(action.feed)) {
                    oldState.copy(selectedFeed = action.feed)
                } else {
                    launch { sideEffect.emit(FeedSideEffect.Error(Exception("Unknown feed"))) }
                    oldState
                }
            }
            is FeedAction.Data -> {
                if (oldState.progress) {
                    val selected = oldState.selectedFeed?.let {
                        if (action.feeds.contains(it)) it else null
                    }
                    FeedState(false, action.feeds, selected)
                } else {
                    launch { sideEffect.emit(FeedSideEffect.Error(Exception("Unexpected action"))) }
                    oldState
                }
            }
            is FeedAction.Error -> {
                if (oldState.progress) {
                    launch { sideEffect.emit(FeedSideEffect.Error(action.error)) }
                    FeedState(false, oldState.feeds)
                } else {
                    launch { sideEffect.emit(FeedSideEffect.Error(Exception("Unexpected action"))) }
                    oldState
                }
            }
        }

        if (newState != oldState) {
            Napier.d(tag = "FeedStore", message = "NewState: $newState")
            state.value = newState
        }
    }

    /**
     * RSS読み込み
     *
     * @param forceLoad trueの時はキャッシュを使わない
     */
    private suspend fun loadAllFeeds(forceLoad: Boolean) {
        try {
            val allFeeds = rssReader.getAllFeeds(forceLoad)
            dispatch(FeedAction.Data(allFeeds))
        } catch (e: Exception) {
            dispatch(FeedAction.Error(e))
        }
    }

    /**
     * RSSの追加
     */
    private suspend fun addFeed(url: String) {
        try {
            // 取得してローカルに保存する
            rssReader.addFeed(url)
            // 全件ローカルから取得する
            val allFeeds = rssReader.getAllFeeds(false)
            dispatch(FeedAction.Data(allFeeds))
        } catch (e: Exception) {
            dispatch(FeedAction.Error(e))
        }
    }

    private suspend fun deleteFeed(url: String) {
        try {
            rssReader.deleteFeed(url)
            val allFeeds = rssReader.getAllFeeds(false)
            dispatch(FeedAction.Data(allFeeds))
        } catch (e: Exception) {
            dispatch(FeedAction.Error(e))
        }
    }
}
