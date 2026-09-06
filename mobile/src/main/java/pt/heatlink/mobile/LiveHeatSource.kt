package pt.heatlink.mobile

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import pt.heatlink.shared.HeatParser
import pt.heatlink.shared.HeatUpdate
import java.net.HttpURLConnection
import java.net.URI

class LiveHeatSource {
    private var cachedPageUrl: String? = null
    private var cachedFeedUrl: String? = null

    suspend fun fetch(url: String, athlete: String): HeatUpdate = withContext(Dispatchers.IO) {
        if (isWaveLegacy(url)) {
            val feedUrl = if (cachedPageUrl == url) cachedFeedUrl else null
                ?: resolveWaveLegacyFeed(url).also {
                    cachedPageUrl = url
                    cachedFeedUrl = it
                }
            HeatParser.parseWaveLegacy(download(feedUrl, "application/json"), athlete)
        } else if (isSurfScores(url)) {
            val feedUrl = if (cachedPageUrl == url) cachedFeedUrl else null
                ?: resolveSurfScoresFeed(url).also {
                    cachedPageUrl = url
                    cachedFeedUrl = it
                }
            HeatParser.parseSurfScores(download(feedUrl, "application/json"), athlete)
        } else {
            HeatParser.parse(download(url, "application/json"), athlete)
        }
    }

    private fun resolveWaveLegacyFeed(pageUrl: String): String {
        val uri = URI(pageUrl)
        if (uri.path.endsWith("/event_state.php", ignoreCase = true)) return pageUrl
        val eventId = queryParameters(uri.rawQuery)["id"]
            ?: queryParameters(uri.rawQuery)["event"]
            ?: error("O link Wave Legacy não contém o identificador do evento")
        val basePath = uri.path.substringBeforeLast('/', "")
        return URI(uri.scheme, uri.authority, "$basePath/event_state.php", "id=$eventId", null).toString()
    }

    private fun queryParameters(query: String?): Map<String, String> = query.orEmpty()
        .split('&')
        .mapNotNull { part ->
            val pieces = part.split('=', limit = 2)
            if (pieces.size == 2) pieces[0] to pieces[1] else null
        }.toMap()

    private fun resolveSurfScoresFeed(pageUrl: String): String {
        if (pageUrl.contains("/scoreboard/getlive.php", ignoreCase = true)) return pageUrl
        val page = download(pageUrl, "text/html")
        val eventId = Regex("live\\.php\\?eventid=([A-Za-z0-9_-]+)", RegexOption.IGNORE_CASE)
            .find(page)?.groupValues?.get(1)
            ?: error("Não foi possível encontrar o evento ao vivo no link SurfScores")
        return "https://surfscores.com/scoreboard/getlive.php?eventid=$eventId&catname=&heatid="
    }

    private fun download(url: String, accept: String): String {
        val connection = URI(url).toURL().openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 8_000
            connection.readTimeout = 8_000
            connection.setRequestProperty("Accept", accept)
            connection.setRequestProperty("User-Agent", "HeatLink/0.1")
            connection.inputStream.bufferedReader().use { reader ->
                return reader.readText()
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun isSurfScores(url: String): Boolean =
        URI(url).host?.endsWith("surfscores.com", ignoreCase = true) == true

    private fun isWaveLegacy(url: String): Boolean {
        val path = URI(url).path.orEmpty()
        return path.endsWith("event_live.php", ignoreCase = true) ||
            path.endsWith("live_heat.php", ignoreCase = true) ||
            path.endsWith("event_state.php", ignoreCase = true)
    }
}
