package net.kino2718.podcast.ui.podcast

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.kino2718.podcast.utils.MyLog
import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader

/** Channel information for UI */
data class ChannelState(
    val title: String,
    val author: String,
)

/** Item information for UI */
data class ItemState(
    val title: String,
    val audioUrl: String,
)

/** RSS feed UI state */
data class PodCastState(
    val channel: ChannelState? = null,
    val itemList: List<ItemState> = emptyList(),
)

class PodCastViewModel(app: Application) : AndroidViewModel(app) {
    private val _uiState = MutableStateFlow(PodCastState())
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
                        parse(xml)
                    }
                }
            }
        }
    }

    private fun parse(xml: String) {
        try {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = true
            val parser = factory.newPullParser()
            parser.setInput(StringReader(xml))
            var event = parser.eventType
            var channelTitle: String? = null
            var channelAuthor: String? = null
            val items = mutableListOf<ItemState>()
            var inItem = false
            var itemTitle: String? = null
            var itemUrl: String? = null
            while (event != XmlPullParser.END_DOCUMENT) {
                when (event) {
                    XmlPullParser.START_TAG -> {
                        when (parser.name) {
                            "item" -> {
                                inItem = true
                                itemTitle = null
                                itemUrl = null
                            }
                            "title" -> {
                                val text = parser.nextText()
                                if (inItem) itemTitle = text else channelTitle = text
                            }
                            "enclosure" -> {
                                if (inItem) {
                                    itemUrl = parser.getAttributeValue(null, "url")
                                }
                            }
                            "author", "itunes:author" -> {
                                if (!inItem) {
                                    channelAuthor = parser.nextText()
                                }
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (parser.name == "item") {
                            if (itemTitle != null && itemUrl != null) {
                                items.add(ItemState(itemTitle!!, itemUrl!!))
                            }
                            inItem = false
                        }
                    }
                }
                event = parser.next()
            }
            val channel = if (channelTitle != null && channelAuthor != null) {
                ChannelState(channelTitle!!, channelAuthor!!)
            } else {
                null
            }
            _uiState.value = PodCastState(channel = channel, itemList = items)
        } catch (e: Exception) {
            MyLog.e(TAG, "parse error: $e")
        }
    }

    companion object {
        private const val TAG = "PodCastViewModel"
    }
}
