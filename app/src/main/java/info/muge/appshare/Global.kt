package info.muge.appshare

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.android.material.snackbar.Snackbar
import info.muge.appshare.items.AppItem
import info.muge.appshare.items.ImportItem
import info.muge.appshare.tasks.ExportTask
import info.muge.appshare.tasks.GetImportLengthAndDuplicateInfoTask
import info.muge.appshare.tasks.ImportTask
import info.muge.appshare.ui.DataObbDialog
import info.muge.appshare.ui.ExportingDialog
import info.muge.appshare.ui.ImportingDialog
import info.muge.appshare.ui.ToastManager
import info.muge.appshare.utils.DocumentFileUtil
import info.muge.appshare.utils.OutputUtil
import info.muge.appshare.utils.SPUtil
import info.muge.appshare.utils.toast
import info.muge.appshare.utils.ZipFileUtil
import java.io.File
import java.util.Collections

/**
 * 全局工具类
 */
object Global {

    /**
     * 全局Handler，用于向主UI线程发送消息
     */
    @JvmField
    val handler = Handler(Looper.getMainLooper())

    /**
     * 用于持有对读取出的list的引用
     */
    @JvmField
    val app_list: MutableList<AppItem> = Collections.synchronizedList(ArrayList())

    /**
     * 导出目录下的文件list引用
     */
    @JvmField
    val item_list: MutableList<ImportItem> = Collections.synchronizedList(ArrayList())

