package net.kino2718.podcast.ui.podcast

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.kino2718.podcast.data.Item
import net.kino2718.podcast.data.PChannel
import net.kino2718.podcast.data.PlayItem
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

    private val _uiState = MutableStateFlow<PodCastUIState?>(null)
    val uiState = _uiState.asStateFlow()

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
                        parse(xml)?.let {
                            val podCast = it.copy(
                                channel = it.channel.copy(feedUrl = feedUrl)
                            )
                            MyLog.d(
                                TAG,
                                "channel = ${podCast.channel}, item = ${podCast.itemList[0]}"
                            )
                            _uiState.value = PodCastUIState(podCast)
                        }
                    }
                }
            }
        }
    }

    private fun parse(xml: String): PodCast? {
        val xmlHead = xml.substring(1, 5000)
        MyLog.d(TAG, "xml = $xmlHead")
        return try {
            val factory = XmlPullParserFactory.newInstance()
            val parser = factory.newPullParser()
            parser.setInput(StringReader(xml))
            var event = parser.eventType

            var currentChannel = PChannel()
            var currentItem = Item()
            val items = mutableListOf<Item>()
            var inItem = false // item解析中
            while (event != XmlPullParser.END_DOCUMENT) {
                when (event) {
                    XmlPullParser.START_TAG -> {
                        when (parser.name) {
                            "item" -> {
                                inItem = true
                                currentItem = Item()
                            }

                            "guid" -> {
                                if (inItem) {
                                    val text = parser.nextText()
                                    currentItem = currentItem.copy(guid = text)
                                }
                            }

                            "title" -> {
                                val text = parser.nextText()
                                if (inItem) currentItem = currentItem.copy(title = text)
                                else currentChannel = currentChannel.copy(title = text)
                            }

                            "itunes:author" -> {
                                val text = parser.nextText()
                                if (inItem) currentItem = currentItem.copy(author = text)
                                else currentChannel = currentChannel.copy(author = text)
                            }

                            "description" -> {
                                val text = parser.nextText()
                                if (inItem) currentItem = currentItem.copy(description = text)
                                else currentChannel = currentChannel.copy(description = text)
                            }

                            "link" -> {
                                val text = parser.nextText()
                                if (inItem) currentItem = currentItem.copy(link = text)
                                else currentChannel = currentChannel.copy(link = text)
                            }

                            "itunes:image" -> {
                                val text = parser.getAttributeValue(null, "href")
                                if (inItem) currentItem = currentItem.copy(imageUrl = text)
                                else currentChannel = currentChannel.copy(imageUrl = text)
                            }

                            "enclosure" -> {
                                if (inItem) {
                                    val text = parser.getAttributeValue(null, "url")
                                    currentItem = currentItem.copy(url = text)
                                }
                            }

                            "pubDate" -> {
                                if (inItem) {
                                    val d = parser.nextText().parseToInstant()
                                    currentItem = currentItem.copy(pubDate = d)
                                }
                            }

                            "itunes:duration" -> {
                                if (inItem) {
                                    val d = parser.nextText().hmsToSeconds()
                                    currentItem = currentItem.copy(duration = d)
                                }
                            }
                        }
                    }

                    XmlPullParser.END_TAG -> {
                        if (parser.name == "item") {
                            items.add(currentItem)
                            inItem = false
                        }
                    }
                }
                event = parser.next()
            }
            PodCast(
                channel = currentChannel,
                itemList = items.toImmutableList()
            )
        } catch (e: Exception) {
            MyLog.e(TAG, "parse error: $e")
            null
        }
    }

    fun addPlayItem(channel: PChannel, item: Item) {
        viewModelScope.launch {
            val playItem = PlayItem(channel = channel, item = item)
            repo.addPlayItem(playItem)
        }
    }

    companion object {
        private const val TAG = "PodCastViewModel"
    }
}
