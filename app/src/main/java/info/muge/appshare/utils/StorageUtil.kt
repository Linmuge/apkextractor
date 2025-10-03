package info.muge.appshare.utils

import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.StatFs
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.Locale

/**
 * 存储工具类
 */
object StorageUtil {
    
    /**
     * 获取指定path的可写入存储容量，单位字节
     */
    @JvmStatic
    fun getAvaliableSizeOfPath(path: String): Long {
        return try {
            val stat = StatFs(path)
            val version = Build.VERSION.SDK_INT
            val blockSize = if (version >= 18) stat.blockSizeLong else stat.blockSize.toLong()
            val availableBlocks = if (version >= 18) stat.availableBlocksLong else stat.availableBlocks.toLong()
            blockSize * availableBlocks
        } catch (e: Exception) {
            e.printStackTrace()
            0
        }
    }

    /**
     * 获取外部存储主路径
     */
    @JvmStatic
    fun getMainExternalStoragePath(): String {
        return try {
            Environment.getExternalStorageDirectory().absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    /**
     * 获取设备挂载的所有外部存储分区path
     */
    @JvmStatic
    fun getAvailableStoragePaths(context: Context): List<String> {
        val paths = ArrayList<String>()
        
        if (Build.VERSION.SDK_INT >= 21) {
            try {
                val files = context.getExternalFilesDirs(null)
                if (files == null) return paths
                
                for (file in files) {
                    var path = file.absolutePath.lowercase()
                    path = path.substring(0, path.indexOf("/android/data"))
                    paths.add(path)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            try {
                val mainStorage = getMainExternalStoragePath().lowercase(Locale.getDefault()).trim()
                try {
                    paths.add(mainStorage)
                } catch (e: Exception) {
                }

                val runtime = Runtime.getRuntime()
                val process = runtime.exec("mount")
                val reader = BufferedReader(InputStreamReader(process.inputStream))
                var line: String?
                
                while (reader.readLine().also { line = it } != null) {
                    if (line!!.contains("fat") || line!!.contains("fuse") || 
                        line!!.contains("ntfs") || line!!.contains("sdcardfs") || 
                        line!!.contains("fuseblk")) {
                        
                        val items = line!!.split(" ")
                        for (s in items) {
                            val trimmed = s.trim().lowercase()
                            if ((trimmed.contains(File.separator) || trimmed.contains("/")) && 
                                !paths.contains(trimmed)) {
                                paths.add(trimmed)
                            }
                        }
                    }
                }
                return paths
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return paths
    }
}

