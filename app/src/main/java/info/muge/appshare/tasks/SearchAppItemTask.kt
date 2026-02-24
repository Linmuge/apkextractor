package info.muge.appshare.tasks

import info.muge.appshare.items.AppItem
import info.muge.appshare.utils.PinyinUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

/**
 * 搜索应用项任务（协程版）
 */
class SearchAppItemTask(
    appItems: List<AppItem>,
    info: String
) {
    private val searchInfo: String = info.trim().lowercase()
    private val appItemList = ArrayList<AppItem>(appItems)

    /**
     * 执行搜索（挂起函数，在 IO 线程执行）
     * 通过 Job.cancel() 取消
     */
    suspend fun execute(): List<AppItem> = withContext(Dispatchers.IO) {
        val resultItems = ArrayList<AppItem>()

        for (item in appItemList) {
            ensureActive() // 替代 isInterrupted 检查

            try {
                val matched = (getFormatString(item.getAppName()).contains(searchInfo) ||
                        getFormatString(item.getPackageName()).contains(searchInfo) ||
                        getFormatString(item.getVersionName()).contains(searchInfo) ||
                        getFormatString(PinyinUtil.getFirstSpell(item.getAppName())).contains(searchInfo) ||
                        getFormatString(PinyinUtil.getFullSpell(item.getAppName())).contains(searchInfo) ||
                        getFormatString(PinyinUtil.getPinYin(item.getAppName())).contains(searchInfo)) &&
                        searchInfo.trim().isNotEmpty()

                if (matched) {
                    resultItems.add(item)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        resultItems
    }

    private fun getFormatString(s: String): String {
        return s.trim().lowercase()
    }

    /**
     * 搜索关键词
     */
    val keyword: String get() = searchInfo

    /**
     * 搜索任务完成回调（保留向后兼容）
     */
    interface SearchTaskCompletedCallback {
        fun onSearchTaskCompleted(appItems: List<AppItem>, keyword: String)
    }
}
