package pt.heatlink.shared

import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Reads a normalized JSON feed. Platform-specific adapters can convert a live
 * scoring provider response to these field names without changing either app.
 */
object HeatParser {
    fun parse(body: String, athleteHint: String = ""): HeatUpdate {
        val root = JSONObject(body)
        val heat = root.optJSONObject("heat") ?: root
        val athletes = heat.optJSONArray("athletes")
        val athlete = selectAthlete(athletes, athleteHint) ?: heat

        return HeatUpdate(
            athlete = athlete.firstString("athlete", "name", "competitor").ifBlank { athleteHint.ifBlank { "ATLETA" } },
            position = athlete.firstInt("position", "rank", "place") ?: 1,
            lastScore = athlete.firstDouble("lastScore", "latestScore", "waveScore", "score"),
            scoreNeeded = athlete.firstDouble("scoreNeeded", "neededScore", "scoreToAdvance", "requirement"),
            priority = athlete.firstInt("priority", "priorityPosition"),
            timeRemaining = heat.firstString("timeRemaining", "remaining", "clock", "time").ifBlank { "--:--" },
            heatName = heat.firstString("heatName", "name", "round").ifBlank { "HEAT" },
        )
    }

    fun parseSurfScores(body: String, athleteHint: String = ""): HeatUpdate {
        val root = JSONObject(body)
        val standings = root.optString("standings")
        val surfers = Regex(
            "<div class='surfer'>(.*?)(?=<div class='surfer'>|$)",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
        )
            .findAll(standings)
            .map { parseSurfScoreSurfer(it.groupValues[1]) }
            .toList()
        val selectedIndex = surfers.indexOfFirst { it.name.contains(athleteHint, ignoreCase = true) }
            .takeIf { it >= 0 }
            ?: 0
        val selected = surfers.getOrNull(selectedIndex)
            ?: error("Não foram encontrados atletas no heat ativo")
        val latestScore = latestSurfScore(root.optString("scores"), selectedIndex)
        val minutes = root.optInt("minleft", 0).coerceAtLeast(0)
        val seconds = root.optInt("secleft", 0).coerceIn(0, 59)

        return HeatUpdate(
            athlete = selected.name,
            position = selected.position,
            lastScore = latestScore,
            scoreNeeded = selected.needed.takeIf { selected.needLabel.equals("needs", ignoreCase = true) },
            priority = selected.priority,
            timeRemaining = "%02d:%02d".format(minutes, seconds),
            heatName = root.optString("CatDescr", root.optString("category", "HEAT")),
        )
    }

    fun parseWaveLegacy(
        body: String,
        athleteHint: String = "",
        nowEpochMs: Long = System.currentTimeMillis(),
    ): HeatUpdate {
        val root = JSONObject(body)
        require(root.optString("format").startsWith("wave_legacy_event_live_state")) {
            "Resposta Wave Legacy inválida"
        }
        val heat = root.optJSONArray("live_heats")?.optJSONObject(0)
            ?: error("Não existe um heat ativo no Wave Legacy")
        val results = heat.optJSONArray("results")
            ?: error("O heat ativo não contém resultados públicos")
        val athlete = selectWaveLegacyAthlete(results, athleteHint)
            ?: error("Atleta não encontrado no heat ativo")
        val waves = athlete.optJSONArray("ondas")
        val latestScore = (0 until (waves?.length() ?: 0))
            .mapNotNull { waves?.optJSONObject(it) }
            .filter { it.optBoolean("scores_complete", it.has("score") && !it.isNull("score")) }
            .maxWithOrNull(compareBy<JSONObject> { it.optInt("numero", 0) }.thenBy { it.optInt("onda_id", 0) })
            ?.optionalDouble("score")

        val needed = if (athlete.optString("status_tipo").equals("needs", ignoreCase = true)) {
            Regex("([0-9]+(?:[.,][0-9]+)?)")
                .find(athlete.optString("status_texto"))
                ?.groupValues?.get(1)?.replace(',', '.')?.toDoubleOrNull()
        } else null

        return HeatUpdate(
            athlete = athlete.optString("nome", athleteHint.ifBlank { "ATLETA" }),
            position = athlete.optInt("rank", 1).coerceAtLeast(1),
            lastScore = latestScore,
            scoreNeeded = needed,
            priority = athlete.optInt("priority", 0).takeIf { it > 0 },
            timeRemaining = waveLegacyRemainingTime(heat, nowEpochMs),
            heatName = heat.optString("public_nome", heat.optString("nome", "HEAT")),
        )
    }

