package net.yuzumone.piyo.complication

import android.graphics.drawable.Icon
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.data.SmallImage
import androidx.wear.watchface.complications.data.SmallImageType
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.yuzumone.piyo.R
import okhttp3.OkHttpClient
import okhttp3.Request
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

data class Item(val name: String?, val date: String?, val value: Any?)

/**
 * Skeleton for complication data source that returns short text.
 */
class MainComplicationService : SuspendingComplicationDataSourceService() {

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        if (type != ComplicationType.SHORT_TEXT) {
            return null
        }
        return createComplicationData("12:00", "datetime")
    }

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData {
        val url = "https://script.google.com/macros/s/A/exec"
        val req = Request.Builder()
            .url(url)
            .build()
        val client = OkHttpClient.Builder().apply {
            connectTimeout(10, TimeUnit.SECONDS)
            readTimeout(30, TimeUnit.SECONDS)
        }.build()
        val responseData = withContext(Dispatchers.IO) {
            client.newCall(req).execute().body?.string()
        }
        if (responseData == null) {
            return createComplicationData("-", "datetime")
        }

        val listType = object : TypeToken<List<Item>>() {}.type
        val data = Gson().fromJson<ArrayList<Item>>(responseData, listType)
        val latest = data.last { it.name == "ミルク" }
        val date = LocalDateTime.parse(latest.date, DateTimeFormatter.ISO_OFFSET_DATE_TIME)

        val formatter = DateTimeFormatter.ofPattern("HH:mm")
        val s = formatter.format(date)
        return createComplicationData(s, "datetime")
    }

    private fun createComplicationData(text: String, contentDescription: String): ShortTextComplicationData {
        val image = SmallImage.Builder(
            Icon.createWithResource(this, R.drawable.pediatrics), SmallImageType.ICON
        ).build()
        return ShortTextComplicationData.Builder(
            text = PlainComplicationText.Builder(text).build(),
            contentDescription = PlainComplicationText.Builder(contentDescription).build()
        ).setSmallImage(image).build()
    }
}
