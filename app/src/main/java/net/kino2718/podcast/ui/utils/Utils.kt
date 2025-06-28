package net.kino2718.podcast.ui.utils

import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toKotlinInstant
import kotlinx.datetime.toLocalDateTime
import java.text.NumberFormat
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.TimeUnit

@Suppress("unused")
private const val TAG = "Utils"

fun String.parseToInstant(): Instant {
    // 英語ロケールを指定して RFC-1123 フォーマッタを取得
    val formatter = DateTimeFormatter.RFC_1123_DATE_TIME.withLocale(Locale.ENGLISH)
    // ZonedDateTime にパース
    val zonedDateTime = ZonedDateTime.parse(this, formatter)
    // ZonedDateTime → java.time.Instant → kotlinx.datetime.Instant
    return zonedDateTime.toInstant().toKotlinInstant()
}

fun String.hmsToSeconds(): Long {
    val parts = this.split(":").map { it.toLong() }
    val (hours, minutes, seconds) =
        when (parts.size) {
            3 -> parts
            2 -> listOf(0L) + parts
            1 -> listOf(0L, 0L) + parts
            0 -> listOf(0L, 0L, 0L)
            else -> parts.subList(0, 3)
        }
    return hours * 3600 + minutes * 60 + seconds
}

fun Int.format(): String = NumberFormat.getNumberInstance(Locale.US).format(this)

fun Instant.format(timeZone: TimeZone = TimeZone.currentSystemDefault()): String {
    val localDateTime = this.toLocalDateTime(timeZone)
    return "${localDateTime.year}-" +
            "${localDateTime.monthNumber.toString().padStart(2, '0')}-" +
            "${localDateTime.dayOfMonth.toString().padStart(2, '0')} " +
            "${localDateTime.hour.toString().padStart(2, '0')}:" +
            localDateTime.minute.toString().padStart(2, '0')
}

fun Instant.formatToDate(
    template: String,
    timeZone: TimeZone = TimeZone.currentSystemDefault()
): String {
    val localDateTime = this.toLocalDateTime(timeZone)
    return String.format(template, localDateTime.monthNumber, localDateTime.dayOfMonth)
}

fun Long.toHMS(): String {
    // 負の時間が入ってきた時は0にする。
    val millis = if (0 <= this) this else return "-"
    // 秒に変換
    var seconds = (millis + 500L) / 1000L

    val sb = StringBuilder()
    // 時は1以上の場合のみ表示する。
    val hours = TimeUnit.SECONDS.toHours(seconds)
    if (0L < hours) {
        sb.append(hours).append(":")
        seconds -= hours * 60 * 60
    }
    // 分は0の場合も表示する。
    val minutes = TimeUnit.SECONDS.toMinutes(seconds)
    if (hours == 0L) {
        // 一時間未満の時は普通に分を表示。
        sb.append(minutes).append(":")
    } else {
        // 一時間を超えるときは分は二桁表示。
        val str = minutes.toString().padStart(2, '0')
        sb.append(str).append(":")
    }
    // 秒は常に二桁表示。
    seconds -= minutes * 60
    val str = seconds.toString().padStart(2, '0')
    sb.append(str)
    return sb.toString()
}

fun String.toHttps() = this.replaceFirst("http://", "https://")
