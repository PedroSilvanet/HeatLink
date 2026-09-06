package pt.heatlink.wear

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService
import pt.heatlink.shared.HeatProtocol
import pt.heatlink.shared.HeatUpdate

class HeatDataService : WearableListenerService() {
    override fun onDataChanged(events: DataEventBuffer) {
        events.filter { it.type == DataEvent.TYPE_CHANGED && it.dataItem.uri.path == HeatProtocol.DATA_PATH }
            .forEach { event ->
                val payload = DataMapItem.fromDataItem(event.dataItem).dataMap.getString(HeatProtocol.DATA_KEY)
                payload?.let { json ->
                    runCatching { HeatUpdate.fromJson(json) }.getOrNull()?.let { update ->
                        HeatStore.save(this, update)
                        vibrate()
                    }
                }
            }
    }

    private fun vibrate() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            getSystemService(VibratorManager::class.java).defaultVibrator
        } else {
            @Suppress("DEPRECATION") getSystemService(VIBRATOR_SERVICE) as Vibrator
        }
        vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 180, 100, 300), -1))
    }
}
