package info.muge.appshare

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import info.muge.appshare.items.AppItem
import info.muge.appshare.items.ImportItem
import info.muge.appshare.tasks.ExportTask
import info.muge.appshare.tasks.GetImportLengthAndDuplicateInfoTask
import info.muge.appshare.tasks.ImportTask
import info.muge.appshare.ui.ToastManager
import info.muge.appshare.ui.dialogs.GlobalDialogManager
import info.muge.appshare.utils.DocumentFileUtil
import info.muge.appshare.utils.EnvironmentUtil
import info.muge.appshare.utils.OutputUtil
import info.muge.appshare.utils.SPUtil
import info.muge.appshare.utils.toast
import info.muge.appshare.utils.ZipFileUtil
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File
import java.util.Collections

/**
 * 全局工具类
 */
object Global {

    /**
     * 全局Handler，用于向主UI线程发送消息
     */
    val handler = Handler(Looper.getMainLooper())

    /**
     * 用于持有对读取出的list的引用
     * 注意：此列表通过 synchronizedList 保证线程安全，
     * 但遍历时仍需手动 synchronized(app_list) 加锁
     */
    val app_list: MutableList<AppItem> = Collections.synchronizedList(ArrayList())

    /**
     * 导出目录下的文件list引用
     * 注意：此列表通过 synchronizedList 保证线程安全，
     * 但遍历时仍需手动 synchronized(item_list) 加锁
     */
    val item_list: MutableList<ImportItem> = Collections.synchronizedList(ArrayList())

    fun showRequestingWritePermissionSnackBar(activity: Activity) {
        val snackbar = Snackbar.make(
            activity.findViewById(android.R.id.content),
            activity.resources.getString(R.string.permission_write),
            Snackbar.LENGTH_SHORT
        )
        snackbar.setAction(activity.resources.getString(R.string.permission_grant)) {
            val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            intent.data = Uri.fromParts("package", activity.packageName, null)
            activity.startActivity(intent)
        }
        snackbar.show()
    }

    /**
     * 导出任务完成监听器
     */
    interface ExportTaskFinishedListener {
        fun onFinished(error_message: String)
    }

    /**
     * 选择data,obb项，确认重复文件，导出list集合中的应用，并向activity显示一个dialog，传入接口来监听完成回调（在主线程）
     * @param list AppItem的副本，当check_data_obb值为true时无需初始，false时须提前设置好data,obb值
     * @param check_data_obb 传入true 则会执行一次data,obb检查（list中没有设置data,obb值）
     */
    fun checkAndExportCertainAppItemsToSetPathWithoutShare(
        activity: Activity,
        list: List<AppItem>,
        check_data_obb: Boolean,
        listener: ExportTaskFinishedListener?
    ) {
        if (list.isEmpty()) return

        if (check_data_obb) {
            GlobalDialogManager.showDataObbSelection(
                title = activity.getString(R.string.dialog_data_obb_title),
                dataLabel = activity.getString(R.string.dialog_data_obb_export_data),
                obbLabel = activity.getString(R.string.dialog_data_obb_export_obb),
                onConfirm = { exportData, exportObb ->
                    list.forEach {
                        it.exportData = exportData
                        it.exportObb = exportObb
                    }
                    val duplicatedInfo = getDuplicatedFileInfo(activity, list)
                    if (duplicatedInfo.trim().isNotEmpty()) {
                        GlobalDialogManager.showConfirm(
                            title = activity.resources.getString(R.string.dialog_duplicate_title),
                            message = "${activity.resources.getString(R.string.dialog_duplicate_msg)}$duplicatedInfo",
                            onConfirm = { exportCertainAppItemsToSetPathAndShare(activity, list, listener) }
                        )
                    } else {
                        exportCertainAppItemsToSetPathAndShare(activity, list, listener)
                    }
                }
            )
        } else {
            val duplicatedInfo = getDuplicatedFileInfo(activity, list)
            if (duplicatedInfo.trim().isNotEmpty()) {
                GlobalDialogManager.showConfirm(
                    title = activity.resources.getString(R.string.dialog_duplicate_title),
                    message = "${activity.resources.getString(R.string.dialog_duplicate_msg)}$duplicatedInfo",
                    onConfirm = { exportCertainAppItemsToSetPathAndShare(activity, list, listener) }
                )
                return
            }
            exportCertainAppItemsToSetPathAndShare(activity, list, listener)
        }
    }

