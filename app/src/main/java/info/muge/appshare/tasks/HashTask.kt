package info.muge.appshare.tasks

import info.muge.appshare.Global
import info.muge.appshare.items.FileItem
import info.muge.appshare.utils.EnvironmentUtil
import info.muge.appshare.utils.FileUtil

/**
 * 哈希计算任务
 */
class HashTask(
    private val fileItem: FileItem,
    private val hashType: HashType,
    private val callback: CompletedCallback
) : Thread() {

    enum class HashType {
        MD5, SHA1, SHA256, CRC32
    }

    companion object {
        private val md5_cache = HashMap<FileItem, String>()
        private val sha1_cache = HashMap<FileItem, String>()
        private val sha256_cache = HashMap<FileItem, String>()
        private val crc32_cache = HashMap<FileItem, String>()

        @JvmStatic
        @Synchronized
        fun clearResultCache() {
            md5_cache.clear()
            sha1_cache.clear()
            sha256_cache.clear()
            crc32_cache.clear()
        }
    }

    override fun run() {
        super.run()
        
        var result: String? = null
        
        when (hashType) {
            HashType.MD5 -> {
                synchronized(md5_cache) {
                    result = md5_cache[fileItem]
                    if (result == null) {
                        try {
                            result = EnvironmentUtil.hashMD5Value(fileItem.getInputStream()!!)
                            md5_cache[fileItem] = result!!
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
            HashType.SHA1 -> {
                synchronized(sha1_cache) {
                    result = sha1_cache[fileItem]
                    if (result == null) {
                        try {
                            result = EnvironmentUtil.hashSHA1Value(fileItem.getInputStream()!!)
                            sha1_cache[fileItem] = result!!
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
            HashType.SHA256 -> {
                synchronized(sha256_cache) {
                    result = sha256_cache[fileItem]
                    if (result == null) {
                        try {
                            result = EnvironmentUtil.hashSHA256Value(fileItem.getInputStream()!!)
                            sha256_cache[fileItem] = result!!
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
            HashType.CRC32 -> {
                synchronized(crc32_cache) {
                    result = crc32_cache[fileItem]
                    if (result == null) {
                        try {
                            result = Integer.toHexString(
                                FileUtil.getCRC32FromInputStream(fileItem.getInputStream()!!).value.toInt()
                            )
                            crc32_cache[fileItem] = result!!
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        }

        val result_final = result.toString()
        Global.handler.post {
            callback.onHashCompleted(result_final)
        }
    }

    /**
     * 哈希计算完成回调
     */
    interface CompletedCallback {
        fun onHashCompleted(result: String)
    }
}

