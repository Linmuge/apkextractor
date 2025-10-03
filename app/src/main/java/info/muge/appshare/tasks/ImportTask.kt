package info.muge.appshare.tasks

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import info.muge.appshare.Constants
import info.muge.appshare.Global
import info.muge.appshare.items.FileItem
import info.muge.appshare.items.ImportItem
import info.muge.appshare.ui.ToastManager
import info.muge.appshare.utils.EnvironmentUtil
import info.muge.appshare.utils.OutputUtil
import info.muge.appshare.utils.SPUtil
import info.muge.appshare.utils.StorageUtil
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.util.zip.ZipInputStream

/**
 * 导入任务
 */
class ImportTask(
    private val context: Context,
    importItems: List<ImportItem>,
    private val callback: ImportTaskCallback?
) : Thread() {

    private val importItemArrayList = ArrayList<ImportItem>()
    
    @Volatile
    private var isInterrupted = false
    
    private var currentWritePath: String? = null
    private var currentWrtingFileItem: FileItem? = null
    private var progress = 0L
    private var progress_check_length = 0L
    private var speed_bytes = 0L
    private var speed_check_time = 0L
    private val isExternal: Boolean
    private val error_info = StringBuilder()
    private var apkUri: Uri? = null
    private var apk_num = 0

    init {
        importItemArrayList.addAll(importItems)
        isExternal = SPUtil.getIsSaved2ExternalStorage(context)
    }

    override fun run() {
        super.run()
        
        for (importItem in importItemArrayList) {
            if (isInterrupted) break
            
            try {
                val zipInputStream = ZipInputStream(importItem.getZipInputStream())
                var zipEntry = zipInputStream.nextEntry
                
                while (zipEntry != null && !isInterrupted) {
                    try {
                        val entryPath = zipEntry.name.replace("\\*", "/")
                        
                        when {
                            entryPath.lowercase().startsWith("android/data") && !zipEntry.isDirectory && importItem.importData -> {
                                unZipToFile(zipInputStream, entryPath)
                            }
                            entryPath.lowercase().startsWith("android/obb") && !zipEntry.isDirectory && importItem.importObb -> {
                                unZipToFile(zipInputStream, entryPath)
                            }
                            entryPath.lowercase().endsWith(".apk") && !zipEntry.isDirectory && !entryPath.contains("/") && importItem.importApk -> {
                                val outputStream: OutputStream
                                val fileName = entryPath.substring(entryPath.lastIndexOf("/") + 1)
                                
                                if (isExternal) {
                                    var writeFileName = getApkFileNameWithNum(fileName)
                                    var checkFile = OutputUtil.getExportPathDocumentFile(context).findFile(writeFileName)
                                    
                                    while (checkFile != null && checkFile.exists()) {
                                        apk_num++
                                        writeFileName = getApkFileNameWithNum(fileName)
                                        checkFile = OutputUtil.getExportPathDocumentFile(context).findFile(writeFileName)
                                    }
                                    
                                    val writeDocumentFile = OutputUtil.getExportPathDocumentFile(context)
                                        .createFile("application/vnd.android.package-archive", writeFileName)!!
                                    outputStream = OutputUtil.getOutputStreamForDocumentFile(context, writeDocumentFile)!!
                                    currentWritePath = "${SPUtil.getInternalSavePath()}/$writeFileName"
                                    currentWrtingFileItem = FileItem(context, writeDocumentFile)
                                    apkUri = writeDocumentFile.uri
                                } else {
                                    var writePath = "${SPUtil.getInternalSavePath()}/${getApkFileNameWithNum(fileName)}"
                                    var writeFile = File(writePath)
                                    
                                    while (writeFile.exists()) {
                                        apk_num++
                                        writeFile = File("${SPUtil.getInternalSavePath()}/${getApkFileNameWithNum(fileName)}")
                                    }
                                    
                                    outputStream = FileOutputStream(writeFile)
                                    currentWritePath = writeFile.absolutePath
                                    currentWrtingFileItem = FileItem(writeFile)
                                    apkUri = if (Build.VERSION.SDK_INT <= 23) {
                                        Uri.fromFile(writeFile)
                                    } else {
                                        EnvironmentUtil.getUriForFileByFileProvider(context, writeFile)
                                    }
                                }
                                
                                val bufferedOutputStream = BufferedOutputStream(outputStream)
                                val buffer = ByteArray(1024)
                                var len: Int
                                
                                while (zipInputStream.read(buffer).also { len = it } != -1 && !isInterrupted) {
                                    bufferedOutputStream.write(buffer, 0, len)
                                    progress += len
                                    checkSpeedAndPostToCallback(len.toLong())
                                    checkProgressAndPostToCallback()
                                }
                                
                                bufferedOutputStream.flush()
                                bufferedOutputStream.close()
                                
                                if (!isInterrupted) {
                                    currentWrtingFileItem = null
                                }
                            }
                        }
                        
                        if (!isInterrupted) {
                            zipEntry = zipInputStream.nextEntry
                        } else {
                            break
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        error_info.append(currentWritePath)
                        error_info.append(":")
                        error_info.append(e.toString())
                        error_info.append("\n\n")
                        try {
                            currentWrtingFileItem?.delete()
                        } catch (ee: Exception) {
                            ee.printStackTrace()
                        }
                    }
                }
                zipInputStream.close()
            } catch (e: Exception) {
                e.printStackTrace()
                error_info.append(importItem.getFileItem().getPath())
                error_info.append(":")
                error_info.append(e.toString())
                error_info.append("\n\n")
            }
        }
        
        if (isInterrupted) {
            try {
                currentWrtingFileItem?.delete()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else if (callback != null) {
            Global.handler.post {
                callback.onImportTaskFinished(error_info.toString())
                context.sendBroadcast(Intent(Constants.ACTION_REFRESH_IMPORT_ITEMS_LIST))
                context.sendBroadcast(Intent(Constants.ACTION_REFRESH_AVAILIBLE_STORAGE))
                
                try {
                    if (importItemArrayList.size == 1 && apkUri != null && error_info.toString().trim().isEmpty()) {
                        val intent = Intent(Intent.ACTION_VIEW)
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        intent.setDataAndType(apkUri, "application/vnd.android.package-archive")
                        context.startActivity(intent)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    ToastManager.showToast(context, e.toString(), Toast.LENGTH_SHORT)
                }
            }
        }
    }

    @Throws(Exception::class)
    private fun unZipToFile(zipInputStream: ZipInputStream, entryPath: String) {
        val folder = File("${StorageUtil.getMainExternalStoragePath()}/${entryPath.substring(0, entryPath.lastIndexOf("/"))}")
        if (!folder.exists()) {
            folder.mkdirs()
        }
        
        val writePath = "${StorageUtil.getMainExternalStoragePath()}/$entryPath"
        val writeFile = File(writePath)
        val outputStream = BufferedOutputStream(FileOutputStream(writeFile))
        currentWritePath = writePath
        currentWrtingFileItem = FileItem(writeFile)
        
        val buffer = ByteArray(1024)
        var len: Int
        
        while (zipInputStream.read(buffer).also { len = it } != -1 && !isInterrupted) {
            outputStream.write(buffer, 0, len)
            progress += len
            checkSpeedAndPostToCallback(len.toLong())
            checkProgressAndPostToCallback()
        }
        
        outputStream.flush()
        outputStream.close()
        
        if (!isInterrupted) {
            currentWrtingFileItem = null
        }
    }

    private fun checkProgressAndPostToCallback() {
        if (progress - progress_check_length > 100 * 1024) {
            progress_check_length = progress
            callback?.let {
                Global.handler.post {
                    it.onImportTaskProgress(currentWritePath ?: "", progress)
                }
            }
        }
    }

    private fun checkSpeedAndPostToCallback(speed_plus_value: Long) {
        speed_bytes += speed_plus_value
        val current = System.currentTimeMillis()
        
        if (current - speed_check_time > 1000) {
            speed_check_time = current
            val speed_post = speed_bytes
            speed_bytes = 0
            
            callback?.let {
                Global.handler.post {
                    it.onRefreshSpeed(speed_post)
                }
            }
        }
    }

    fun setInterrupted() {
        this.isInterrupted = true
    }

    private fun getApkFileNameWithNum(originName: String): String {
        val nameWithoutExt = originName.substring(0, originName.lastIndexOf("."))
        val numSuffix = if (apk_num > 0) apk_num.toString() else ""
        return "$nameWithoutExt$numSuffix.apk"
    }

    /**
     * 导入任务回调
     */
    interface ImportTaskCallback {
        fun onImportTaskStarted()
        fun onRefreshSpeed(speed: Long)
        fun onImportTaskProgress(writePath: String, progress: Long)
        fun onImportTaskFinished(errorMessage: String)
    }
}

