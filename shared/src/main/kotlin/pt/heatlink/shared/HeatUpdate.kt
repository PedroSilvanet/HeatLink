package pt.heatlink.shared

import org.json.JSONObject

data class HeatUpdate(
    val athlete: String = "ATLETA",
    val position: Int = 1,
    val lastScore: Double? = null,
    val scoreNeeded: Double? = null,
    val priority: Int? = null,
    val timeRemaining: String = "--:--",
    val heatName: String = "HEAT",
    val updatedAtEpochMs: Long = System.currentTimeMillis(),
) {
    fun toJson(): String = JSONObject()
        .put("athlete", athlete)
        .put("position", position)
        .put("lastScore", lastScore)
        .put("scoreNeeded", scoreNeeded)
        .put("priority", priority)
        .put("timeRemaining", timeRemaining)
        .put("heatName", heatName)
        .put("updatedAtEpochMs", updatedAtEpochMs)
        .toString()

    companion object {
        fun fromJson(json: String): HeatUpdate {
            val value = JSONObject(json)
            return HeatUpdate(
                athlete = value.optString("athlete", "ATLETA"),
                position = value.optInt("position", 1).coerceAtLeast(1),
                lastScore = value.optionalDouble("lastScore"),
                scoreNeeded = value.optionalDouble("scoreNeeded"),
                priority = value.optionalInt("priority"),
                timeRemaining = value.optString("timeRemaining", "--:--"),
                heatName = value.optString("heatName", "HEAT"),
                updatedAtEpochMs = value.optLong("updatedAtEpochMs", System.currentTimeMillis()),
            )
        }

        private fun JSONObject.optionalDouble(key: String): Double? =
            if (!has(key) || isNull(key)) null else optDouble(key).takeUnless(Double::isNaN)

        private fun JSONObject.optionalInt(key: String): Int? =
            if (!has(key) || isNull(key)) null else optInt(key)
    }
}

object HeatProtocol {
    const val DATA_PATH = "/heat/update"
    const val DATA_KEY = "payload"
}