    /**
     * 导出list集合中的应用，并向activity显示一个dialog，传入接口来监听完成回调（在主线程）
     */
    private fun exportCertainAppItemsToSetPathAndShare(
        activity: Activity,
        export_list: List<AppItem>,
        listener: ExportTaskFinishedListener?
    ) {
        val task = ExportTask(activity, export_list, null)
        var exportJob: Job? = null

        GlobalDialogManager.showProgress(
            title = activity.getString(R.string.dialog_exporting),
            indeterminate = false,
            cancelText = activity.getString(R.string.dialog_export_stop),
            onCancel = {
                exportJob?.cancel()
                GlobalDialogManager.dismiss()
            }
        )
        GlobalDialogManager.updateProgress(0, export_list.size)

        task.setExportProgressListener(object : ExportTask.ExportProgressListener {
            override fun onExportAppItemStarted(order: Int, item: AppItem, total: Int, write_path: String) {
                GlobalDialogManager.updateProgress(
                    progress = order,
                    total = total,
                    message = "${item.getAppName()}\n$write_path"
                )
            }

            override fun onExportProgressUpdated(current: Long, total: Long, write_path: String) {}
            override fun onExportZipProgressUpdated(write_path: String) {}
            override fun onExportSpeedUpdated(speed: Long) {}

            override fun onExportTaskFinished(fileItems: List<info.muge.appshare.items.FileItem>, error_message: String) {
                GlobalDialogManager.dismiss()
                listener?.onFinished(error_message)
            }
        })

        exportJob = (activity as androidx.lifecycle.LifecycleOwner).lifecycleScope.launch {
            task.execute()
        }
    }

    /**
     * 通过包名获取指定list中的item（忽略大小写）
     */
    fun getAppItemByPackageNameFromList(list: List<AppItem>, package_name: String): AppItem? {
        val target = package_name.trim().lowercase()
        return synchronized(list) {
            list.firstOrNull {
                it.getPackageName().trim().lowercase() == target
            }
        }
    }

    /**
     * 通过FileItem的path从指定list中取出ImportItem（忽略大小写）
     */
    fun getImportItemByFileItemPath(list: List<ImportItem>, path: String): ImportItem? {
        return synchronized(list) {
            list.firstOrNull {
                it.getFileItem().getPath().equals(path, ignoreCase = true)
            }
        }
    }

    private fun getDuplicatedFileInfo(context: Context, items: List<AppItem>): String {
        if (items.isEmpty()) return ""
        
        val builder = StringBuilder()
        
        for (i in items.indices) {
            val item = items[i]
            val extension = if (item.exportData || item.exportObb) {
                SPUtil.getCompressingExtensionName(context)
            } else {
                "apk"
            }
            val file = File(OutputUtil.getAbsoluteWritePath(context, item, extension, i + 1))
            if (file.exists()) {
                builder.append(file.absolutePath)
                builder.append("\n\n")
            }
        }
        
        return builder.toString()
    }

    /**
     * 分享指定item应用
     * @param items 传入AppItem的副本，data obb为false
     */
    fun shareCertainAppsByItems(activity: Activity, items: List<AppItem>) {
        if (items.isEmpty()) return
        val uris = ArrayList<Uri>()
        for (item in items) {
            val file = File(item.getSourcePath())
            if (file.exists()) {
                uris.add(EnvironmentUtil.getUriForFileByFileProvider(activity, file))
            }
        }
        shareCertainFiles(activity, uris, activity.getString(R.string.share_title))
    }

