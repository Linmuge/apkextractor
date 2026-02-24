package info.muge.appshare.tasks

import info.muge.appshare.items.FileItem
import info.muge.appshare.utils.EnvironmentUtil
import info.muge.appshare.utils.FileUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 哈希计算任务（协程版）
 */
class HashTask(
    private val fileItem: FileItem,
    private val hashType: HashType
) {
    enum class HashType {
        MD5, SHA1, SHA256, CRC32
    }

    companion object {
        private val md5Cache = HashMap<FileItem, String>()
        private val sha1Cache = HashMap<FileItem, String>()
        private val sha256Cache = HashMap<FileItem, String>()
        private val crc32Cache = HashMap<FileItem, String>()

        @Synchronized
        fun clearResultCache() {
            md5Cache.clear()
            sha1Cache.clear()
            sha256Cache.clear()
            crc32Cache.clear()
        }
    }

    /**
     * 执行哈希计算（挂起函数，在 IO 线程执行）
     */
    suspend fun execute(): String = withContext(Dispatchers.IO) {
        val cache = when (hashType) {
            HashType.MD5 -> md5Cache
            HashType.SHA1 -> sha1Cache
            HashType.SHA256 -> sha256Cache
            HashType.CRC32 -> crc32Cache
        }

        synchronized(cache) {
            cache[fileItem]?.let { return@withContext it }
        }

        val result = try {
            when (hashType) {
                HashType.MD5 -> EnvironmentUtil.hashMD5Value(fileItem.getInputStream()!!)
                HashType.SHA1 -> EnvironmentUtil.hashSHA1Value(fileItem.getInputStream()!!)
                HashType.SHA256 -> EnvironmentUtil.hashSHA256Value(fileItem.getInputStream()!!)
                HashType.CRC32 -> Integer.toHexString(
                    FileUtil.getCRC32FromInputStream(fileItem.getInputStream()!!).value.toInt()
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }

        if (result != null) {
            synchronized(cache) {
                cache[fileItem] = result
            }
        }

        result.toString()
    }

    /**
     * 哈希计算完成回调（保留向后兼容）
     */
    interface CompletedCallback {
        fun onHashCompleted(result: String)
    }
}
