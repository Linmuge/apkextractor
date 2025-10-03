package info.muge.appshare.items

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.text.TextUtils
import androidx.documentfile.provider.DocumentFile
import info.muge.appshare.utils.DocumentFileUtil
import info.muge.appshare.utils.EnvironmentUtil
import info.muge.appshare.utils.PinyinUtil
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

/**
 * 对File,DocumentFile的封装
 */
class FileItem : Comparable<FileItem> {

    companion object {
        @JvmField
        var sort_config = 0

        @JvmStatic
        @Synchronized
        fun setSort_config(value: Int) {
            sort_config = value
        }
    }

    private val context: Context?
    private var file: File? = null
    private var documentFile: DocumentFile? = null
    private val contentUri: Uri?

    /**
     * 构造一个documentFile实例的FileItem
     */
    @Throws(Exception::class)
    constructor(context: Context, treeUri: Uri, segments: String?) {
        this.context = context
        val documentFile = DocumentFile.fromTreeUri(context, treeUri)
            ?: throw Exception("Can not get documentFile by the treeUri")
        this.documentFile = DocumentFileUtil.getDocumentFileBySegments(documentFile, segments)
        this.contentUri = null
    }

    /**
     * 构造一个File 实例的FileItem
     */
    constructor(path: String) {
        this.file = File(path)
        this.context = null
        this.contentUri = null
    }

    constructor(context: Context, documentFile: DocumentFile) {
        this.context = context
        this.documentFile = documentFile
        this.contentUri = null
    }

    constructor(file: File) {
        this.file = file
        this.context = null
        this.contentUri = null
    }

    constructor(context: Context, contentUri: Uri) {
        this.context = context
        this.contentUri = contentUri
    }

