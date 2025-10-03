package info.muge.appshare.utils

import info.muge.appshare.items.FileItem
import info.muge.appshare.items.ImportItem
import java.io.InputStream
import java.util.Enumeration
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream

/**
 * Zip文件工具类
 */
object ZipFileUtil {

    @JvmStatic
    fun getZipFileInfoOfImportItem(importItem: ImportItem): ZipFileInfo? {
        val fileItem = importItem.getFileItem()
        try {
            return when {
                fileItem.isDocumentFile() || fileItem.isShareUriInstance() -> {
                    getZipFileInfoOfZipInputStream(fileItem.getInputStream()!!)
                }
                fileItem.isFileInstance() -> {
                    getZipFileInfoOfZipFile(ZipFile(fileItem.getFile()!!))
                }
                else -> null
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun getZipFileInfoOfZipFile(zipFile: ZipFile): ZipFileInfo {
        val zipFileInfo = ZipFileInfo()
        try {
            val entries: Enumeration<*> = zipFile.entries()
            while (entries.hasMoreElements()) {
                val zipEntry = entries.nextElement() as ZipEntry
                val entryPath = zipEntry.name.replace("\\*", "/")
                
                when {
                    entryPath.lowercase().startsWith("android/data") && !zipEntry.isDirectory -> {
                        zipFileInfo.addEntry(entryPath)
                        zipFileInfo.addDataSize(zipEntry.size)
                    }
                    entryPath.lowercase().startsWith("android/obb") && !zipEntry.isDirectory -> {
                        zipFileInfo.addEntry(entryPath)
                        zipFileInfo.addObbSize(zipEntry.size)
                    }
                    entryPath.lowercase().endsWith(".apk") && !entryPath.contains("/") && !zipEntry.isDirectory -> {
                        zipFileInfo.addEntry(entryPath)
                        zipFileInfo.addApkSize(zipEntry.size)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return zipFileInfo
    }

    /**
     * 获取一个zip文件中data或者obb的大小，为耗时阻塞方法
     * @return 字节
     */
    private fun getZipFileInfoOfZipInputStream(inputStream: InputStream): ZipFileInfo {
        val zipFileInfo = ZipFileInfo()
        try {
            val zipInputStream = ZipInputStream(inputStream)
            var zipEntry = zipInputStream.nextEntry
            
            while (zipEntry != null) {
                val entryPath = zipEntry.name.replace("\\*", "/")
                
                when {
                    entryPath.lowercase().startsWith("android/data") && !zipEntry.isDirectory -> {
                        zipFileInfo.addEntry(entryPath)
                        var total_this_file = 0L
                        val buffer = ByteArray(1024)
                        var len: Int
                        while (zipInputStream.read(buffer).also { len = it } != -1) {
                            total_this_file += len
                        }
                        zipFileInfo.addDataSize(total_this_file)
                    }
                    entryPath.lowercase().startsWith("android/obb") && !zipEntry.isDirectory -> {
                        zipFileInfo.addEntry(entryPath)
                        var total_this_file = 0L
                        val buffer = ByteArray(1024)
                        var len: Int
                        while (zipInputStream.read(buffer).also { len = it } != -1) {
                            total_this_file += len
                        }
                        zipFileInfo.addObbSize(total_this_file)
                    }
                    entryPath.lowercase().endsWith(".apk") && !zipEntry.isDirectory && !entryPath.contains("/") -> {
                        zipFileInfo.addEntry(entryPath)
                        var total_this_file = 0L
                        val buffer = ByteArray(1024)
                        var len: Int
                        while (zipInputStream.read(buffer).also { len = it } != -1) {
                            total_this_file += len
                        }
                        zipFileInfo.addApkSize(total_this_file)
                    }
                }
                
                zipEntry = zipInputStream.nextEntry
            }
            zipInputStream.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return zipFileInfo
    }

    /**
     * 包含此zip包中的data,obb,apk信息
     */
    class ZipFileInfo {
        private val entryPaths = ArrayList<String>()
        var dataSize = 0L
            private set
        var obbSize = 0L
            private set
        var apkSize = 0L
            private set

        internal fun addEntry(entryPath: String) {
            entryPaths.add(entryPath)
        }

        internal fun addDataSize(dataSize: Long) {
            this.dataSize += dataSize
        }

        internal fun addObbSize(obbSize: Long) {
            this.obbSize += obbSize
        }

        internal fun addApkSize(apkSize: Long) {
            this.apkSize += apkSize
        }

        fun getEntryPaths(): ArrayList<String> {
            return entryPaths
        }
    }
}

