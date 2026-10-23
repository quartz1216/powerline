package dev.quxomiya.powerline.data

import android.util.Log
import dev.quxomiya.powerline.search.SearchEngine
import dev.quxomiya.powerline.search.ShareTarget
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

// The lists kept in SharedPreferences, as JSON arrays of objects. Fields are named here, not taken
// from the classes, so R8 can rename the classes freely.
object StoredJson {

    fun engines(json: String): List<SearchEngine>? = read(json) {
        SearchEngine(getString("id"), getString("name"), optStringOrNull("alias"), getString("url"))
    }

    fun ofEngines(engines: List<SearchEngine>): String = write(engines) {
        JSONObject().put("id", id).put("name", name).put("alias", alias ?: JSONObject.NULL).put("url", url)
    }

    fun shareTargets(json: String): List<ShareTarget>? = read(json) {
        ShareTarget(getString("id"), getString("name"), getString("alias"), getString("packageName"), getString("className"))
    }

    fun ofShareTargets(targets: List<ShareTarget>): String = write(targets) {
        JSONObject().put("id", id).put("name", name).put("alias", alias)
            .put("packageName", packageName).put("className", className)
    }

    fun history(json: String): List<HistoryItem>? = read(json) {
        HistoryItem(getString("packageName"), getString("className"), getLong("timestamp"))
    }

    fun ofHistory(history: List<HistoryItem>): String = write(history) {
        JSONObject().put("packageName", packageName).put("className", className).put("timestamp", timestamp)
    }

    // null when the text is not a JSON array; an entry missing a field is dropped on its own
    private fun <T> read(json: String, item: JSONObject.() -> T): List<T>? = try {
        val array = JSONArray(json)
        (0 until array.length()).mapNotNull { i ->
            try {
                array.getJSONObject(i).item()
            } catch (e: JSONException) {
                null
            }
        }
    } catch (e: JSONException) {
        Log.w("StoredJson", "Unreadable list", e)
        null
    }

    private fun <T> write(items: List<T>, item: T.() -> JSONObject): String =
        JSONArray().apply { items.forEach { put(it.item()) } }.toString()

    private fun JSONObject.optStringOrNull(name: String): String? = if (isNull(name)) null else getString(name)
}
