package info.muge.appshare.tasks

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import info.muge.appshare.Constants
import info.muge.appshare.items.FileItem
import info.muge.appshare.items.ImportItem
import info.muge.appshare.ui.ToastManager
import info.muge.appshare.utils.EnvironmentUtil
import info.muge.appshare.utils.OutputUtil
import info.muge.appshare.utils.SPUtil
import info.muge.appshare.utils.StorageUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.util.zip.ZipInputStream
import kotlin.coroutines.coroutineContext

/**
 * 导入任务（协程版）
 */
class ImportTask(
    private val context: Context,
    importItems: List<ImportItem>,
    private val callback: ImportTaskCallback?
) {
    private val importItemArrayList = ArrayList<ImportItem>(importItems)

    private var currentWritePath: String? = null
    private var currentWritingFileItem: FileItem? = null
    private var progress = 0L
    private var progressCheckLength = 0L
    private var speedBytes = 0L
    private var speedCheckTime = 0L
    private val isExternal: Boolean = SPUtil.getIsSaved2ExternalStorage(context)
    private val errorInfo = StringBuilder()
    private var apkUri: Uri? = null
    private var apkNum = 0

    /**
     * 执行导入（挂起函数，在 IO 线程执行）
     */
    suspend fun execute() = withContext(Dispatchers.IO) {
        for (importItem in importItemArrayList) {
            if (!coroutineContext.isActive) break

            try {
                val zipInputStream = ZipInputStream(importItem.getZipInputStream())
                var zipEntry = zipInputStream.nextEntry

                while (zipEntry != null && coroutineContext.isActive) {
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
                                        apkNum++
                                        writeFileName = getApkFileNameWithNum(fileName)
                                        checkFile = OutputUtil.getExportPathDocumentFile(context).findFile(writeFileName)
                                    }

                                    val writeDocumentFile = OutputUtil.getExportPathDocumentFile(context)
                                        .createFile("application/vnd.android.package-archive", writeFileName)!!
                                    outputStream = OutputUtil.getOutputStreamForDocumentFile(context, writeDocumentFile)!!
                                    currentWritePath = "${SPUtil.getInternalSavePath()}/$writeFileName"
                                    currentWritingFileItem = FileItem(context, writeDocumentFile)
                                    apkUri = writeDocumentFile.uri
                                } else {
                                    var writePath = "${SPUtil.getInternalSavePath()}/${getApkFileNameWithNum(fileName)}"
                                    var writeFile = File(writePath)

                                    while (writeFile.exists()) {
                                        apkNum++
                                        writeFile = File("${SPUtil.getInternalSavePath()}/${getApkFileNameWithNum(fileName)}")
                                    }

                                    outputStream = FileOutputStream(writeFile)
                                    currentWritePath = writeFile.absolutePath
                                    currentWritingFileItem = FileItem(writeFile)
                                    apkUri = if (Build.VERSION.SDK_INT <= 23) {
                                        Uri.fromFile(writeFile)
                                    } else {
                                        EnvironmentUtil.getUriForFileByFileProvider(context, writeFile)
                                    }
                                }

                                val bufferedOutputStream = BufferedOutputStream(outputStream)
                                val buffer = ByteArray(1024)
                                var len: Int

                                while (zipInputStream.read(buffer).also { len = it } != -1 && coroutineContext.isActive) {
                                    bufferedOutputStream.write(buffer, 0, len)
                                    progress += len
                                    checkSpeedAndPostToCallback(len.toLong())
                                    checkProgressAndPostToCallback()
                                }

                                bufferedOutputStream.flush()
                                bufferedOutputStream.close()

                                if (coroutineContext.isActive) {
                                    currentWritingFileItem = null
                                }
                            }
                        }

                        if (coroutineContext.isActive) {
                            zipEntry = zipInputStream.nextEntry
                        } else {
                            break
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        errorInfo.append(currentWritePath)
                        errorInfo.append(":")
                        errorInfo.append(e.toString())
                        errorInfo.append("\n\n")
                        try {
                            currentWritingFileItem?.delete()
                        } catch (ee: Exception) {
                            ee.printStackTrace()
                        }
                    }
                }
                zipInputStream.close()
            } catch (e: Exception) {
                e.printStackTrace()
                errorInfo.append(importItem.getFileItem().getPath())
                errorInfo.append(":")
                errorInfo.append(e.toString())
                errorInfo.append("\n\n")
            }
        }

        if (!coroutineContext.isActive) {
            try {
                currentWritingFileItem?.delete()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else if (callback != null) {
            withContext(Dispatchers.Main) {
                callback.onImportTaskFinished(errorInfo.toString())
                context.sendBroadcast(Intent(Constants.ACTION_REFRESH_IMPORT_ITEMS_LIST))
                context.sendBroadcast(Intent(Constants.ACTION_REFRESH_AVAILIBLE_STORAGE))

                try {
                    if (importItemArrayList.size == 1 && apkUri != null && errorInfo.toString().trim().isEmpty()) {
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
    private suspend fun unZipToFile(zipInputStream: ZipInputStream, entryPath: String) {
        val folder = File("${StorageUtil.getMainExternalStoragePath()}/${entryPath.substring(0, entryPath.lastIndexOf("/"))}")
        if (!folder.exists()) {
            folder.mkdirs()
        }

        val writePath = "${StorageUtil.getMainExternalStoragePath()}/$entryPath"
        val writeFile = File(writePath)
        val outputStream = BufferedOutputStream(FileOutputStream(writeFile))
        currentWritePath = writePath
        currentWritingFileItem = FileItem(writeFile)

        val buffer = ByteArray(1024)
        var len: Int

        while (zipInputStream.read(buffer).also { len = it } != -1 && coroutineContext.isActive) {
            outputStream.write(buffer, 0, len)
            progress += len
            checkSpeedAndPostToCallback(len.toLong())
            checkProgressAndPostToCallback()
        }

        outputStream.flush()
        outputStream.close()

        if (coroutineContext.isActive) {
            currentWritingFileItem = null
        }
    }

    private suspend fun checkProgressAndPostToCallback() {
        if (progress - progressCheckLength > 100 * 1024) {
            progressCheckLength = progress
            callback?.let {
                withContext(Dispatchers.Main) {
                    it.onImportTaskProgress(currentWritePath ?: "", progress)
                }
            }
        }
    }

    private suspend fun checkSpeedAndPostToCallback(speedPlusValue: Long) {
        speedBytes += speedPlusValue
        val current = System.currentTimeMillis()

        if (current - speedCheckTime > 1000) {
            speedCheckTime = current
            val speedPost = speedBytes
            speedBytes = 0

            callback?.let {
                withContext(Dispatchers.Main) {
                    it.onRefreshSpeed(speedPost)
                }
            }
        }
    }

    private fun getApkFileNameWithNum(originName: String): String {
        val nameWithoutExt = originName.substring(0, originName.lastIndexOf("."))
        val numSuffix = if (apkNum > 0) apkNum.toString() else ""
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
