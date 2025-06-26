package net.kino2718.podcast.ui.podcast

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.kino2718.podcast.data.Episode
import net.kino2718.podcast.data.PChannel
import net.kino2718.podcast.data.PodCast
import net.kino2718.podcast.data.Repository
import net.kino2718.podcast.ui.utils.hmsToSeconds
import net.kino2718.podcast.ui.utils.parseToInstant
import net.kino2718.podcast.utils.MyLog
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.internal.toImmutableList
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader

data class PodCastUIState(
    val podCast: PodCast,
)

class PodCastViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = Repository(app)

    private val podCastFlowFromSearch = MutableSharedFlow<PodCast>()

    val uiState = podCastFlowFromSearch.map { fromSearch ->
        val fromDb = repo.getPodCastByFeedUrl(fromSearch.channel.feedUrl)
        // このpodcastはdatabaseには存在しない。
        if (fromDb == null) return@map PodCastUIState(fromSearch)

        // dbからのchannelのidを状態をコピーする。
        val fromDbChannel = fromDb.channel
        val newChannel = fromSearch.channel.copy(
            id = fromDbChannel.id,
            subscribed = fromDbChannel.subscribed,
            lastUpdate = fromDbChannel.lastUpdate
        )
        // dbからのitemのidと状態をコピーする。
        val itemListFromSearch = fromSearch.episodeLists
        val itemListFromDb = fromDb.episodeLists
        val newItemList = itemListFromSearch.map { itemFromSearch ->
            itemListFromDb.find { itemFromDb -> itemFromSearch.guid == itemFromDb.guid }
                ?.let { foundItem ->
                    itemFromSearch.copy(
                        id = foundItem.id,
                        channelId = foundItem.channelId,
                        playbackPosition = foundItem.playbackPosition,
                        duration = foundItem.duration,
                        isPlaybackCompleted = foundItem.isPlaybackCompleted,
                    )
                } ?: itemFromSearch
        }
        PodCastUIState(
            fromSearch.copy(channel = newChannel, episodeLists = newItemList)
        )
    }.stateIn(viewModelScope, SharingStarted.Lazily, null)

    // Urlからrssを読み解析してPodCastオブジェクトを作成してflowに流す。
    fun load(feedUrl: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val client = OkHttpClient()
            val request = Request.Builder().url(feedUrl).build()
            val response = try {
                client.newCall(request).execute()
            } catch (e: Exception) {
                MyLog.e(TAG, "load error: $e")
                null
            }
            response?.let { r ->
                if (r.isSuccessful) {
                    r.body?.string()?.let { xml ->
                        parse(xml, feedUrl)?.let {
                            podCastFlowFromSearch.emit(it)
                        }
                    }
                }
            }
        }
    }

    private fun parse(xml: String, feedUrl: String): PodCast? {
        return try {
            val factory = XmlPullParserFactory.newInstance()
            val parser = factory.newPullParser()
            parser.setInput(StringReader(xml))
            var event = parser.eventType

            val currentChannel = PChannel.Builder()
            currentChannel.feedUrl = feedUrl
            var currentEpisode = Episode.Builder()
            val episodes = mutableListOf<Episode>()
            var inItem = false // item解析中
            while (event != XmlPullParser.END_DOCUMENT) {
                when (event) {
                    XmlPullParser.START_TAG -> {
                        when (parser.name) {
                            "item" -> {
                                inItem = true
                                currentEpisode = Episode.Builder()
                            }

                            "guid" -> {
                                if (inItem) {
                                    val text = parser.nextText()
                                    currentEpisode.guid = text
                                }
                            }

                            "title" -> {
                                val text = parser.nextText()
                                if (inItem) currentEpisode.title = text
                                else currentChannel.title = text
                            }

                            "itunes:author" -> {
                                val text = parser.nextText()
                                if (inItem) currentEpisode.author = text
                                else currentChannel.author = text
                            }

                            "description" -> {
                                val text = parser.nextText()
                                if (inItem) currentEpisode.description = text
                                else currentChannel.description = text
                            }

                            "link" -> {
                                val text = parser.nextText()
                                if (inItem) currentEpisode.link = text
                                else currentChannel.link = text
                            }

                            "itunes:image" -> {
                                val text = parser.getAttributeValue(null, "href")
                                if (inItem) currentEpisode.imageUrl = text
                                else currentChannel.imageUrl = text
                            }

                            "enclosure" -> {
                                if (inItem) {
                                    val text = parser.getAttributeValue(null, "url")
                                    currentEpisode.url = text
                                }
                            }

                            "pubDate" -> {
                                if (inItem) {
                                    val d = parser.nextText().parseToInstant()
                                    currentEpisode.pubDate = d
                                }
                            }

                            "itunes:duration" -> {
                                if (inItem) {
                                    val d = parser.nextText().hmsToSeconds() * 1000L
                                    currentEpisode.duration = d
                                }
                            }
                        }
                    }

                    XmlPullParser.END_TAG -> {
                        if (parser.name == "item") {
                            episodes.add(currentEpisode.build())
                            inItem = false
                        }
                    }
                }
                event = parser.next()
            }
            PodCast(
                channel = currentChannel.build(),
                episodeLists = episodes.toImmutableList()
            )
        } catch (e: Exception) {
            MyLog.e(TAG, "parse error: $e")
            null
        }
    }

    fun subscribe(channel: PChannel, subscribe: Boolean) {
        viewModelScope.launch {
            repo.subscribe(channel, subscribe)
        }
    }

    companion object {
        private const val TAG = "PodCastViewModel"
    }
}
