package info.muge.appshare.tasks

import android.content.Context
import android.content.Intent
import androidx.documentfile.provider.DocumentFile
import info.muge.appshare.Constants
import info.muge.appshare.Global
import info.muge.appshare.data.ExportStatsManager
import info.muge.appshare.items.AppItem
import info.muge.appshare.items.FileItem
import info.muge.appshare.utils.EnvironmentUtil
import info.muge.appshare.utils.FileUtil
import info.muge.appshare.utils.OutputUtil
import info.muge.appshare.utils.SPUtil
import info.muge.appshare.utils.StorageUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.coroutines.coroutineContext

/**
 * 导出任务（协程版）
 * @param list 要导出的AppItem集合
 * @param maxRetries 单个文件失败时的最大重试次数
 */
class ExportTask(
    private val context: Context,
    private val list: List<AppItem>,
    private var listener: ExportProgressListener?,
    private val maxRetries: Int = 2
) {
    private val isExternal: Boolean = SPUtil.getIsSaved2ExternalStorage(context)

    private var progress = 0L
    private var total = 0L
    private var progressCheckZip = 0L
    private var zipTime = 0L
    private var zipWriteLengthSecond = 0L

    private var currentWritingFile: FileItem? = null
    private var currentWritingPath: String? = null

    private val writePaths = ArrayList<FileItem>()
    private val errorMessage = StringBuilder()

    // 记录重试次数
    private var retryCount = 0
    private val retryLog = StringBuilder()

    fun setExportProgressListener(listener: ExportProgressListener) {
        this.listener = listener
    }

    /**
     * 执行导出（挂起函数，在 IO 线程执行）
     */
    suspend fun execute() = withContext(Dispatchers.IO) {
        try {
            if (!isExternal) {
                val exportPath = File(SPUtil.getInternalSavePath())
                if (!exportPath.exists()) {
                    exportPath.mkdirs()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            postToMain {
                listener?.onExportTaskFinished(ArrayList(), e.toString())
            }
            return@withContext
        }

        total = getTotalLength()
        var progressCheckApk = 0L
        var bytesPerSecond = 0L
        var startTime = System.currentTimeMillis()

        for (i in list.indices) {
            if (!coroutineContext.isActive) break

            var success = false
            var lastException: Exception? = null
            var currentRetry = 0

            while (!success && currentRetry <= maxRetries && coroutineContext.isActive) {
                if (currentRetry > 0) {
                    retryLog.append("重试 #$currentRetry: ${list[i].getAppName()}\n")
                    postToMain {
                        listener?.onExportRetry(currentRetry, maxRetries, list[i].getAppName())
                    }
                    // 重试前等待一小段时间
                    kotlinx.coroutines.delay(500)
                }

                try {
                    val item = list[i]
                    val orderThisLoop = i + 1

                    val splits = item.getSplitSourceDirs()
                    val hasSplits = !splits.isNullOrEmpty()

                    if (!item.exportData && !item.exportObb && !hasSplits) {
                        // Export single Base APK
                        val outputStream: OutputStream
                        if (isExternal) {
                            val documentFile = OutputUtil.getWritingDocumentFileForAppItem(context, item, "apk", i + 1)!!
                            currentWritingFile = FileItem(context, documentFile)
                            currentWritingPath = "${SPUtil.getInternalSavePath()}/${documentFile.name}"
                            outputStream = OutputUtil.getOutputStreamForDocumentFile(context, documentFile)!!
                        } else {
                            val writePath = OutputUtil.getAbsoluteWritePath(context, item, "apk", i + 1)
                            currentWritingFile = FileItem(writePath)
                            currentWritingPath = writePath
                            outputStream = FileOutputStream(File(OutputUtil.getAbsoluteWritePath(context, item, "apk", i + 1)))
                        }

                        postToMain {
                            listener?.onExportAppItemStarted(orderThisLoop, item, list.size, currentWritingPath.toString())
                        }

                        val inputStream = FileInputStream(item.getSourcePath())
                        val out = BufferedOutputStream(outputStream)

                        val buffer = ByteArray(1024 * 10)
                        var byteread: Int

                        while (inputStream.read(buffer).also { byteread = it } != -1 && coroutineContext.isActive) {
                            out.write(buffer, 0, byteread)
                            progress += byteread
                            bytesPerSecond += byteread

                            val endTime = System.currentTimeMillis()
                            if (endTime - startTime > 1000) {
                                startTime = endTime
                                val speed = bytesPerSecond
                                bytesPerSecond = 0
                                postToMain {
                                    listener?.onExportSpeedUpdated(speed)
                                }
                            }

                            if (progress - progressCheckApk > 100 * 1024) {
                                progressCheckApk = progress
                                postToMain {
                                    listener?.onExportProgressUpdated(progress, total, currentWritingPath.toString())
                                }
                            }
                        }

                        out.flush()
                        inputStream.close()
                        out.close()
                        writePaths.add(currentWritingFile!!)
                        // 记录导出统计
                        ExportStatsManager.recordExport(context, item.getPackageName(), item.getAppName(), item.getSize())
                        if (coroutineContext.isActive) currentWritingFile = null
                        success = true
                    } else {
                        // Export ZIP
                        val outputStream: OutputStream
                        val ext = if (hasSplits) "apks" else SPUtil.getCompressingExtensionName(context)
                        if (isExternal) {
                            val documentFile = OutputUtil.getWritingDocumentFileForAppItem(context, item, ext, i + 1)!!
                            currentWritingFile = FileItem(context, documentFile)
                            currentWritingPath = "${SPUtil.getInternalSavePath()}/${documentFile.name}"
                            outputStream = OutputUtil.getOutputStreamForDocumentFile(context, documentFile)!!
                        } else {
                            val writePath = OutputUtil.getAbsoluteWritePath(context, item, ext, i + 1)
                            currentWritingFile = FileItem(writePath)
                            currentWritingPath = writePath
                            outputStream = FileOutputStream(File(OutputUtil.getAbsoluteWritePath(context, item, ext, i + 1)))
                        }

                        postToMain {
                            listener?.onExportAppItemStarted(orderThisLoop, item, list.size, currentWritingFile!!.getPath())
                        }

                        val zos = ZipOutputStream(BufferedOutputStream(outputStream))
                        zos.setComment("Packaged by info.muge.appshare \nhttps://github.com/ghmxr/appshare")
                        val zipLevel = SPUtil.getGlobalSharedPreferences(context)
                            .getInt(Constants.PREFERENCE_ZIP_COMPRESS_LEVEL, Constants.PREFERENCE_ZIP_COMPRESS_LEVEL_DEFAULT)

                        if (zipLevel in 0..9) zos.setLevel(zipLevel)

                        if (hasSplits) {
                            writeZip(File(item.getSourcePath()), "base.apk", zos, zipLevel, true)
                            for (splitPath in splits!!) {
                                val splitFile = File(splitPath)
                                writeZip(splitFile, splitFile.name, zos, zipLevel, true)
                            }
                        } else {
                            writeZipRecursive(File(item.getSourcePath()), "", zos, zipLevel)
                        }

                        if (item.exportData) {
                            writeZipRecursive(
                                File("${StorageUtil.getMainExternalStoragePath()}/android/data/${item.getPackageName()}"),
                                "Android/data/", zos, zipLevel
                            )
                        }
                        if (item.exportObb) {
                            writeZipRecursive(
                                File("${StorageUtil.getMainExternalStoragePath()}/android/obb/${item.getPackageName()}"),
                                "Android/obb/", zos, zipLevel
                            )
                        }

                        zos.flush()
                        zos.close()
                        writePaths.add(currentWritingFile!!)
                        // 记录导出统计
                        ExportStatsManager.recordExport(context, item.getPackageName(), item.getAppName(), item.getSize())
                        if (coroutineContext.isActive) currentWritingFile = null
                        success = true
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    lastException = e
                    try {
                        currentWritingFile?.delete()
                    } catch (_: Exception) { }
                    currentRetry++
                }
            }

            // 所有重试都失败后记录错误
            if (!success && lastException != null) {
                errorMessage.append(currentWritingPath)
                errorMessage.append(":")
                errorMessage.append(lastException.toString())
                if (currentRetry > 1) {
                    errorMessage.append(" (重试 $currentRetry 次后失败)")
                }
                errorMessage.append("\n\n")
            }
        }

        if (!coroutineContext.isActive) {
            try {
                currentWritingFile?.delete()
            } catch (_: Exception) { }
        }

        EnvironmentUtil.requestUpdatingMediaDatabase(context)

        postToMain {
            if (coroutineContext.isActive) {
                listener?.onExportTaskFinished(writePaths, errorMessage.toString())
            }
            context.sendBroadcast(Intent(Constants.ACTION_REFRESH_IMPORT_ITEMS_LIST))
            context.sendBroadcast(Intent(Constants.ACTION_REFRESH_AVAILIBLE_STORAGE))
        }
    }

    private suspend fun postToMain(block: () -> Unit) {
        withContext(Dispatchers.Main) { block() }
    }

    private fun getTotalLength(): Long {
        var total = 0L
        for (item in list) {
            total += item.getSize()
            if (item.exportData) {
                total += FileUtil.getFileOrFolderSize(
                    File("${StorageUtil.getMainExternalStoragePath()}/android/data/${item.getPackageName()}")
                )
            }
            if (item.exportObb) {
                total += FileUtil.getFileOrFolderSize(
                    File("${StorageUtil.getMainExternalStoragePath()}/android/obb/${item.getPackageName()}")
                )
            }
            val splits = item.getSplitSourceDirs()
            if (splits != null) {
                for (split in splits) {
                    total += File(split).length()
                }
            }
        }
        return total
    }

    private suspend fun writeZipRecursive(file: File?, parent: String, zos: ZipOutputStream, zipLevel: Int) {
        if (file == null || !coroutineContext.isActive) return
        if (!file.exists()) return

        if (file.isDirectory) {
            val newParent = parent + file.name + File.separator
            val files = file.listFiles()

            if (files != null && files.isNotEmpty()) {
                for (f in files) {
                    writeZipRecursive(f, newParent, zos, zipLevel)
                }
            } else {
                try {
                    zos.putNextEntry(ZipEntry(newParent))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } else {
            writeZip(file, parent + file.name, zos, zipLevel, false)
        }
    }

    private suspend fun writeZip(file: File, entryName: String, zos: ZipOutputStream, zipLevel: Int, isSingle: Boolean) {
        if (!coroutineContext.isActive || !file.exists() || file.isDirectory) return

        try {
            val inputStream = FileInputStream(file)
            val zipentry = ZipEntry(entryName)

            if (zipLevel == Constants.ZIP_LEVEL_STORED) {
                zipentry.method = ZipOutputStream.STORED
                zipentry.compressedSize = file.length()
                zipentry.size = file.length()
                zipentry.crc = FileUtil.getCRC32FromFile(file).value
            }

            zos.putNextEntry(zipentry)
            val buffer = ByteArray(1024)
            var length: Int

            postToMain {
                listener?.onExportZipProgressUpdated(file.absolutePath)
            }

            while (inputStream.read(buffer).also { length = it } != -1 && coroutineContext.isActive) {
                zos.write(buffer, 0, length)
                progress += length
                zipWriteLengthSecond += length

                val endTime = System.currentTimeMillis()
                if (endTime - zipTime > 1000) {
                    zipTime = endTime
                    val zipSpeed = zipWriteLengthSecond
                    postToMain {
                        listener?.onExportSpeedUpdated(zipSpeed)
                    }
                    zipWriteLengthSecond = 0
                }

                if (progress - progressCheckZip > 100 * 1024) {
                    progressCheckZip = progress
                    postToMain {
                        listener?.onExportProgressUpdated(progress, total, file.absolutePath)
                    }
                }
            }

            zos.flush()
            inputStream.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 导出进度监听器
     */
    interface ExportProgressListener {
        fun onExportAppItemStarted(order: Int, item: AppItem, total: Int, write_path: String)
        fun onExportProgressUpdated(current: Long, total: Long, write_path: String)
        fun onExportZipProgressUpdated(write_path: String)
        fun onExportSpeedUpdated(speed: Long)
        fun onExportRetry(currentRetry: Int, maxRetries: Int, appName: String) {}
        fun onExportTaskFinished(write_paths: List<FileItem>, error_message: String)
    }
}