    private fun selectWaveLegacyAthlete(results: JSONArray, hint: String): JSONObject? {
        val athletes = (0 until results.length()).mapNotNull(results::optJSONObject)
        if (hint.isBlank()) return athletes.firstOrNull()
        return athletes.firstOrNull { it.optString("nome").contains(hint, ignoreCase = true) }
    }

    private fun waveLegacyRemainingTime(heat: JSONObject, nowEpochMs: Long): String {
        val start = heat.optString("inicio")
        val durationSeconds = heat.optInt("duracao_minutos", 0).coerceAtLeast(0) * 60L
        if (start.isBlank() || durationSeconds == 0L) return "--:--"
        val startEpochMs = runCatching {
            LocalDateTime.parse(start, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        }.getOrNull() ?: return "--:--"
        val remaining = (durationSeconds - ((nowEpochMs - startEpochMs) / 1000L)).coerceAtLeast(0L)
        return "%02d:%02d".format(remaining / 60L, remaining % 60L)
    }

    private data class SurfScoreSurfer(
        val name: String,
        val position: Int,
        val priority: Int?,
        val needLabel: String,
        val needed: Double?,
    )

    private fun parseSurfScoreSurfer(html: String) = SurfScoreSurfer(
        name = html.classText("name"),
        position = html.classText("place").filter(Char::isDigit).toIntOrNull() ?: 1,
        priority = html.classText("priority").filter(Char::isDigit).toIntOrNull(),
        needLabel = html.classText("needs"),
        needed = html.classText("needpts").toDoubleOrNull(),
    )

    private fun latestSurfScore(scoresHtml: String, athleteIndex: Int): Double? {
        val column = Regex(
            "<td[^>]*id='col${athleteIndex + 1}'[^>]*>(.*?)</td>",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
        ).find(scoresHtml)?.groupValues?.get(1) ?: return null
        return Regex("<div class='avg[^']*'>([0-9]+(?:\\.[0-9]+)?)</div>", RegexOption.IGNORE_CASE)
            .findAll(column)
            .mapNotNull { it.groupValues[1].toDoubleOrNull() }
            .lastOrNull()
    }

    private fun String.classText(className: String): String = Regex(
        "<div class='$className'[^>]*>(.*?)</div>",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    ).find(this)?.groupValues?.get(1)
        ?.replace(Regex("<[^>]+>"), "")
        ?.replace("&amp;", "&")
        ?.replace("&nbsp;", " ")
        ?.trim()
        .orEmpty()

    private fun selectAthlete(array: JSONArray?, hint: String): JSONObject? {
        if (array == null || array.length() == 0) return null
        if (hint.isBlank()) return array.optJSONObject(0)
        return (0 until array.length())
            .mapNotNull(array::optJSONObject)
            .firstOrNull { item ->
                item.firstString("athlete", "name", "competitor").contains(hint, ignoreCase = true)
            }
    }

    private fun JSONObject.firstString(vararg keys: String): String = keys
        .firstNotNullOfOrNull { key -> optString(key).takeIf(String::isNotBlank) }
        .orEmpty()

    private fun JSONObject.firstDouble(vararg keys: String): Double? = keys
        .firstNotNullOfOrNull { key ->
            if (!has(key) || isNull(key)) null
            else opt(key).toString().replace(',', '.').toDoubleOrNull()
        }

    private fun JSONObject.firstInt(vararg keys: String): Int? = keys
        .firstNotNullOfOrNull { key ->
            if (!has(key) || isNull(key)) null else opt(key).toString().toIntOrNull()
        }
}
