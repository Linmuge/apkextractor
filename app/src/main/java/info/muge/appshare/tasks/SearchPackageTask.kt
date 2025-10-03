package info.muge.appshare.tasks

import info.muge.appshare.Global
import info.muge.appshare.items.ImportItem
import info.muge.appshare.utils.PinyinUtil

/**
 * 搜索包任务
 */
class SearchPackageTask(
    importItemList: List<ImportItem>,
    info: String,
    private val callback: SearchTaskCompletedCallback
) : Thread() {

    @Volatile
    private var isInterrupted = false
    private val search_info: String = info.trim().lowercase()
    private val importItemList = ArrayList<ImportItem>()
    private val result_importItems = ArrayList<ImportItem>()

    init {
        this.importItemList.addAll(importItemList)
    }

    override fun run() {
        super.run()
        
        for (importItem in importItemList) {
            if (isInterrupted) {
                result_importItems.clear()
                return
            }
            
            try {
                val b = (getFormatString(importItem.getItemName()).contains(search_info) ||
                        getFormatString(importItem.getDescription()).contains(search_info) ||
                        getFormatString(PinyinUtil.getFirstSpell(importItem.getItemName())).contains(search_info) ||
                        getFormatString(PinyinUtil.getFullSpell(importItem.getItemName())).contains(search_info) ||
                        getFormatString(PinyinUtil.getPinYin(importItem.getItemName())).contains(search_info) ||
                        getFormatString(PinyinUtil.getFirstSpell(importItem.getDescription())).contains(search_info) ||
                        getFormatString(PinyinUtil.getFullSpell(importItem.getDescription())).contains(search_info) ||
                        getFormatString(PinyinUtil.getPinYin(importItem.getDescription())).contains(search_info)) &&
                        search_info.trim().isNotEmpty()
                
                if (b) {
                    result_importItems.add(importItem)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        Global.handler.post {
            if (!isInterrupted) {
                callback.onSearchTaskCompleted(result_importItems, search_info)
            }
        }
    }

    fun setInterrupted() {
        isInterrupted = true
    }

    private fun getFormatString(s: String): String {
        return s.trim().lowercase()
    }

    /**
     * 搜索任务完成回调
     */
    interface SearchTaskCompletedCallback {
        fun onSearchTaskCompleted(importItems: List<ImportItem>, keyword: String)
    }
}

