package pt.heatlink.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import pt.heatlink.shared.HeatUpdate

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme { HeatControlScreen(HeatLinkClient(this), LiveHeatSource()) }
        }
    }
}

@Composable
private fun HeatControlScreen(client: HeatLinkClient, liveSource: LiveHeatSource) {
    var url by remember { mutableStateOf("") }
    var athlete by remember { mutableStateOf("") }
    var heatName by remember { mutableStateOf("Heat") }
    var position by remember { mutableStateOf("1") }
    var lastScore by remember { mutableStateOf("") }
    var scoreNeeded by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("--:--") }
    var automatic by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Pronto") }
    val scope = rememberCoroutineScope()

    fun currentUpdate() = HeatUpdate(
        athlete = athlete.ifBlank { "ATLETA" },
        position = position.toIntOrNull() ?: 1,
        lastScore = lastScore.decimalOrNull(),
        scoreNeeded = scoreNeeded.decimalOrNull(),
        priority = priority.toIntOrNull(),
        timeRemaining = time.ifBlank { "--:--" },
        heatName = heatName.ifBlank { "HEAT" },
    )

    suspend fun send(update: HeatUpdate) {
        val watches = client.send(update)
        status = if (watches > 0) "Enviado para $watches relógio(s)" else "Guardado; nenhum relógio ligado"
    }

    LaunchedEffect(automatic, url, athlete) {
        var lastSignature: String? = null
        while (automatic && url.isNotBlank()) {
            runCatching { liveSource.fetch(url, athlete) }
                .onSuccess { update ->
                    heatName = update.heatName
                    position = update.position.toString()
                    lastScore = update.lastScore.formatScore()
                    scoreNeeded = update.scoreNeeded.formatScore()
                    priority = update.priority?.toString().orEmpty()
                    time = update.timeRemaining
                    val signature = listOf(
                        update.athlete,
                        update.position,
                        update.lastScore,
                        update.scoreNeeded,
                        update.priority,
                        update.timeRemaining,
                        update.heatName,
                    ).joinToString("|")
                    if (signature != lastSignature) {
                        send(update)
                        lastSignature = signature
                    } else {
                        status = "Ligado; sem alterações"
                    }
                }
                .onFailure { status = "Erro automático: ${it.message ?: "feed inválido"}" }
            delay(3_000)
        }
    }

    Surface(Modifier.fillMaxSize()) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Wave Legacy · HeatLink", style = MaterialTheme.typography.headlineLarge)
            Text("Enviar informação do heat para o relógio")
            OutlinedTextField(url, { url = it }, label = { Text("Link Wave Legacy ou SurfScores") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(athlete, { athlete = it }, label = { Text("Nome do atleta") }, modifier = Modifier.fillMaxWidth())
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Atualização automática (3 s)")
                Switch(automatic, { automatic = it && url.isNotBlank() })
            }
            OutlinedTextField(heatName, { heatName = it }, label = { Text("Heat") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(position, { position = it }, label = { Text("Posição") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(lastScore, { lastScore = it }, label = { Text("Última nota") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(scoreNeeded, { scoreNeeded = it }, label = { Text("Nota necessária") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(priority, { priority = it }, label = { Text("Prioridade") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(time, { time = it }, label = { Text("Tempo restante") }, modifier = Modifier.fillMaxWidth())
            Button(
                onClick = { scope.launch { runCatching { send(currentUpdate()) }.onFailure { status = "Erro: ${it.message}" } } },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("ENVIAR AGORA") }
            Text(status, color = MaterialTheme.colorScheme.primary)
            Text("Aceita o link público Event Live do Wave Legacy ou um link de evento SurfScores.", style = MaterialTheme.typography.bodySmall)
        }
    }
}

private fun String.decimalOrNull(): Double? = replace(',', '.').toDoubleOrNull()
private fun Double?.formatScore(): String = this?.let { "%.2f".format(it) }.orEmpty()