    fun getName(): String {
        documentFile?.let { return it.name ?: "" }
        file?.let { return it.name }
        
        if (context != null && contentUri != null) {
            if (ContentResolver.SCHEME_FILE.equals(contentUri.scheme, ignoreCase = true)) {
                return contentUri.lastPathSegment ?: ""
            }
            val nameQueried = EnvironmentUtil.getFileNameFromContentUri(context, contentUri)
            if (!TextUtils.isEmpty(nameQueried)) return nameQueried!!
            
            val pathQueried = EnvironmentUtil.getFilePathFromContentUri(context, contentUri)
            if (pathQueried != null && !TextUtils.isEmpty(pathQueried)) {
                return File(pathQueried).name
            }
            
            try {
                val documentFile = DocumentFile.fromSingleUri(context, contentUri)
                if (documentFile != null) {
                    val fileName = documentFile.name
                    if (!TextUtils.isEmpty(fileName)) {
                        return fileName!!
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return "unknown.file"
        }
        return ""
    }

    fun isDirectory(): Boolean {
        documentFile?.let { return it.isDirectory }
        file?.let { return it.isDirectory }
        return false
    }

    fun exists(): Boolean {
        documentFile?.let { return it.exists() }
        file?.let { return it.exists() }
        return false
    }

    @Throws(Exception::class)
    fun renameTo(newName: String): Boolean {
        file?.let {
            val path = it.parent
            val destFile = if (path == null) {
                File("/$newName")
            } else {
                File("$path/$newName")
            }
            if (destFile.exists()) {
                throw Exception("${destFile.absolutePath} already exists")
            }
            if (it.renameTo(destFile)) {
                file = destFile
                return true
            } else {
                throw Exception("error renaming to ${destFile.absolutePath}")
            }
        }
        documentFile?.let {
            return it.renameTo(newName)
        }
        return false
    }

    /**
     * 此方法只针对file生效
     */
    fun mkdirs(): Boolean {
        return file?.mkdirs() ?: false
    }

    /**
     * 针对documentFile创建文件夹
     * @param name 文件夹名称
     * @return 创建的文件夹对应的documentFile
     */
    fun createDirectory(name: String): DocumentFile? {
        return documentFile?.createDirectory(name)
    }

    /**
     * 本FileItem实例存储的是否为一个documentFile实例
     * @return true-documentFile
     */
    fun isDocumentFile(): Boolean = documentFile != null

    /**
     * 本FileItem实际存储的是否为一个File实例
     * @return true-File实例
     */
    fun isFileInstance(): Boolean = file != null

    /**
     * 本FileItem是否为通过provider获取的uri实例
     */
    fun isShareUriInstance(): Boolean = contentUri != null

    /**
     * 本FileItem是否能拿到文件信息的真实路径
     * @return true 通过getPath()方法可获得文件用于展示的真实路径
     */
    fun canGetRealPath(): Boolean {
        if (isFileInstance() || isDocumentFile()) {
            return true
        }
        if (isShareUriInstance()) {
            if (context != null && contentUri != null) {
                return !TextUtils.isEmpty(EnvironmentUtil.getFilePathFromContentUri(context, contentUri))
            }
        }
        return false
    }

    /**
     * 如果为documentFile实例，则会返回以"external/"开头的片段；如果为File实例，则返回正常的完整路径
     */
    fun getPath(): String {
        documentFile?.let {
            val uriPath = it.uri.path ?: return ""
            val index = uriPath.lastIndexOf(":") + 1
            if (index <= uriPath.length) return "external/${uriPath.substring(index)}"
        }
        file?.let { return it.absolutePath }
        
        if (context != null && contentUri != null) {
            if (ContentResolver.SCHEME_FILE.equals(contentUri.scheme, ignoreCase = true)) {
                return contentUri.path ?: ""
            }
            val path = EnvironmentUtil.getFilePathFromContentUri(context, contentUri)
            if (!TextUtils.isEmpty(path)) return path!!
            return contentUri.toString()
        }
        return ""
    }

    fun delete(): Boolean {
        documentFile?.let { return it.delete() }
        file?.let { return it.delete() }
        return false
    }

    fun listFileItems(): List<FileItem> {
        val arrayList = ArrayList<FileItem>()
        documentFile?.let {
            try {
                val documentFiles = it.listFiles()
                for (documentFile in documentFiles) {
                    arrayList.add(FileItem(context!!, documentFile))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return arrayList
        }
        file?.let {
            try {
                val files = it.listFiles()
                if (files != null) {
                    for (file in files) {
                        arrayList.add(FileItem(file))
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return arrayList
        }
        return arrayList
    }

    fun length(): Long {
        try {
            documentFile?.let { return it.length() }
            file?.let { return it.length() }
            
            if (contentUri != null && context != null) {
                if (ContentResolver.SCHEME_FILE.equals(contentUri.scheme, ignoreCase = true)) {
                    return File(contentUri.path!!).length()
                }
                val length = EnvironmentUtil.getFileLengthFromContentUri(context, contentUri)
                if (length != null && !TextUtils.isEmpty(length)) {
                    return length.toLong()
                }
                val inputStream = getInputStream()
                val available = inputStream?.available() ?: 0
                inputStream?.close()
                return available.toLong()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return 0
    }

    fun lastModified(): Long {
        try {
            documentFile?.let { return it.lastModified() }
            file?.let { return it.lastModified() }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return 0
    }

    fun getParent(): FileItem? {
        file?.let {
            val parent = it.parentFile
            if (parent != null) return FileItem(parent)
        }
        documentFile?.let {
            val parent = it.parentFile
            if (parent != null) return FileItem(context!!, parent)
        }
        return null
    }

    fun isHidden(): Boolean {
        return file?.isHidden ?: false
    }

    @Throws(Exception::class)
    fun getInputStream(): InputStream? {
        documentFile?.let { return context!!.contentResolver.openInputStream(it.uri) }
        file?.let { return FileInputStream(it) }
        
        if (contentUri != null && context != null) {
            if (ContentResolver.SCHEME_FILE.equals(contentUri.scheme, ignoreCase = true)) {
                return FileInputStream(File(contentUri.path!!))
            }
            return context.contentResolver.openInputStream(contentUri)
        }
        return null
    }

    @Throws(Exception::class)
    fun getOutputStream(): OutputStream? {
        documentFile?.let { return context!!.contentResolver.openOutputStream(it.uri) }
        file?.let { return FileOutputStream(it) }
        if (contentUri != null && context != null) {
            return context.contentResolver.openOutputStream(contentUri)
        }
        return null
    }

    fun getDocumentFile(): DocumentFile? = documentFile

    fun getFile(): File? = file

    fun getContentUri(): Uri? = contentUri

    override fun compareTo(other: FileItem): Int {
        return when (sort_config) {
            0 -> {
                try {
                    PinyinUtil.getFirstSpell(getName()).lowercase()
                        .compareTo(PinyinUtil.getFirstSpell(other.getName()).lowercase())
                } catch (e: Exception) {
                    e.printStackTrace()
                    0
                }
            }
            1 -> {
                try {
                    PinyinUtil.getFirstSpell(other.getName()).lowercase()
                        .compareTo(PinyinUtil.getFirstSpell(getName()).lowercase())
                } catch (e: Exception) {
                    e.printStackTrace()
                    0
                }
            }
            else -> 0
        }
    }

    override fun toString(): String {
        documentFile?.let { return it.uri.toString() }
        file?.let { return it.absolutePath }
        return super.toString()
    }
}