    /**
     * 展示查重对话框，启动导入流程
     */
    fun showCheckingDuplicationDialogAndStartImporting(
        activity: Activity,
        importItems: List<ImportItem>,
        zipFileInfos: List<ZipFileUtil.ZipFileInfo>,
        callback: ImportTaskFinishedCallback?
    ) {
        var infoJob: Job? = null

        infoJob = (activity as androidx.lifecycle.LifecycleOwner).lifecycleScope.launch {
            val infoTask = GetImportLengthAndDuplicateInfoTask(importItems, zipFileInfos)
            val (duplicationInfos, total) = infoTask.execute()

            GlobalDialogManager.dismiss()

            var importJob: Job? = null
            GlobalDialogManager.showProgress(
                title = activity.getString(R.string.dialog_importing),
                indeterminate = false,
                cancelText = activity.getString(R.string.word_stop),
                onCancel = {
                    importJob?.cancel()
                    GlobalDialogManager.dismiss()
                }
            )
            GlobalDialogManager.updateProgress(0, total.toInt())

            val importTaskCallback = object : ImportTask.ImportTaskCallback {
                override fun onImportTaskStarted() {}
                override fun onRefreshSpeed(speed: Long) {}

                override fun onImportTaskProgress(writePath: String, progress: Long) {
                    GlobalDialogManager.updateProgress(
                        progress = progress.toInt(),
                        total = total.toInt(),
                        message = writePath
                    )
                }

                override fun onImportTaskFinished(errorMessage: String) {
                    GlobalDialogManager.dismiss()
                    callback?.onImportFinished(errorMessage)
                }
            }

            val importTask = ImportTask(activity, importItems, importTaskCallback)

            if (duplicationInfos.isEmpty()) {
                importJob = (activity as androidx.lifecycle.LifecycleOwner).lifecycleScope.launch {
                    importTask.execute()
                }
            } else {
                val stringBuilder = StringBuilder()
                var checkingIndex = duplicationInfos.size
                var unListed = 0

                if (checkingIndex > 100) {
                    unListed = checkingIndex - 100
                    checkingIndex = 100
                }

                for (i in 0 until checkingIndex) {
                    stringBuilder.append(duplicationInfos[i])
                    stringBuilder.append("\n\n")
                }

                if (unListed > 0) {
                    stringBuilder.append("+")
                    stringBuilder.append(unListed)
                    stringBuilder.append(activity.resources.getString(R.string.dialog_import_duplicate_more))
                }

                GlobalDialogManager.dismiss()
                GlobalDialogManager.showConfirm(
                    title = activity.resources.getString(R.string.dialog_import_duplicate_title),
                    message = "${activity.resources.getString(R.string.dialog_import_duplicate_message)}$stringBuilder",
                    onConfirm = {
                        GlobalDialogManager.showProgress(
                            title = activity.getString(R.string.dialog_importing),
                            indeterminate = false,
                            cancelText = activity.getString(R.string.word_stop),
                            onCancel = {
                                importJob?.cancel()
                                GlobalDialogManager.dismiss()
                            }
                        )
                        GlobalDialogManager.updateProgress(0, total.toInt())
                        importJob = (activity as androidx.lifecycle.LifecycleOwner).lifecycleScope.launch {
                            importTask.execute()
                        }
                    }
                )
            }
        }

        GlobalDialogManager.showProgress(
            title = activity.getString(R.string.dialog_wait),
            indeterminate = true,
            cancelText = activity.getString(R.string.dialog_button_cancel),
            onCancel = {
                GlobalDialogManager.dismiss()
                infoJob?.cancel()
            }
        )
    }

    /**
     * 导入任务完成回调
     */
    interface ImportTaskFinishedCallback {
        fun onImportFinished(error_message: String)
    }

    fun shareImportItems(activity: Activity, importItems: List<ImportItem>) {
        if (importItems.isEmpty()) return
        val uris = ArrayList<Uri>()
        for (item in importItems) {
            val uri = item.getUri()
            if (uri != null) {
                uris.add(uri)
            }
        }
        shareCertainFiles(activity, uris, activity.getString(R.string.share_title))
    }

    /**
     * 执行分享应用操作
     */
    fun shareCertainFiles(context: Context, uris: List<Uri>, title: String) {
        if (uris.isEmpty()) return

        val intent = Intent()
        intent.type = "application/x-zip-compressed"

        if (uris.size > 1) {
            intent.action = Intent.ACTION_SEND_MULTIPLE
            intent.putExtra(Intent.EXTRA_STREAM, ArrayList(uris))
        } else {
            intent.action = Intent.ACTION_SEND
            intent.putExtra(Intent.EXTRA_STREAM, uris[0])
        }

        intent.putExtra(Intent.EXTRA_SUBJECT, title)
        intent.putExtra(Intent.EXTRA_TEXT, title)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)

        try {
            context.startActivity(Intent.createChooser(intent, title))
        } catch (e: Exception) {
            e.printStackTrace()
            ToastManager.showToast(context, e.toString(), Toast.LENGTH_SHORT)
        }
    }

    /**
     * 通过系统分享接口分享本应用
     */
    fun shareThisApp(context: Context) {
        try {
            val applicationInfo = context.packageManager.getApplicationInfo(context.packageName, 0)
            val title = context.resources.getString(R.string.share_title)
            val intent = Intent(Intent.ACTION_SEND)
            intent.type = "application/vnd.android.package-archive"
            intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(File(applicationInfo.sourceDir)))
            intent.putExtra(Intent.EXTRA_SUBJECT, title)
            intent.putExtra(Intent.EXTRA_TEXT, title)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            context.startActivity(Intent.createChooser(intent, title))
        } catch (e: Exception) {
            e.printStackTrace()
            ToastManager.showToast(context, e.toString(), Toast.LENGTH_SHORT)
        }
    }
}
