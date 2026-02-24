package info.muge.appshare.tasks

import info.muge.appshare.items.ImportItem
import info.muge.appshare.utils.PinyinUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

/**
 * 搜索包任务（协程版）
 */
class SearchPackageTask(
    importItemList: List<ImportItem>,
    info: String
) {
    private val searchInfo: String = info.trim().lowercase()
    private val importItemList = ArrayList<ImportItem>(importItemList)

    /**
     * 执行搜索（挂起函数，在 IO 线程执行）
     */
    suspend fun execute(): List<ImportItem> = withContext(Dispatchers.IO) {
        val resultItems = ArrayList<ImportItem>()

        for (importItem in importItemList) {
            ensureActive()

            try {
                val matched = (getFormatString(importItem.getItemName()).contains(searchInfo) ||
                        getFormatString(importItem.getDescription()).contains(searchInfo) ||
                        getFormatString(PinyinUtil.getFirstSpell(importItem.getItemName())).contains(searchInfo) ||
                        getFormatString(PinyinUtil.getFullSpell(importItem.getItemName())).contains(searchInfo) ||
                        getFormatString(PinyinUtil.getPinYin(importItem.getItemName())).contains(searchInfo) ||
                        getFormatString(PinyinUtil.getFirstSpell(importItem.getDescription())).contains(searchInfo) ||
                        getFormatString(PinyinUtil.getFullSpell(importItem.getDescription())).contains(searchInfo) ||
                        getFormatString(PinyinUtil.getPinYin(importItem.getDescription())).contains(searchInfo)) &&
                        searchInfo.trim().isNotEmpty()

                if (matched) {
                    resultItems.add(importItem)
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

    val keyword: String get() = searchInfo

    /**
     * 搜索任务完成回调（保留向后兼容）
     */
    interface SearchTaskCompletedCallback {
        fun onSearchTaskCompleted(importItems: List<ImportItem>, keyword: String)
    }
}
