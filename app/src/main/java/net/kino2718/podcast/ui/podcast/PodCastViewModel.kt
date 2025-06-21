package net.kino2718.podcast.ui.podcast

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.kino2718.podcast.utils.MyLog
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlAttr
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import okhttp3.OkHttpClient
import okhttp3.Request

@Serializable
@XmlSerialName("rss", namespace = "", prefix = "")
private data class Rss(
    @XmlElement(true)
    val channel: Channel = Channel()
)

@Serializable
private data class Channel(
    val title: String? = null,
    @SerialName("itunes:author") val author: String? = null,
    @XmlElement(true)
    val item: List<Item> = emptyList()
)

@Serializable
private data class Item(
    val title: String? = null,
    val enclosure: Enclosure? = null
)

@Serializable
private data class Enclosure(
    @XmlAttr
    val url: String? = null
)

data class ItemState(val title: String, val url: String)

data class PodCastUIState(
    val channelTitle: String = "",
    val channelAuthor: String = "",
    val items: List<ItemState> = emptyList()
)

class PodCastViewModel(app: Application) : AndroidViewModel(app) {
    private val _uiState = MutableStateFlow(PodCastUIState())
    val uiState = _uiState.asStateFlow()

    fun load(feedUrl: String) {
        if (feedUrl.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            val client = OkHttpClient()
            val request = Request.Builder().url(feedUrl).build()
            val response = try {
                client.newCall(request).execute()
            } catch (e: Exception) {
                MyLog.e(TAG, "load error: $e")
                null
            }
            response?.use { r ->
                if (r.isSuccessful) {
                    val body = r.body?.string()
                    body?.let { xmlStr ->
                        val xml = XML { }
                        val rss = try {
                            xml.decodeFromString<Rss>(xmlStr)
                        } catch (e: Exception) {
                            MyLog.e(TAG, "parse error: $e")
                            null
                        }
                        rss?.let { res ->
                            val items = res.channel.item.map { i ->
                                ItemState(
                                    title = i.title ?: "",
                                    url = i.enclosure?.url ?: ""
                                )
                            }
                            _uiState.value = PodCastUIState(
                                channelTitle = res.channel.title ?: "",
                                channelAuthor = res.channel.author ?: "",
                                items = items
                            )
                        }
                    }
                }
            }
        }
    }

    companion object {
        private const val TAG = "PodCastViewModel"
    }
}
