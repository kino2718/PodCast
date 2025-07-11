package net.kino2718.podcast.ui.utils

import net.kino2718.podcast.data.Episode
import net.kino2718.podcast.data.MutableEpisode
import net.kino2718.podcast.data.MutablePChannel
import net.kino2718.podcast.data.PodCast
import net.kino2718.podcast.utils.MyLog
import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader

fun loadRss(feedUrl: String): PodCast? {
    val client = OkHttpClient()
    val request = Request.Builder().url(feedUrl).build()
    val response = try {
        client.newCall(request).execute()
    } catch (e: Exception) {
        MyLog.e(TAG, "load error: $e")
        null
    }
    return response?.let { r ->
        if (r.isSuccessful) {
            r.body.string().let { xml ->
                parse(xml, feedUrl)?.let { podCast ->
                    // episodeにimageUrlが含まれていない場合はchanelのimageUrlで代用する。
                    val episodes = podCast.episodeList.map { episode ->
                        if (episode.imageUrl == null) episode.copy(imageUrl = podCast.channel.imageUrl) else episode
                    }
                    // 最新のepisodeの発行日付をchannelに設定
                    val latestPubDate = episodes.getOrNull(0)?.pubDate
                    podCast.copy(
                        channel = podCast.channel.copy(lastUpdate = latestPubDate),
                        episodeList = episodes
                    )
                }
            }
        } else null
    }
}

private fun parse(xml: String, feedUrl: String): PodCast? {
    return try {
        val factory = XmlPullParserFactory.newInstance()
        val parser = factory.newPullParser()
        parser.setInput(StringReader(xml))
        var event = parser.eventType

        val currentChannel = MutablePChannel()
        currentChannel.feedUrl = feedUrl
        var currentEpisode = MutableEpisode()
        val episodes = mutableListOf<Episode>()
        var inItem = false // item解析中
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "item" -> {
                            inItem = true
                            currentEpisode = MutableEpisode()
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
                        episodes.add(currentEpisode.toImmutable())
                        inItem = false
                    }
                }
            }
            event = parser.next()
        }
        PodCast(
            channel = currentChannel.toImmutable(),
            episodeList = episodes.toList()
        )
    } catch (e: Exception) {
        MyLog.e(TAG, "parse error: $e")
        null
    }
}

private const val TAG = "Rss"