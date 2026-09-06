package pt.heatlink.wear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import pt.heatlink.shared.HeatUpdate

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        HeatStore.load(this)
        setContent {
            val update by HeatStore.updates.collectAsStateWithLifecycle()
            MaterialTheme { HeatScreen(update) }
        }
    }
}

@Composable
private fun HeatScreen(update: HeatUpdate) {
    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFF03171F)).padding(horizontal = 18.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly,
    ) {
        Text(update.heatName.uppercase(), color = Color(0xFF80DEEA), fontSize = 12.sp, maxLines = 1)
        Text("${update.position}.º LUGAR", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Black)
        Metric("PRECISA", update.scoreNeeded.score(), Color(0xFFFFD54F))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Metric("ÚLTIMA", update.lastScore.score(), Color.White, Modifier.weight(1f))
            Metric("PRIOR.", update.priority?.toString() ?: "–", Color.White, Modifier.weight(1f))
        }
        Text(
            update.timeRemaining,
            modifier = Modifier.background(Color(0xFF0D3A48), RoundedCornerShape(20.dp)).padding(horizontal = 18.dp, vertical = 5.dp),
            color = Color(0xFF69F0AE),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(update.athlete.uppercase(), color = Color.LightGray, fontSize = 10.sp, maxLines = 1)
    }
}

@Composable
private fun Metric(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = Color.Gray, fontSize = 9.sp)
        Text(value, color = color, fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    }
}

private fun Double?.score(): String = this?.let { "%.2f".format(it) } ?: "–"
