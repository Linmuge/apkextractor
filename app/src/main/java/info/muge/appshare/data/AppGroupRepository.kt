package info.muge.appshare.data

import android.content.Context
import info.muge.appshare.utils.SPUtil
import org.json.JSONArray
import org.json.JSONObject

/**
 * 应用分组仓库
 * 使用 SharedPreferences 持久化存储
 */
object AppGroupRepository {

    private const val PREFS_NAME = "app_groups"
    private const val KEY_GROUPS = "groups"

    /**
     * 获取所有分组
     */
    fun getAllGroups(context: Context): List<AppGroup> {
        return try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val json = prefs.getString(KEY_GROUPS, "[]") ?: "[]"
            parseGroupsFromJson(json)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * 保存所有分组
     */
    fun saveGroups(context: Context, groups: List<AppGroup>) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val json = serializeGroupsToJson(groups)
            prefs.edit().putString(KEY_GROUPS, json).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 添加新分组
     */
    fun addGroup(context: Context, group: AppGroup) {
        val groups = getAllGroups(context).toMutableList()
        groups.add(group)
        saveGroups(context, groups)
    }

    /**
     * 更新分组
     */
    fun updateGroup(context: Context, group: AppGroup) {
        val groups = getAllGroups(context).toMutableList()
        val index = groups.indexOfFirst { it.id == group.id }
        if (index >= 0) {
            groups[index] = group
            saveGroups(context, groups)
        }
    }

    /**
     * 删除分组
     */
    fun deleteGroup(context: Context, groupId: String) {
        val groups = getAllGroups(context).filter { it.id != groupId }
        saveGroups(context, groups)
    }

    /**
     * 获取应用所属的所有分组
     */
    fun getGroupsForPackage(context: Context, packageName: String): List<AppGroup> {
        return getAllGroups(context).filter { it.contains(packageName) }
    }

    /**
     * 将应用添加到分组
     */
    fun addPackageToGroup(context: Context, groupId: String, packageName: String) {
        val groups = getAllGroups(context).toMutableList()
        val index = groups.indexOfFirst { it.id == groupId }
        if (index >= 0) {
            groups[index] = groups[index].addPackage(packageName)
            saveGroups(context, groups)
        }
    }

    /**
     * 从分组移除应用
     */
    fun removePackageFromGroup(context: Context, groupId: String, packageName: String) {
        val groups = getAllGroups(context).toMutableList()
        val index = groups.indexOfFirst { it.id == groupId }
        if (index >= 0) {
            groups[index] = groups[index].removePackage(packageName)
            saveGroups(context, groups)
        }
    }

    /**
     * 重命名分组
     */
    fun renameGroup(context: Context, groupId: String, newName: String) {
        val groups = getAllGroups(context).toMutableList()
        val index = groups.indexOfFirst { it.id == groupId }
        if (index >= 0) {
            groups[index] = groups[index].rename(newName)
            saveGroups(context, groups)
        }
    }

    // JSON 序列化/反序列化
    private fun parseGroupsFromJson(json: String): List<AppGroup> {
        val groups = mutableListOf<AppGroup>()
        val jsonArray = JSONArray(json)
        for (i in 0 until jsonArray.length()) {
            try {
                val jsonObj = jsonArray.getJSONObject(i)
                val group = AppGroup(
                    id = jsonObj.getString("id"),
                    name = jsonObj.getString("name"),
                    color = jsonObj.optLong("color", 0xFF2196F3),
                    packageNames = parsePackageNames(jsonObj.optJSONArray("packageNames")),
                    createdAt = jsonObj.optLong("createdAt", System.currentTimeMillis()),
                    updatedAt = jsonObj.optLong("updatedAt", System.currentTimeMillis())
                )
                groups.add(group)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return groups
    }

    private fun parsePackageNames(jsonArray: JSONArray?): Set<String> {
        if (jsonArray == null) return emptySet()
        val set = mutableSetOf<String>()
        for (i in 0 until jsonArray.length()) {
            set.add(jsonArray.getString(i))
        }
        return set
    }

    private fun serializeGroupsToJson(groups: List<AppGroup>): String {
        val jsonArray = JSONArray()
        groups.forEach { group ->
            val jsonObj = JSONObject().apply {
                put("id", group.id)
                put("name", group.name)
                put("color", group.color)
                put("packageNames", JSONArray(group.packageNames))
                put("createdAt", group.createdAt)
                put("updatedAt", group.updatedAt)
            }
            jsonArray.put(jsonObj)
        }
        return jsonArray.toString()
    }
}
