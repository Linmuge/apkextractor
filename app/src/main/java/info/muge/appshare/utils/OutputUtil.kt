package info.muge.appshare.utils

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import info.muge.appshare.Constants
import info.muge.appshare.items.AppItem
import java.io.OutputStream
import java.util.Calendar

/**
 * 输出工具类
 */
object OutputUtil {

    /**
     * 为AppItem获取一个内置存储绝对写入路径
     * @param extension "apk"或者"zip"
     */
    fun getAbsoluteWritePath(context: Context, item: AppItem, extension: String, sequence_number: Int): String {
        return "${SPUtil.getInternalSavePath()}/${getWriteFileNameForAppItem(context, item, extension, sequence_number)}"
    }

    @Throws(Exception::class)
    fun getWritingDocumentFileForAppItem(
        context: Context,
        appItem: AppItem,
        extension: String,
        sequence_number: Int
    ): DocumentFile? {
        val writingFileName = getWriteFileNameForAppItem(context, appItem, extension, sequence_number)
        val parent = getExportPathDocumentFile(context)
        val documentFile = parent.findFile(writingFileName)
        if (documentFile != null && documentFile.exists()) {
            documentFile.delete()
        }
        val mimeType = if (extension.lowercase() == "apk") {
            "application/vnd.android.package-archive"
        } else {
            "application/x-zip-compressed"
        }
        return parent.createFile(mimeType, writingFileName)
    }

    /**
     * 创建一个按照命名规则命名的写入documentFile的输出流
     * @param documentFile 要写入的documentFile
     * @return 已按照命名规则的写入的documentFile输出流
     */
    @Throws(Exception::class)
    fun getOutputStreamForDocumentFile(context: Context, documentFile: DocumentFile): OutputStream? {
        return context.contentResolver.openOutputStream(documentFile.uri)
    }

    /**
     * 获取导出根目录的documentFile
     */
    @Throws(Exception::class)
    fun getExportPathDocumentFile(context: Context): DocumentFile {
        val segments = SPUtil.getInternalSavePath()
        val treeUri = Uri.parse(SPUtil.getExternalStorageUri(context))
        val rootDocumentFile = DocumentFile.fromTreeUri(context, treeUri)
            ?: throw Exception("Cannot get DocumentFile from tree URI")
        return DocumentFileUtil.getDocumentFileBySegments(rootDocumentFile, segments)
    }

    /**
     * 为一个AppItem获取一个写入的文件名，例如example.apk
     */
    fun getWriteFileNameForAppItem(context: Context, item: AppItem, extension: String, sqNum: Int): String {
        val settings = SPUtil.getGlobalSharedPreferences(context)
        
        val fontKey = if (extension.equals("apk", ignoreCase = true)) {
            Constants.PREFERENCE_FILENAME_FONT_APK
        } else {
            Constants.PREFERENCE_FILENAME_FONT_ZIP
        }
        
        val template = settings.getString(fontKey, Constants.PREFERENCE_FILENAME_FONT_DEFAULT) ?: Constants.PREFERENCE_FILENAME_FONT_DEFAULT
        
        val replacements = mapOf(
            Constants.FONT_APP_NAME to EnvironmentUtil.removeIllegalFileNameCharacters(item.getAppName()),
            Constants.FONT_APP_PACKAGE_NAME to item.getPackageName(),
            Constants.FONT_APP_VERSIONCODE to item.getVersionCode().toString(),
            Constants.FONT_APP_VERSIONNAME to item.getVersionName(),
            Constants.FONT_YEAR to EnvironmentUtil.getCurrentTimeValue(Calendar.YEAR),
            Constants.FONT_MONTH to EnvironmentUtil.getCurrentTimeValue(Calendar.MONTH),
            Constants.FONT_DAY_OF_MONTH to EnvironmentUtil.getCurrentTimeValue(Calendar.DAY_OF_MONTH),
            Constants.FONT_HOUR_OF_DAY to EnvironmentUtil.getCurrentTimeValue(Calendar.HOUR_OF_DAY),
            Constants.FONT_MINUTE to EnvironmentUtil.getCurrentTimeValue(Calendar.MINUTE),
            Constants.FONT_SECOND to EnvironmentUtil.getCurrentTimeValue(Calendar.SECOND),
            Constants.FONT_AUTO_SEQUENCE_NUMBER to sqNum.toString()
        )
        
        val result = replacements.entries.fold(template) { acc, (key, value) ->
            acc.replace(key, value)
        }
        
        return "$result.$extension"
    }
}

