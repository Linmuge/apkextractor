package info.muge.appshare.tasks

import info.muge.appshare.items.ImportItem
import info.muge.appshare.utils.StorageUtil
import info.muge.appshare.utils.ZipFileUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 获取导入长度和重复信息任务（协程版）
 */
class GetImportLengthAndDuplicateInfoTask(
    private val importItems: List<ImportItem>,
    private val zipFileInfos: List<ZipFileUtil.ZipFileInfo>
) {
    /**
     * 执行检查（挂起函数，在 IO 线程执行）
     * @return Pair(重复文件路径列表, 总大小)
     */
    suspend fun execute(): Pair<List<String>, Long> = withContext(Dispatchers.IO) {
        val duplicationInfos = ArrayList<String>()
        var total = 0L

        for (i in importItems.indices) {
            ensureActive()

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
                        duplicationInfos.add(exportWritingTarget.absolutePath)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        Pair(duplicationInfos, total)
    }

    /**
     * 获取导入长度和重复信息回调（保留向后兼容）
     */
    interface GetImportLengthAndDuplicateInfoCallback {
        fun onCheckingFinished(results: List<String>, total: Long)
    }
}
