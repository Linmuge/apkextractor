package info.muge.appshare.utils

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import info.muge.appshare.R

/**
 * DocumentFile工具类
 */
object DocumentFileUtil {

    /**
     * 通过segment片段定位到parent的指定文件夹，如果没有则尝试创建
     */
    @JvmStatic
    @Throws(Exception::class)
    fun getDocumentFileBySegments(parent: DocumentFile, segment: String?): DocumentFile {
        if (segment == null) return parent
        
        val segments = segment.split("/")
        var documentFile = parent
        
        for (i in segments.indices) {
            var lookup = documentFile.findFile(segments[i])
            if (lookup == null) {
                lookup = documentFile.createDirectory(segments[i])
            }
            if (lookup == null) {
                throw Exception("Can not create folder ${segments[i]}")
            }
            documentFile = lookup
        }
        return documentFile
    }

    /**
     * 将segments数组转换为string
     */
    @JvmStatic
    fun toSegmentString(segments: Array<Any>): String {
        val builder = StringBuilder()
        for (i in segments.indices) {
            builder.append(segments[i])
            if (i < segments.size - 1) builder.append("/")
        }
        return builder.toString()
    }

    /**
     * 获取一个documentFile用于展示的路径
     */
    @JvmStatic
    fun getDisplayPathForDocumentFile(context: Context, documentFile: DocumentFile): String {
        val uriPath = documentFile.uri.path
        if (uriPath == null) return ""
        val index = uriPath.lastIndexOf(":") + 1
        if (index <= uriPath.length) {
            return "${context.resources.getString(R.string.external_storage)}/${uriPath.substring(index)}"
        }
        return ""
    }
}

