package info.muge.appshare.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

/**
 * 应用变更类型
 */
enum class ChangeType {
    INSTALLED, UPDATED, UNINSTALLED
}

/**
 * 应用变更记录
 */
data class AppChangeRecord(
    val packageName: String,
    val appName: String,
    val changeType: ChangeType,
    val versionName: String? = null,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("packageName", packageName)
            put("appName", appName)
            put("changeType", changeType.name)
            put("versionName", versionName ?: "")
            put("timestamp", timestamp)
        }
    }

    companion object {
        fun fromJson(json: JSONObject): AppChangeRecord {
            return AppChangeRecord(
                packageName = json.optString("packageName", ""),
                appName = json.optString("appName", ""),
                changeType = try {
                    ChangeType.valueOf(json.optString("changeType", "INSTALLED"))
                } catch (_: Exception) {
                    ChangeType.INSTALLED
                },
                versionName = json.optString("versionName", "").ifEmpty { null },
                timestamp = json.optLong("timestamp", System.currentTimeMillis())
            )
        }
    }
}

/**
 * 应用变更记录仓库
 * 使用 SharedPreferences + JSON 序列化存储
 */
object AppChangeRepository {
    private const val PREFS_NAME = "app_change_records"
    private const val KEY_RECORDS = "records"
    private const val MAX_RECORDS = 200

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * 添加一条变更记录
     */
    fun addRecord(context: Context, record: AppChangeRecord) {
        val records = getRecords(context).toMutableList()
        records.add(0, record) // 最新的在前面
        // 限制最大数量
        while (records.size > MAX_RECORDS) {
            records.removeAt(records.size - 1)
        }
        saveRecords(context, records)
    }

    /**
     * 获取所有变更记录
     */
    fun getRecords(context: Context): List<AppChangeRecord> {
        val prefs = getPrefs(context)
        val jsonStr = prefs.getString(KEY_RECORDS, null) ?: return emptyList()
        return try {
            val array = JSONArray(jsonStr)
            (0 until array.length()).map { i ->
                AppChangeRecord.fromJson(array.getJSONObject(i))
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * 清空所有记录
     */
    fun clearRecords(context: Context) {
        getPrefs(context).edit().remove(KEY_RECORDS).apply()
    }

    /**
     * 获取记录数量
     */
    fun getRecordCount(context: Context): Int {
        return getRecords(context).size
    }

    private fun saveRecords(context: Context, records: List<AppChangeRecord>) {
        val array = JSONArray()
        records.forEach { array.put(it.toJson()) }
        getPrefs(context).edit().putString(KEY_RECORDS, array.toString()).apply()
    }
}
