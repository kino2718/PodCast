package net.kino2718.podcast.ui.search

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.kino2718.podcast.data.Repository
import net.kino2718.podcast.utils.MyLog
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

data class PodcastState(
    val collectionName: String,
    val artistName: String,
    val feedUrl: String,
    val trackCount: Int,
    val artworkUrl100: String,
)

data class SearchUIState(
    val results: List<PodcastState> = listOf()
)

class SearchViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = Repository(app)
    private val _searchUIStateFlow = MutableStateFlow(SearchUIState())
    val searchUIStateFlow = _searchUIStateFlow.asStateFlow()
    val subscribedFlow =
        repo.subscribedChannelsFlow().stateIn(viewModelScope, SharingStarted.Lazily, listOf())

    fun searchPodcasts(keyword: String) {
        if (keyword.isBlank()) return

        viewModelScope.launch(Dispatchers.IO) {
            val client = OkHttpClient.Builder()
                .connectTimeout(TIME_OUT, TimeUnit.SECONDS)  // 接続タイムアウト
                .readTimeout(TIME_OUT, TimeUnit.SECONDS)     // 読み取りタイムアウト
                .writeTimeout(TIME_OUT, TimeUnit.SECONDS)    // 書き込みタイムアウト
                .build()
            val encoded = URLEncoder.encode(keyword, "UTF-8")
            val request = Request.Builder()
                .url("https://itunes.apple.com/search?media=podcast&term=$encoded")
                .build()
            val response = try {
                client.newCall(request).execute()
            } catch (e: Exception) {
                MyLog.e(TAG, "searchPodcasts error: $e")
                null
            }
            response?.use { r ->
                if (r.isSuccessful) {
                    val body = r.body
                    body.let { b ->
                        val json = JSONObject(b.string())
                        val results = json.getJSONArray("results")
                        val spsList = List(results.length()) { i ->
                            results.getJSONObject(i)
                        }.map { jo ->
                            PodcastState(
                                collectionName = jo.optString("collectionName"),
                                artistName = jo.optString("artistName"),
                                feedUrl = jo.optString("feedUrl"),
                                trackCount = jo.optInt("trackCount"),
                                artworkUrl100 = jo.optString("artworkUrl100"),
                            )
                        }
                        _searchUIStateFlow.value = SearchUIState(results = spsList)
                    }
                }
            }
        }
    }

    companion object {
        private const val TAG = "SearchViewModel"
        private const val TIME_OUT = 30L // sec
    }
}