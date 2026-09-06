package pt.heatlink.shared

import kotlin.test.Test
import kotlin.test.assertEquals
import java.time.LocalDateTime
import java.time.ZoneId

class HeatParserTest {
    @Test
    fun `selects configured athlete from normalized feed`() {
        val result = HeatParser.parse(
            """{"heat":{"name":"Final","timeRemaining":"04:32","athletes":[{"name":"Ana","rank":2,"latestScore":5.4,"scoreToAdvance":6.25,"priority":1}]}}""",
            "Ana",
        )

        assertEquals("Ana", result.athlete)
        assertEquals(2, result.position)
        assertEquals(6.25, result.scoreNeeded)
        assertEquals("04:32", result.timeRemaining)
    }

    @Test
    fun `round trips protocol json`() {
        val original = HeatUpdate(athlete = "Rui", position = 3, scoreNeeded = 7.1)
        assertEquals(original, HeatUpdate.fromJson(original.toJson()))
    }

    @Test
    fun `parses active SurfScores heat`() {
        val payload = """{
          "minleft":9,"secleft":5,"CatDescr":"WOMEN - QUARTER FINALS - HEAT 3",
          "standings":"<div class='surfer'><div class='place'>1<span>st</span></div><div class='name'>SARA ABIKO</div><div class='priority'>P1</div><div class='score'>11.00</div><div class='best'>6.50+4.50</div><div class='needs'>wins by</div><div class='needpts'>3.25</div></div><div class='surfer'><div class='place'>2<span>nd</span></div><div class='name'>NEYMARA CARVALHO</div><div class='priority'>P2</div><div class='score'>7.75</div><div class='best'>7.75+0.00</div><div class='needs'>needs</div><div class='needpts'>3.25</div></div>",
          "scores":"<table><tr><td id='col1' class='scorecol'><div class='avg topscore'>4.50</div><div class='avg topscore'>6.50</div></td><td id='col2' class='scorecol'><div class='avg topscore'>7.75</div></td></tr></table>"
        }"""
        val result = HeatParser.parseSurfScores(payload, "Neymara")

        assertEquals("NEYMARA CARVALHO", result.athlete)
        assertEquals(2, result.position)
        assertEquals(7.75, result.lastScore)
        assertEquals(3.25, result.scoreNeeded)
        assertEquals(2, result.priority)
        assertEquals("09:05", result.timeRemaining)
    }

    @Test
    fun `parses Wave Legacy active heat`() {
        val start = LocalDateTime.of(2026, 9, 6, 14, 0)
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val payload = """{
          "format":"wave_legacy_event_live_state_v3",
          "live_heats":[{
            "nome":"Heat 2","public_nome":"Ronda 1 - Heat 2","inicio":"2026-09-06 14:00:00","duracao_minutos":20,
            "results":[{
              "nome":"ANA SILVA","rank":2,"priority":1,"status_tipo":"needs","status_texto":"NEEDS 6.25",
              "ondas":[
                {"onda_id":10,"numero":1,"score":5.1,"scores_complete":true},
                {"onda_id":12,"numero":2,"score":7.4,"scores_complete":true}
              ]
            }]
          }]
        }"""
        val result = HeatParser.parseWaveLegacy(payload, "Ana", start + 5 * 60 * 1000)

        assertEquals("ANA SILVA", result.athlete)
        assertEquals(2, result.position)
        assertEquals(7.4, result.lastScore)
        assertEquals(6.25, result.scoreNeeded)
        assertEquals(1, result.priority)
        assertEquals("15:00", result.timeRemaining)
        assertEquals("Ronda 1 - Heat 2", result.heatName)
    }
}
