package info.muge.appshare.tasks

import android.content.Context
import android.net.Uri
import android.text.TextUtils
import info.muge.appshare.Constants
import info.muge.appshare.Global
import info.muge.appshare.items.FileItem
import info.muge.appshare.items.ImportItem
import info.muge.appshare.utils.SPUtil
import info.muge.appshare.utils.StorageUtil
import java.util.Collections

/**
 * 刷新导入列表任务
 */
class RefreshImportListTask(
    private val context: Context,
    private val callback: RefreshImportListTaskCallback?
) : Thread() {

    override fun run() {
        val arrayList = ArrayList<ImportItem>()
        
        callback?.let {
            Global.handler.post {
                it.onRefreshStarted()
            }
        }
        
        val package_scope_value = SPUtil.getGlobalSharedPreferences(context)
            .getInt(Constants.PREFERENCE_PACKAGE_SCOPE, Constants.PREFERENCE_PACKAGE_SCOPE_DEFAULT)
        
        var fileItem: FileItem? = null
        
        when (package_scope_value) {
            Constants.PACKAGE_SCOPE_ALL -> {
                fileItem = FileItem(StorageUtil.getMainExternalStoragePath())
            }
            Constants.PACKAGE_SCOPE_EXPORTING_PATH -> {
                if (SPUtil.getIsSaved2ExternalStorage(context)) {
                    try {
                        fileItem = FileItem(
                            context,
                            Uri.parse(SPUtil.getExternalStorageUri(context)),
                            SPUtil.getInternalSavePath()
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                } else {
                    fileItem = FileItem(SPUtil.getInternalSavePath())
                }
            }
        }
        
        try {
            arrayList.addAll(getAllImportItemsFromPath(fileItem))
            
            if (!TextUtils.isEmpty(SPUtil.getExternalStorageUri(context)) && 
                package_scope_value == Constants.PACKAGE_SCOPE_ALL) {
                arrayList.addAll(
                    getAllImportItemsFromPath(
                        FileItem(
                            context,
                            Uri.parse(SPUtil.getExternalStorageUri(context)),
                            SPUtil.getInternalSavePath()
                        )
                    )
                )
            }
            
            ImportItem.sort_config = SPUtil.getGlobalSharedPreferences(context)
                .getInt(Constants.PREFERENCE_SORT_CONFIG_IMPORT_ITEMS, 0)
            Collections.sort(arrayList)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        synchronized(Global.item_list) {
            Global.item_list.clear()
            Global.item_list.addAll(arrayList)
        }
        
        HashTask.clearResultCache()
        GetSignatureInfoTask.clearCache()
        
        callback?.let {
            Global.handler.post {
                it.onRefreshCompleted(arrayList)
            }
        }
    }

    private fun getAllImportItemsFromPath(fileItem: FileItem?): ArrayList<ImportItem> {
        val arrayList = ArrayList<ImportItem>()
        
        try {
            if (fileItem == null) return arrayList
            
            if (!fileItem.isDirectory()) {
                val path = fileItem.getPath().trim().lowercase()
                if (path.endsWith(".apk") || path.endsWith(".zip") || 
                    path.endsWith(".xapk") || 
                    path.endsWith(SPUtil.getCompressingExtensionName(context).lowercase())) {
                    
                    callback?.let {
                        Global.handler.post {
                            it.onProgress(fileItem)
                        }
                    }
                    arrayList.add(ImportItem(context, fileItem))
                }
                return arrayList
            }
            
            val fileItems = fileItem.listFileItems()
            for (fileItem1 in fileItems) {
                if (fileItem1.isDirectory()) {
                    arrayList.addAll(getAllImportItemsFromPath(fileItem1))
                } else {
                    val path = fileItem1.getPath().trim().lowercase()
                    if (path.endsWith(".apk") || path.endsWith(".zip") || 
                        path.endsWith(".xapk") || 
                        path.endsWith(SPUtil.getCompressingExtensionName(context).lowercase())) {
                        
                        try {
                            callback?.let {
                                Global.handler.post {
                                    it.onProgress(fileItem1)
                                }
                            }
                            arrayList.add(ImportItem(context, fileItem1))
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        return arrayList
    }

    /**
     * 刷新导入列表任务回调
     */
    interface RefreshImportListTaskCallback {
        fun onRefreshStarted()
        fun onProgress(fileItem: FileItem)
        fun onRefreshCompleted(list: List<ImportItem>)
    }
}

