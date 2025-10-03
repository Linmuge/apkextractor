package info.muge.appshare.tasks

import info.muge.appshare.Global
import info.muge.appshare.items.ImportItem
import info.muge.appshare.utils.StorageUtil
import info.muge.appshare.utils.ZipFileUtil
import java.io.File

/**
 * 获取导入长度和重复信息任务
 */
class GetImportLengthAndDuplicateInfoTask(
    private val importItems: List<ImportItem>,
    private val zipFileInfos: List<ZipFileUtil.ZipFileInfo>,
    private val callback: GetImportLengthAndDuplicateInfoCallback?
) : Thread() {

    @Volatile
    private var isInterrupted = false

    override fun run() {
        super.run()
        
        val duplication_infos = ArrayList<String>()
        var total = 0L
        
        for (i in importItems.indices) {
            if (isInterrupted) return
            
            try {
                val importItem = importItems[i]
                val zipFileInfo = zipFileInfos[i]
                val entryPaths = zipFileInfo.getEntryPaths()
                
                if (importItem.importData) {
                    total += zipFileInfo.dataSize
                }
                if (importItem.importObb) {
                    total += zipFileInfo.obbSize
                }
                if (importItem.importApk) {
                    total += zipFileInfo.apkSize
                }
                
                for (s in entryPaths) {
                    if (!s.contains("/") && s.endsWith(".apk")) continue
                    if (!importItem.importObb && s.lowercase().startsWith("android/obb")) continue
                    if (!importItem.importData && s.lowercase().startsWith("android/data")) continue
                    
                    val exportWritingTarget = File("${StorageUtil.getMainExternalStoragePath()}/$s")
                    if (exportWritingTarget.exists()) {
                        duplication_infos.add(exportWritingTarget.absolutePath)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        val total_length = total
        if (callback != null && !isInterrupted) {
            Global.handler.post {
                callback.onCheckingFinished(duplication_infos, total_length)
            }
        }
    }

    fun setInterrupted() {
        this.isInterrupted = true
    }

    /**
     * 获取导入长度和重复信息回调
     */
    interface GetImportLengthAndDuplicateInfoCallback {
        fun onCheckingFinished(results: List<String>, total: Long)
    }
}

