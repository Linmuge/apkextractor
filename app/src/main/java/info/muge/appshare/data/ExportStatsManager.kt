package info.muge.appshare.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 导出统计记录
 */
data class ExportRecord(
    val packageName: String,
    val appName: String,
    val size: Long,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * 导出统计管理器
 * 使用 SharedPreferences 持久化导出日志
 */
object ExportStatsManager {
    private const val PREF_NAME = "export_stats"
    private const val KEY_RECORDS = "export_records"
    private const val KEY_TOTAL_COUNT = "total_count"
    private const val KEY_TOTAL_SIZE = "total_size"
    private const val MAX_RECORDS = 200 // 保留最近200条记录

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    /**
     * 记录一次导出
     */
    fun recordExport(context: Context, packageName: String, appName: String, size: Long) {
        val prefs = getPrefs(context)

        // 更新总计
        val totalCount = prefs.getLong(KEY_TOTAL_COUNT, 0) + 1
        val totalSize = prefs.getLong(KEY_TOTAL_SIZE, 0) + size

        // 追加记录
        val records = getRecordsJson(prefs)
        val record = JSONObject().apply {
            put("pkg", packageName)
            put("name", appName)
            put("size", size)
            put("time", System.currentTimeMillis())
        }
        records.put(record)

        // 限制记录数量
        while (records.length() > MAX_RECORDS) {
            records.remove(0)
        }

        prefs.edit()
            .putLong(KEY_TOTAL_COUNT, totalCount)
            .putLong(KEY_TOTAL_SIZE, totalSize)
            .putString(KEY_RECORDS, records.toString())
            .apply()
    }

    /**
     * 获取总导出次数
     */
    fun getTotalCount(context: Context): Long {
        return getPrefs(context).getLong(KEY_TOTAL_COUNT, 0)
    }

    /**
     * 获取总导出大小
     */
    fun getTotalSize(context: Context): Long {
        return getPrefs(context).getLong(KEY_TOTAL_SIZE, 0)
    }

    /**
     * 获取最常导出的 TOP10 应用
     */
    fun getTopExportedApps(context: Context, limit: Int = 10): List<Pair<String, Int>> {
        val records = getRecords(context)
        return records
            .groupBy { it.appName }
            .map { it.key to it.value.size }
            .sortedByDescending { it.second }
            .take(limit)
    }

    /**
     * 获取所有导出记录
     */
    fun getRecords(context: Context): List<ExportRecord> {
        val prefs = getPrefs(context)
        val arr = getRecordsJson(prefs)
        val list = mutableListOf<ExportRecord>()
        for (i in 0 until arr.length()) {
            try {
                val obj = arr.getJSONObject(i)
                list.add(
                    ExportRecord(
                        packageName = obj.optString("pkg", ""),
                        appName = obj.optString("name", ""),
                        size = obj.optLong("size", 0),
                        timestamp = obj.optLong("time", 0)
                    )
                )
            } catch (_: Exception) { }
        }
        return list
    }

    /**
     * 获取按月导出趋势
     */
    fun getExportTrend(context: Context): List<Pair<String, Int>> {
        val records = getRecords(context)
        val sdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        return records
            .groupBy { sdf.format(Date(it.timestamp)) }
            .entries
            .sortedBy { it.key }
            .takeLast(12)
            .map { it.key to it.value.size }
    }

    private fun getRecordsJson(prefs: SharedPreferences): JSONArray {
        return try {
            JSONArray(prefs.getString(KEY_RECORDS, "[]"))
        } catch (_: Exception) {
            JSONArray()
        }
    }
}
