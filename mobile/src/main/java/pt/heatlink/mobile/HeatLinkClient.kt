package pt.heatlink.mobile

import android.content.Context
import android.net.Uri
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.tasks.await
import pt.heatlink.shared.HeatProtocol
import pt.heatlink.shared.HeatUpdate

class HeatLinkClient(context: Context) {
    private val dataClient = Wearable.getDataClient(context)
    private val nodeClient = Wearable.getNodeClient(context)

    suspend fun send(update: HeatUpdate): Int {
        val request = PutDataMapRequest.create(HeatProtocol.DATA_PATH).apply {
            dataMap.putString(HeatProtocol.DATA_KEY, update.toJson())
            dataMap.putLong("sentAt", System.currentTimeMillis())
        }.asPutDataRequest().setUrgent()
        dataClient.putDataItem(request).await()
        return nodeClient.connectedNodes.await().size
    }

    suspend fun connectedWatchCount(): Int = nodeClient.connectedNodes.await().size
}
