package info.muge.appshare.tasks

import info.muge.appshare.Global
import info.muge.appshare.items.AppItem
import info.muge.appshare.utils.PinyinUtil

/**
 * 搜索应用项任务
 */
class SearchAppItemTask(
    appItems: List<AppItem>,
    info: String,
    private val callback: SearchTaskCompletedCallback
) : Thread() {

    @Volatile
    private var isInterrupted = false
    private val search_info: String = info.trim().lowercase()
    private val appItemList = ArrayList<AppItem>()
    private val result_appItems = ArrayList<AppItem>()

    init {
        this.appItemList.addAll(appItems)
    }

    override fun run() {
        super.run()
        
        for (item in appItemList) {
            if (isInterrupted) {
                break
            }
            
            try {
                val b = (getFormatString(item.getAppName()).contains(search_info) ||
                        getFormatString(item.getPackageName()).contains(search_info) ||
                        getFormatString(item.getVersionName()).contains(search_info) ||
                        getFormatString(PinyinUtil.getFirstSpell(item.getAppName())).contains(search_info) ||
                        getFormatString(PinyinUtil.getFullSpell(item.getAppName())).contains(search_info) ||
                        getFormatString(PinyinUtil.getPinYin(item.getAppName())).contains(search_info)) &&
                        search_info.trim().isNotEmpty()
                
                if (b) {
                    result_appItems.add(item)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        Global.handler.post {
            if (!isInterrupted) {
                callback.onSearchTaskCompleted(result_appItems, search_info)
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
        fun onSearchTaskCompleted(appItems: List<AppItem>, keyword: String)
    }
}