    @JvmStatic
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
    @JvmStatic
    fun checkAndExportCertainAppItemsToSetPathWithoutShare(
        activity: Activity,
        list: List<AppItem>,
        check_data_obb: Boolean,
        listener: ExportTaskFinishedListener?
    ) {
        if (list.isEmpty()) return
        
        if (check_data_obb) {
            val dialog = DataObbDialog(activity, list, object : DataObbDialog.DialogDataObbConfirmedCallback {
                override fun onDialogDataObbConfirmed(export_list: List<AppItem>) {
                    val dulplicated_info = getDuplicatedFileInfo(activity, export_list)
                    if (dulplicated_info.trim().isNotEmpty()) {
                        AlertDialog.Builder(activity)
                            .setTitle(activity.resources.getString(R.string.dialog_duplicate_title))
                            .setMessage("${activity.resources.getString(R.string.dialog_duplicate_msg)}$dulplicated_info")
                            .setPositiveButton(activity.resources.getString(R.string.dialog_button_confirm)) { _, _ ->
                                exportCertainAppItemsToSetPathAndShare(activity, export_list, listener)
                            }
                            .setNegativeButton(activity.resources.getString(R.string.dialog_button_cancel), null)
                            .show()
                        return
                    }
                    exportCertainAppItemsToSetPathAndShare(activity, export_list, listener)
                }
            })
            dialog.show()
        } else {
            val dulplicated_info = getDuplicatedFileInfo(activity, list)
            if (dulplicated_info.trim().isNotEmpty()) {
                AlertDialog.Builder(activity)
                    .setTitle(activity.resources.getString(R.string.dialog_duplicate_title))
                    .setMessage("${activity.resources.getString(R.string.dialog_duplicate_msg)}$dulplicated_info")
                    .setPositiveButton(activity.resources.getString(R.string.dialog_button_confirm)) { _, _ ->
                        exportCertainAppItemsToSetPathAndShare(activity, list, listener)
                    }
                    .setNegativeButton(activity.resources.getString(R.string.dialog_button_cancel), null)
                    .show()
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
        val dialog = ExportingDialog(activity)
        val task = ExportTask(activity, export_list, null)
        
        dialog.setButton(AlertDialog.BUTTON_NEGATIVE, activity.resources.getString(R.string.dialog_export_stop)) { dialog1, _ ->
            task.setInterrupted()
            dialog1.cancel()
        }
        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(false)
        dialog.show()
        
        task.setExportProgressListener(object : ExportTask.ExportProgressListener {
            override fun onExportAppItemStarted(order: Int, item: AppItem, total: Int, write_path: String) {
                dialog.setProgressOfApp(order, total, item, write_path)
            }

            override fun onExportProgressUpdated(current: Long, total: Long, write_path: String) {
                dialog.setProgressOfWriteBytes(current, total)
            }

            override fun onExportZipProgressUpdated(write_path: String) {
                dialog.setProgressOfCurrentZipFile(write_path)
            }

            override fun onExportSpeedUpdated(speed: Long) {
                dialog.setSpeed(speed)
            }

            override fun onExportTaskFinished(fileItems: List<info.muge.appshare.items.FileItem>, error_message: String) {
                dialog.cancel()
                listener?.onFinished(error_message)
            }
        })
        task.start()
    }

    /**
     * 通过包名获取指定list中的item
     * @param list 要遍历的list
     * @param package_name 要定位的包名
     * @return 查询到的AppItem
     */
    @JvmStatic
    fun getAppItemByPackageNameFromList(list: List<AppItem>, package_name: String): AppItem? {
        for (item in list) {
            try {
                if (item.getPackageName().trim().lowercase() == package_name.trim().lowercase()) {
                    return item
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return null
    }

    /**
     * 通过FileItem的path从指定list中取出ImportItem
     * @param list 要遍历的list
     * @param path FileItem的path，参考[info.muge.appshare.items.FileItem.getPath]
     * @return 指定的ImportItem
     */
    @JvmStatic
    fun getImportItemByFileItemPath(list: List<ImportItem>, path: String): ImportItem? {
        for (importItem in list) {
            try {
                if (importItem.getFileItem().getPath().equals(path, ignoreCase = true)) {
                    return importItem
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return null
    }

    private fun getDuplicatedFileInfo(context: Context, items: List<AppItem>): String {
        if (items.isEmpty()) return ""
        
        val builder = StringBuilder()
        val external = SPUtil.getIsSaved2ExternalStorage(context)
        
        if (external) {
            for (i in items.indices) {
                val item = items[i]
                try {
                    val extension = if (item.exportData || item.exportObb) {
                        SPUtil.getCompressingExtensionName(context)
                    } else {
                        "apk"
                    }
                    val searchFile = OutputUtil.getExportPathDocumentFile(context)
                        .findFile(OutputUtil.getWriteFileNameForAppItem(context, item, extension, i))
                    
                    if (searchFile != null) {
                        builder.append(DocumentFileUtil.getDisplayPathForDocumentFile(context, searchFile))
                        builder.append("\n\n")
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } else {
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
        }
        
        return builder.toString()
    }

    /**
     * 分享指定item应用
     * @param items 传入AppItem的副本，data obb为false
     */
    @JvmStatic
    fun shareCertainAppsByItems(activity: Activity, items: List<AppItem>) {
        "分享应用List".toast()
    }

    /**
     * 展示查重对话框，启动导入流程
     */
    @JvmStatic
    fun showCheckingDuplicationDialogAndStartImporting(
        activity: Activity,
        importItems: List<ImportItem>,
        zipFileInfos: List<ZipFileUtil.ZipFileInfo>,
        callback: ImportTaskFinishedCallback?
    ) {
        val dialog_duplication_wait = AlertDialog.Builder(activity)
            .setTitle(activity.resources.getString(R.string.dialog_wait))
            .setView(LayoutInflater.from(activity).inflate(R.layout.dialog_duplication_file, null))
            .setNegativeButton(activity.resources.getString(R.string.dialog_button_cancel), null)
            .setCancelable(false)
            .show()

        val infoTask = GetImportLengthAndDuplicateInfoTask(importItems, zipFileInfos,
            object : GetImportLengthAndDuplicateInfoTask.GetImportLengthAndDuplicateInfoCallback {
                override fun onCheckingFinished(duplication_infos: List<String>, total: Long) {
                    dialog_duplication_wait.cancel()

                    val importingDialog = ImportingDialog(activity, total)
                    val importTaskCallback = object : ImportTask.ImportTaskCallback {
                        override fun onImportTaskStarted() {}

                        override fun onRefreshSpeed(speed: Long) {
                            importingDialog.setSpeed(speed)
                        }

                        override fun onImportTaskProgress(writePath: String, progress: Long) {
                            importingDialog.setProgress(progress)
                            importingDialog.setCurrentWritingName(writePath)
                        }

                        override fun onImportTaskFinished(errorMessage: String) {
                            importingDialog.cancel()
                            callback?.onImportFinished(errorMessage)
                        }
                    }

                    val importTask = ImportTask(activity, importItems, importTaskCallback)
                    importingDialog.setButton(AlertDialog.BUTTON_NEGATIVE,
                        activity.resources.getString(R.string.word_stop)) { dialog, _ ->
                        importTask.setInterrupted()
                        importingDialog.cancel()
                    }

                    if (duplication_infos.isEmpty()) {
                        importingDialog.show()
                        importTask.start()
                    } else {
                        val stringBuilder = StringBuilder()
                        var checkingIndex = duplication_infos.size
                        var unListed = 0

                        if (checkingIndex > 100) {
                            unListed = checkingIndex - 100
                            checkingIndex = 100
                        }

                        for (i in 0 until checkingIndex) {
                            stringBuilder.append(duplication_infos[i])
                            stringBuilder.append("\n\n")
                        }

                        if (unListed > 0) {
                            stringBuilder.append("+")
                            stringBuilder.append(unListed)
                            stringBuilder.append(activity.resources.getString(R.string.dialog_import_duplicate_more))
                        }

                        AlertDialog.Builder(activity)
                            .setTitle(activity.resources.getString(R.string.dialog_import_duplicate_title))
                            .setMessage("${activity.resources.getString(R.string.dialog_import_duplicate_message)}$stringBuilder")
                            .setPositiveButton(activity.resources.getString(R.string.dialog_button_confirm)) { _, _ ->
                                importingDialog.show()
                                importTask.start()
                            }
                            .setNegativeButton(activity.resources.getString(R.string.dialog_button_cancel), null)
                            .show()
                    }
                }
            })

        infoTask.start()
        dialog_duplication_wait.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener {
            dialog_duplication_wait.cancel()
            infoTask.setInterrupted()
        }
    }

    /**
     * 导入任务完成回调
     */
    interface ImportTaskFinishedCallback {
        fun onImportFinished(error_message: String)
    }

    @JvmStatic
    fun shareImportItems(activity: Activity, importItems: List<ImportItem>) {
        "分享应用".toast()
    }

    /**
     * 执行分享应用操作
     */
    @JvmStatic
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
    @JvmStatic
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

