package net.kino2718.podcast.ui.search

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder

@Composable
fun SearchScreen(modifier: Modifier = Modifier) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf(listOf<String>()) }
    val scope = rememberCoroutineScope()

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("Search Podcasts") },
            trailingIcon = {
                IconButton(onClick = {
                    scope.launch {
                        results = searchPodcasts(query)
                    }
                }) {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(results) { name ->
                Text(text = name, modifier = Modifier.padding(vertical = 4.dp))
            }
        }
    }
}

private suspend fun searchPodcasts(keyword: String): List<String> {
    if (keyword.isBlank()) return emptyList()
    val client = OkHttpClient()
    val encoded = URLEncoder.encode(keyword, "UTF-8")
    val request = Request.Builder()
        .url("https://itunes.apple.com/search?media=podcast&term=$encoded")
        .build()
    return withContext(Dispatchers.IO) {
        runCatching { client.newCall(request).execute() }.getOrNull()?.use { response ->
            if (!response.isSuccessful) return@withContext emptyList<String>()
            val body = response.body?.string() ?: return@withContext emptyList<String>()
            val json = JSONObject(body)
            val results = json.getJSONArray("results")
            List(results.length()) { index ->
                results.getJSONObject(index).optString("collectionName")
            }
        } ?: emptyList()
    }
}
