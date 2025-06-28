package net.kino2718.podcast.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.kino2718.podcast.data.PlayItem
import net.kino2718.podcast.data.Repository
import net.kino2718.podcast.ui.utils.loadRss
import net.kino2718.podcast.utils.MyLog
import okhttp3.internal.toImmutableList

class HomeViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = Repository(app)
    val subscribedFlow =
        repo.subscribedChannelFlow().stateIn(viewModelScope, SharingStarted.Lazily, listOf())

    // last played item id が変化したら recent plays list を更新する。last played item id自体は使用しない。タイミングを取るだけ。
    val recentPlays = repo.getLastPlayedItemIdFlow().map { _ ->
        repo.getRecentPlays(NUM_RECENT_PLAYS)
    }.stateIn(viewModelScope, SharingStarted.Lazily, listOf())

    // next episodes flow
    private val _nextEpisodesFlow = MutableStateFlow<List<PlayItem>>(listOf())
    val nextEpisodesFlow = _nextEpisodesFlow.asStateFlow()

    init {
        viewModelScope.launch {
            // next episodes flowを作成。
            // 登録してあるpodcastそれぞれから最近聴き終えたepisodeの次のepisodeを表示する。
            // 取得するタイミングは last played item id が変化したタイミング。
            repo.getLastPlayedItemIdFlow().collect { _ ->
                MyLog.d(TAG, "nextEpisodesFlow")
                val nextEpisodeList = mutableListOf<PlayItem>()

                repo.subscribedChannels().map { channel ->
                    // 各channel毎に処理する。
                    // rssを読み込む。
                    withContext(Dispatchers.IO) { loadRss(channel.feedUrl) }?.let { rssData ->
                        // 最後に聴いたepisodeを取得
                        repo.getLastPlayedEpisode(channel.id)?.let { lastPlayed ->
                            // podCastの中から最後に聴いたepisodeのlist中のidを取得
                            val index =
                                rssData.episodeList.indexOfFirst { it.guid == lastPlayed.guid }
                            // 見つからない(index == -1)場合は何もしない。先頭なら最後に聴いたのが最新なので何もしない。
                            // そうでないなら次に聴くepisodeとしてリストアップする。
                            if (0 < index) {
                                val nextEpisode = rssData.episodeList[index - 1]
                                MyLog.d(TAG, "nextEpisodesFlow: next episode = $nextEpisode")
                                nextEpisodeList.add(
                                    PlayItem(
                                        channel = rssData.channel,
                                        episode = nextEpisode
                                    )
                                )
                                // rss処理に時間がかかるので途中経過も表示する。
                                _nextEpisodesFlow.value = nextEpisodeList.toImmutableList()
                            }
                        }
                    }
                }
            }
        }
    }

    companion object {
        @Suppress("unused")
        private const val TAG = "HomeViewModel"
        private const val NUM_RECENT_PLAYS = 10
    }
}