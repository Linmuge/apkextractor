package info.muge.appshare.tasks

import android.content.Context
import android.content.Intent
import androidx.documentfile.provider.DocumentFile
import info.muge.appshare.Constants
import info.muge.appshare.Global
import info.muge.appshare.items.AppItem
import info.muge.appshare.items.FileItem
import info.muge.appshare.utils.EnvironmentUtil
import info.muge.appshare.utils.FileUtil
import info.muge.appshare.utils.OutputUtil
import info.muge.appshare.utils.SPUtil
import info.muge.appshare.utils.StorageUtil
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * 导出任务
 * @param list 要导出的AppItem集合
 * @param callback 任务进度回调，在主UI线程
 */
class ExportTask(
    private val context: Context,
    private val list: List<AppItem>,
    private var listener: ExportProgressListener?
) : Thread() {

    /**
     * 本次导出任务的目的存储路径是否为外置存储
     */
    private val isExternal: Boolean = SPUtil.getIsSaved2ExternalStorage(context)
    
    @Volatile
    private var isInterrupted = false
    private var progress = 0L
    private var total = 0L
    private var progress_check_zip = 0L
    private var zipTime = 0L
    private var zipWriteLength_second = 0L

    private var currentWritingFile: FileItem? = null
    private var currentWritingPath: String? = null

    private val write_paths = ArrayList<FileItem>()
    private val error_message = StringBuilder()

    fun setExportProgressListener(listener: ExportProgressListener) {
        this.listener = listener
    }

    override fun run() {
        try {
            // 初始化File导出路径
            if (!isExternal) {
                val export_path = File(SPUtil.getInternalSavePath())
                if (!export_path.exists()) {
                    export_path.mkdirs()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            listener?.let {
                Global.handler.post {
                    it.onExportTaskFinished(ArrayList(), e.toString())
                }
            }
            return
        }

        total = getTotalLength()
        var progress_check_apk = 0L
        var bytesPerSecond = 0L
        var startTime = System.currentTimeMillis()

        for (i in list.indices) {
            if (isInterrupted) break
            
            try {
                val item = list[i]
                val order_this_loop = i + 1

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

                    postCallback2Listener {
                        listener?.onExportAppItemStarted(order_this_loop, item, list.size, currentWritingPath.toString())
                    }

                    val inputStream = FileInputStream(item.getSourcePath())
                    val out = BufferedOutputStream(outputStream)

                    val buffer = ByteArray(1024 * 10)
                    var byteread: Int
                    
                    while (inputStream.read(buffer).also { byteread = it } != -1 && !isInterrupted) {
                        out.write(buffer, 0, byteread)
                        progress += byteread
                        bytesPerSecond += byteread
                        
                        val endTime = System.currentTimeMillis()
                        if (endTime - startTime > 1000) {
                            startTime = endTime
                            val speed = bytesPerSecond
                            bytesPerSecond = 0
                            
                            postCallback2Listener {
                                listener?.onExportSpeedUpdated(speed)
                            }
                        }

                        if (progress - progress_check_apk > 100 * 1024) {
                            progress_check_apk = progress
                            postCallback2Listener {
                                listener?.onExportProgressUpdated(progress, total, currentWritingPath.toString())
                            }
                        }
                    }
                    
                    out.flush()
                    inputStream.close()
                    out.close()
                    write_paths.add(currentWritingFile!!)
                    if (!isInterrupted) currentWritingFile = null
                } else {
                    // Export ZIP (Contains Base APK + Splits + Data/Obb)
                    val outputStream: OutputStream
                    val ext = if (hasSplits) "apks" else SPUtil.getCompressingExtensionName(context)
                    if (isExternal) {
                        val documentFile = OutputUtil.getWritingDocumentFileForAppItem(
                            context, item, ext, i + 1
                        )!!
                        currentWritingFile = FileItem(context, documentFile)
                        currentWritingPath = "${SPUtil.getInternalSavePath()}/${documentFile.name}"
                        outputStream = OutputUtil.getOutputStreamForDocumentFile(context, documentFile)!!
                    } else {
                        val writePath = OutputUtil.getAbsoluteWritePath(
                            context, item, ext, i + 1
                        )
                        currentWritingFile = FileItem(writePath)
                        currentWritingPath = writePath
                        outputStream = FileOutputStream(
                            File(OutputUtil.getAbsoluteWritePath(context, item, ext, i + 1))
                        )
                    }
                    
                    postCallback2Listener {
                        listener?.onExportAppItemStarted(order_this_loop, item, list.size, currentWritingFile!!.getPath())
                    }

                    val zos = ZipOutputStream(BufferedOutputStream(outputStream))
                    zos.setComment("Packaged by info.muge.appshare \nhttps://github.com/ghmxr/appshare")
                    val zip_level = SPUtil.getGlobalSharedPreferences(context)
                        .getInt(Constants.PREFERENCE_ZIP_COMPRESS_LEVEL, Constants.PREFERENCE_ZIP_COMPRESS_LEVEL_DEFAULT)

                    if (zip_level in 0..9) zos.setLevel(zip_level)

                    // Write Base APK
                    if (hasSplits) {
                         // Write Base APK as base.apk
                         writeZipFileWithName(File(item.getSourcePath()), "base.apk", zos, zip_level)
                         // Write Splits
                         for (splitPath in splits!!) {
                             val splitFile = File(splitPath)
                             writeZipFileWithName(splitFile, splitFile.name, zos, zip_level)
                         }
                    } else {
                         // Original behavior
                         writeZip(File(item.getSourcePath()), "", zos, zip_level)
                    }

                    if (item.exportData) {
                        writeZip(
                            File("${StorageUtil.getMainExternalStoragePath()}/android/data/${item.getPackageName()}"),
                            "Android/data/",
                            zos,
                            zip_level
                        )
                    }
                    if (item.exportObb) {
                        writeZip(
                            File("${StorageUtil.getMainExternalStoragePath()}/android/obb/${item.getPackageName()}"),
                            "Android/obb/",
                            zos,
                            zip_level
                        )
                    }
                    
                    zos.flush()
                    zos.close()
                    write_paths.add(currentWritingFile!!)
                    if (!isInterrupted) currentWritingFile = null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                error_message.append(currentWritingPath)
                error_message.append(":")
                error_message.append(e.toString())
                error_message.append("\n\n")
                try {
                    currentWritingFile?.delete() // 在写入中如果有异常就尝试删除这个文件，有可能是破损的
                } catch (ee: Exception) {
                }
            }
        }

        if (isInterrupted) {
            try {
                currentWritingFile?.delete() // 没有写入完成的文件为破损文件，尝试删除
            } catch (e: Exception) {
            }
        }

        // 更新导出文件到媒体库
        EnvironmentUtil.requestUpdatingMediaDatabase(context)

        postCallback2Listener {
            if (!isInterrupted) {
                listener?.onExportTaskFinished(write_paths, error_message.toString())
            }
            context.sendBroadcast(Intent(Constants.ACTION_REFRESH_IMPORT_ITEMS_LIST))
            context.sendBroadcast(Intent(Constants.ACTION_REFRESH_AVAILIBLE_STORAGE))
        }
    }

    private fun postCallback2Listener(runnable: Runnable?) {
        if (listener == null || runnable == null) return
        Global.handler.post(runnable)
    }

    /**
     * 获取本次导出的总计长度
     * @return 总长度，字节
     */
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
            // Add size of split APKs
            val splits = item.getSplitSourceDirs()
            if (splits != null) {
                for (split in splits) {
                    total += File(split).length()
                }
            }
        }
        return total
    }

    /**
     * 将本Runnable停止，删除当前正在导出而未完成的文件，使线程返回
     */
    fun setInterrupted() {
        isInterrupted = true
    }

    private fun writeZip(file: File?, parent: String, zos: ZipOutputStream, zip_level: Int) {
        if (file == null || isInterrupted) return
        if (!file.exists()) return

        if (file.isDirectory) {
            val newParent = parent + file.name + File.separator
            val files = file.listFiles()

            if (files != null && files.isNotEmpty()) {
                for (f in files) {
                    writeZip(f, newParent, zos, zip_level)
                }
            } else {
                try {
                    zos.putNextEntry(ZipEntry(newParent))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } else {
            try {
                val inputStream = FileInputStream(file)
                val zipentry = ZipEntry(parent + file.name)

                if (zip_level == Constants.ZIP_LEVEL_STORED) {
                    zipentry.method = ZipOutputStream.STORED
                    zipentry.compressedSize = file.length()
                    zipentry.size = file.length()
                    zipentry.crc = FileUtil.getCRC32FromFile(file).value
                }

                zos.putNextEntry(zipentry)
                val buffer = ByteArray(1024)
                var length: Int

                postCallback2Listener {
                    listener?.onExportZipProgressUpdated(file.absolutePath)
                }

                while (inputStream.read(buffer).also { length = it } != -1 && !isInterrupted) {
                    zos.write(buffer, 0, length)
                    progress += length
                    zipWriteLength_second += length

                    val endTime = System.currentTimeMillis()
                    if (endTime - zipTime > 1000) {
                        zipTime = endTime
                        val zip_speed = zipWriteLength_second

                        postCallback2Listener {
                            listener?.onExportSpeedUpdated(zip_speed)
                        }
                        zipWriteLength_second = 0
                    }

                    if (progress - progress_check_zip > 100 * 1024) {
                        progress_check_zip = progress
                        postCallback2Listener {
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
    }

    /**
     * 写入单个文件到 ZIP，允许自定义 ZIP 中的文件名
     */
    private fun writeZipFileWithName(file: File, entryName: String, zos: ZipOutputStream, zip_level: Int) {
        if (isInterrupted || !file.exists() || file.isDirectory) return

        try {
            val inputStream = FileInputStream(file)
            val zipentry = ZipEntry(entryName)

            if (zip_level == Constants.ZIP_LEVEL_STORED) {
                zipentry.method = ZipOutputStream.STORED
                zipentry.compressedSize = file.length()
                zipentry.size = file.length()
                zipentry.crc = FileUtil.getCRC32FromFile(file).value
            }

            zos.putNextEntry(zipentry)
            val buffer = ByteArray(1024)
            var length: Int

            postCallback2Listener {
                listener?.onExportZipProgressUpdated(file.absolutePath)
            }

            while (inputStream.read(buffer).also { length = it } != -1 && !isInterrupted) {
                zos.write(buffer, 0, length)
                progress += length
                zipWriteLength_second += length

                val endTime = System.currentTimeMillis()
                if (endTime - zipTime > 1000) {
                    zipTime = endTime
                    val zip_speed = zipWriteLength_second

                    postCallback2Listener {
                        listener?.onExportSpeedUpdated(zip_speed)
                    }
                    zipWriteLength_second = 0
                }

                if (progress - progress_check_zip > 100 * 1024) {
                    progress_check_zip = progress
                    postCallback2Listener {
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
        fun onExportTaskFinished(write_paths: List<FileItem>, error_message: String)
    }
}

