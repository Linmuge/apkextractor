package info.muge.appshare.utils

import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.util.zip.CRC32

/**
 * 文件工具类
 */
object FileUtil {

    /**
     * 获取文件，文件夹的大小，单位字节
     * @return 文件或文件夹大小，单位字节
     */
    fun getFileOrFolderSize(file: File?): Long {
        return try {
            if (file == null) return 0
            if (!file.exists()) return 0
            if (!file.isDirectory) {
                file.length()
            } else {
                var total = 0L
                val files = file.listFiles()
                if (files == null || files.isEmpty()) return 0
                for (f in files) {
                    total += getFileOrFolderSize(f)
                }
                total
            }
        } catch (e: Exception) {
            e.printStackTrace()
            0
        }
    }

    /**
     * 获取一个文件的CRC32值
     */
    @Throws(Exception::class)
    fun getCRC32FromFile(file: File): CRC32 {
        return getCRC32FromInputStream(FileInputStream(file.absolutePath))
    }

    @Throws(Exception::class)
    fun getCRC32FromInputStream(inputStream: InputStream): CRC32 {
        val crc = CRC32()
        val bytes = ByteArray(1024)
        var length: Int
        while (inputStream.read(bytes).also { length = it } != -1) {
            crc.update(bytes, 0, length)
        }
        inputStream.close()
        return crc
    }
}

