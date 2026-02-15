package info.muge.appshare.utils

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Bundle
import android.provider.MediaStore
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.format.Formatter
import android.text.style.ForegroundColorSpan
import android.util.TypedValue
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.annotation.ColorInt
import androidx.core.content.FileProvider
import info.muge.appshare.Constants
import info.muge.appshare.R
import info.muge.appshare.ui.ToastManager
import java.io.BufferedInputStream
import java.io.File
import java.io.InputStream
import java.security.MessageDigest
import java.security.cert.X509Certificate
import java.util.Calendar
import java.util.Locale
import java.util.jar.JarFile

/**
 * 环境工具类
 */
object EnvironmentUtil {

    @JvmStatic
    fun showInputMethod(view: View) {
        try {
            val inputMethodManager = view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            view.requestFocus()
            inputMethodManager.showSoftInput(view, 0)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @JvmStatic
    fun hideInputMethod(activity: Activity) {
        try {
            (activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                .hideSoftInputFromWindow(activity.currentFocus?.windowToken, 0)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @JvmStatic
    fun drawableToBitmap(drawable: Drawable): Bitmap {
        val bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth,
            drawable.intrinsicHeight,
            if (drawable.opacity != PixelFormat.OPAQUE) Bitmap.Config.ARGB_8888 else Bitmap.Config.RGB_565
        )
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
        drawable.draw(canvas)
        return bitmap
    }

    @JvmStatic
    fun getAppNameByPackageName(context: Context, package_name: String): String {
        try {
            val packageManager = context.packageManager
            return packageManager.getApplicationLabel(packageManager.getApplicationInfo(package_name, 0)).toString()
        } catch (ne: PackageManager.NameNotFoundException) {
            // Do nothing
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return ""
    }

    /**
     * 返回当前时间值
     * @param field 参考[Calendar.YEAR] [Calendar.MONTH] [Calendar.MINUTE]
     * [Calendar.HOUR_OF_DAY] [Calendar.MINUTE] [Calendar.SECOND]
     */
    @JvmStatic
    fun getCurrentTimeValue(field: Int): String {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = System.currentTimeMillis()
        var value = calendar.get(field)
        if (field == Calendar.MONTH) value++
        return getFormatNumberWithZero(value)
    }

    private fun getFormatNumberWithZero(value: Int): String {
        return if (value in 0..9) {
            "0$value"
        } else {
            value.toString()
        }
    }

    @JvmStatic
    fun getEmptyVariableString(value: String): String {
        var result = value
        result = result.replace(Constants.FONT_APP_NAME, "")
        result = result.replace(Constants.FONT_APP_PACKAGE_NAME, "")
        result = result.replace(Constants.FONT_APP_VERSIONNAME, "")
        result = result.replace(Constants.FONT_APP_VERSIONCODE, "")
        result = result.replace(Constants.FONT_YEAR, "")
        result = result.replace(Constants.FONT_MONTH, "")
        result = result.replace(Constants.FONT_DAY_OF_MONTH, "")
        result = result.replace(Constants.FONT_HOUR_OF_DAY, "")
        result = result.replace(Constants.FONT_MINUTE, "")
        result = result.replace(Constants.FONT_SECOND, "")
        result = result.replace(Constants.FONT_AUTO_SEQUENCE_NUMBER, "")
        return result
    }

    /**
     * 获取apk包签名基本信息
     * @return string[0]证书发行者,string[1]证书所有者,string[2]序列号
     * string[3]证书起始时间 string[4]证书结束时间
     */
    @JvmStatic
    fun getAPKSignInfo(filePath: String): Array<String> {
        var subjectDN = ""
        var issuerDN = ""
        var serial = ""
        var notBefore = ""
        var notAfter = ""
        try {
            val jarFile = JarFile(filePath)
            val jarEntry = jarFile.getJarEntry("AndroidManifest.xml")
            if (jarEntry != null) {
                val readBuffer = ByteArray(8192)
                val inputStream = BufferedInputStream(jarFile.getInputStream(jarEntry))
                while (inputStream.read(readBuffer, 0, readBuffer.size) != -1) {
                    // not using
                }
                val certs = jarEntry.certificates
                if (certs != null && certs.isNotEmpty()) {
                    // 获取证书
                    val x509cert = certs[0] as X509Certificate
                    // 获取证书发行者
                    issuerDN = x509cert.issuerDN.toString()
                    // 获取证书所有者
                    subjectDN = x509cert.subjectDN.toString()
                    // 证书序列号
                    serial = x509cert.serialNumber.toString()
                    // 证书起始有效期
                    notBefore = x509cert.notBefore.toString()
                    // 证书结束有效期
                    notAfter = x509cert.notAfter.toString()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return arrayOf(subjectDN, issuerDN, serial, notBefore, notAfter)
    }

    @JvmStatic
    fun hashMD5Value(inputStream: InputStream): String {
        return getHashValue(inputStream, "MD5")
    }

    @JvmStatic
    fun hashSHA256Value(inputStream: InputStream): String {
        return getHashValue(inputStream, "SHA256")
    }

    @JvmStatic
    fun hashSHA1Value(inputStream: InputStream): String {
        return getHashValue(inputStream, "SHA1")
    }

    @JvmStatic
    fun getSignatureMD5StringOfPackageInfo(info: PackageInfo): String {
        return getSignatureStringOfPackageInfo(info, "MD5")
    }

    @JvmStatic
    fun getSignatureSHA1OfPackageInfo(info: PackageInfo): String {
        return getSignatureStringOfPackageInfo(info, "SHA1")
    }

    @JvmStatic
    fun getSignatureSHA256OfPackageInfo(info: PackageInfo): String {
        return getSignatureStringOfPackageInfo(info, "SHA256")
    }

    private fun getSignatureStringOfPackageInfo(packageInfo: PackageInfo, type: String): String {
        try {
            val localMessageDigest = MessageDigest.getInstance(type)
            packageInfo.signatures?.let { signatures ->
                if (signatures.isNotEmpty()) {
                    localMessageDigest.update(signatures[0].toByteArray())
                    return getHexString(localMessageDigest.digest())
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return ""
    }

    private fun getHashValue(inputStream: InputStream, type: String): String {
        try {
            val messageDigest = MessageDigest.getInstance(type)
            var length: Int
            val buffer = ByteArray(1024)
            while (inputStream.read(buffer).also { length = it } != -1) {
                messageDigest.update(buffer, 0, length)
            }
            inputStream.close()
            return getHexString(messageDigest.digest())
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return ""
    }

    private fun getHexString(paramArrayOfByte: ByteArray?): String {
        if (paramArrayOfByte == null) {
            return ""
        }
        val localStringBuilder = StringBuilder(2 * paramArrayOfByte.size)
        for (i in paramArrayOfByte.indices) {
            var str = Integer.toString(0xFF and paramArrayOfByte[i].toInt(), 16)
            if (str.length == 1) {
                str = "0$str"
            }
            localStringBuilder.append(str)
        }
        return localStringBuilder.toString()
    }

    /**
     * 当SharedPreference中设置了加载启动项的值，则会查询启动Receiver，否则会直接返回一个空Bundle（查询为耗时操作，此方法会阻塞）
     */
    @JvmStatic
    fun getStaticRegisteredReceiversOfBundleTypeForPackageName(context: Context, package_name: String): Bundle {
        val bundle = Bundle()
        if (!SPUtil.getGlobalSharedPreferences(context)
                .getBoolean(Constants.PREFERENCE_LOAD_STATIC_LOADERS, Constants.PREFERENCE_LOAD_STATIC_LOADERS_DEFAULT)
        ) {
            return bundle
        }
        val packageManager = context.packageManager
        val static_filters = context.resources.getStringArray(R.array.static_receiver_filters)

        for (s in static_filters) {
            val list = packageManager.queryBroadcastReceivers(Intent(s), 0)
            if (list == null) continue
            for (info in list) {
                val pn = info.activityInfo.packageName ?: continue
                var filters_class = bundle.getStringArrayList(info.activityInfo.name)
                if (filters_class == null) {
                    filters_class = ArrayList()
                    filters_class.add(s)
                    if (pn == package_name) bundle.putStringArrayList(info.activityInfo.name, filters_class)
                } else {
                    if (!filters_class.contains(s)) filters_class.add(s)
                }
            }
        }
        return bundle
    }



    /**
     * 判断一个字符串是否为标准Linux/Windows的标准合法文件名（不包含非法字符）
     * @param name 文件名称（仅文件名，不包含路径）
     * @return true-合法文件名  false-包含非法字符
     */
    @JvmStatic
    fun isALegalFileName(name: String): Boolean {
        try {
            if (name.contains("?") || name.contains("\\") || name.contains("/") ||
                name.contains(":") || name.contains("*") || name.contains("\"") ||
                name.contains("<") || name.contains(">") || name.contains("|")
            ) return false
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return true
    }

    /**
     * 将字符串中包含的非法文件系统符号去掉
     * @param content 要处理的内容
     * @return 去掉了文件系统非法符号的内容
     */
    internal fun removeIllegalFileNameCharacters(content: String): String {
        var result = content
        result = result.replace("?", "")
        result = result.replace("\\", "")
        result = result.replace("/", "")
        result = result.replace(":", "")
        result = result.replace("*", "")
        result = result.replace("\"", "")
        result = result.replace("<", "")
        result = result.replace(">", "")
        result = result.replace("|", "")
        return result
    }

    /**
     * 截取文件扩展名，例如Test.apk 则返回 apk
     */
    @JvmStatic
    fun getFileExtensionName(fileName: String): String {
        try {
            return fileName.substring(fileName.lastIndexOf(".") + 1)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return ""
    }

    /**
     * 返回文件主体的文件名，例如 Test.File.java 则返回Test.File
     */
    @JvmStatic
    fun getFileMainName(fileName: String): String {
        try {
            val lastIndex = fileName.lastIndexOf(".")
            if (lastIndex == -1) return fileName
            return fileName.substring(0, lastIndex)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return ""
    }

    /**
     * 判断当前是否连接了WiFi网络
     * @return true-连接了WiFi网络
     */
    @JvmStatic
    fun isWifiConnected(context: Context): Boolean {
        try {
            val wifiInfo = (context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager).connectionInfo
            return wifiInfo != null && wifiInfo.ipAddress != 0
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }

    /**
     * 获取系统热点是否开启
     */
    @JvmStatic
    fun isAPEnabled(context: Context): Boolean {
        try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val method = wifiManager.javaClass.getDeclaredMethod("getWifiApState")
            val field = wifiManager.javaClass.getDeclaredField("WIFI_AP_STATE_ENABLED")
            val value_wifi_enabled = field.get(wifiManager) as Int
            return (method.invoke(wifiManager) as Int) == value_wifi_enabled
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }

    /**
     * 跳转到系统热点配置页
     */
    @JvmStatic
    fun goToApPageActivity(context: Context) {
        try {
            val intent = Intent()
            val cm = ComponentName("com.android.settings", "com.android.settings.TetherSettings")
            intent.component = cm
            intent.action = "android.intent.action.VIEW"
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            ToastManager.showToast(context, e.toString(), Toast.LENGTH_SHORT)
        }
    }

    @JvmStatic
    fun getRouterIpAddress(context: Context): String {
        try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val dhcpInfo = wifiManager.dhcpInfo
            return Formatter.formatIpAddress(dhcpInfo.gateway)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return "192.168.1.1"
    }

    /**
     * 获取本机连接WiFi网络的IP地址
     */
    @JvmStatic
    fun getSelfIp(context: Context): String {
        try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            return Formatter.formatIpAddress(wifiManager.dhcpInfo.ipAddress)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return "0.0.0.0"
    }

    /**
     * 获取本应用名称
     */
    @JvmStatic
    fun getAppName(context: Context): String {
        return getAppNameByPackageName(context, context.packageName)
    }

    /**
     * 获取本应用版本名
     */
    @JvmStatic
    fun getAppVersionName(context: Context): String {
        try {
            val packageManager = context.packageManager
            return packageManager.getPackageInfo(context.packageName, 0).versionName.toString()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return ""
    }

    /**
     * 通过contentUri获取文件名
     */
    @JvmStatic
    fun getFileNameFromContentUri(context: Context, contentUri: Uri): String? {
        return queryResultByContentResolver(context, contentUri, MediaStore.Files.FileColumns.DISPLAY_NAME)
    }

    /**
     * 通过contentUri获取文件路径
     */
    @JvmStatic
    fun getFilePathFromContentUri(context: Context, contentUri: Uri): String? {
        return queryResultByContentResolver(context, contentUri, MediaStore.Files.FileColumns.DATA)
    }

    /**
     * 通过contentUri获取文件大小，返回字符串型长度，单位字节
     */
    @JvmStatic
    fun getFileLengthFromContentUri(context: Context, contentUri: Uri): String? {
        return queryResultByContentResolver(context, contentUri, MediaStore.Files.FileColumns.SIZE)
    }

    private fun queryResultByContentResolver(context: Context, contentUri: Uri, selection: String): String? {
        try {
            var result: String? = null
            val cursor: Cursor? = context.contentResolver.query(
                contentUri,
                arrayOf(selection),
                null, null, null
            )
            if (cursor == null) return null
            else {
                if (cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(selection))
                }
                cursor.close()
            }
            return result
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    /**
     * 传入的file须为主存储下的文件，且对file有完整的读写权限
     */
    @JvmStatic
    fun getUriForFileByFileProvider(context: Context, file: File): Uri {
        return FileProvider.getUriForFile(context, "info.muge.appshare.FileProvider", file)
    }

    /**
     * 请求更新媒体数据库
     */
    @JvmStatic
    fun requestUpdatingMediaDatabase(context: Context) {
        try {
            val bundle = Bundle()
            bundle.putString("volume", "external")
            val intent = Intent()
            intent.putExtras(bundle)
            intent.component = ComponentName(
                "com.android.providers.media",
                "com.android.providers.media.MediaScannerService"
            )
            context.startService(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 计算出来的位置，y方向就在anchorView的上面和下面对齐显示，x方向就是与屏幕右边对齐显示
     * 如果anchorView的位置有变化，就可以适当自己额外加入偏移来修正
     * @param anchorView  呼出window的view
     * @param contentView   window的内容布局
     * @return window显示的左上角的xOff,yOff坐标
     */
    @JvmStatic
    fun calculatePopWindowPos(anchorView: View, contentView: View): IntArray {
        val windowPos = IntArray(2)
        val anchorLoc = IntArray(2)
        // 获取锚点View在屏幕上的左上角坐标位置
        anchorView.getLocationOnScreen(anchorLoc)
        val anchorHeight = anchorView.height
        // 获取屏幕的高宽
        val screenHeight = anchorView.context.resources.displayMetrics.heightPixels
        val screenWidth = anchorView.resources.displayMetrics.widthPixels
        contentView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        // 计算contentView的高宽
        val windowHeight = contentView.measuredHeight
        val windowWidth = contentView.measuredWidth
        // 判断需要向上弹出还是向下弹出显示
        val isNeedShowUp = (screenHeight - anchorLoc[1] - anchorHeight < windowHeight)
        if (isNeedShowUp) {
            windowPos[0] = anchorLoc[0]
            windowPos[1] = anchorLoc[1] - windowHeight
        } else {
            windowPos[0] = anchorLoc[0]
            windowPos[1] = anchorLoc[1] + anchorHeight
        }
        return windowPos
    }

    /**
     * 通过keyword高亮content中的指定内容，支持汉字首字母、全拼匹配
     * @param content 要匹配的内容
     * @param keyword 匹配字符
     * @param color 高亮颜色
     * @return 生成的Spannable
     */
    @JvmStatic
    fun getSpannableString(content: String, keyword: String?, @ColorInt color: Int): SpannableStringBuilder {
        val builder = SpannableStringBuilder(content)
        if (keyword == null || keyword.isEmpty()) return builder

        val index = content.lowercase().indexOf(keyword.lowercase())
        if (index >= 0) {
            builder.setSpan(
                ForegroundColorSpan(color),
                index,
                index + keyword.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            return builder
        }

        val keywordLower = keyword.lowercase()
        val singleCharFullSpell = ArrayList<String>()
        val fullSpell = StringBuilder()
        val singleSpell = StringBuilder()
        val chars_content = content.toCharArray()

        for (i in chars_content.indices) {
            if (PinyinUtil.isChineseChar(chars_content[i])) {
                fullSpell.append(PinyinUtil.getFullSpell(chars_content[i].toString()).lowercase())
                singleSpell.append(PinyinUtil.getFirstSpell(chars_content[i].toString()).lowercase())
                singleCharFullSpell.add(PinyinUtil.getFullSpell(chars_content[i].toString()).lowercase())
            } else {
                fullSpell.append(chars_content[i].toString().lowercase())
                singleSpell.append(chars_content[i].toString().lowercase())
                singleCharFullSpell.add(chars_content[i].toString().lowercase())
            }
        }

        var span_index_begin = -1
        var span_index_end = -1
        val index_first_spell = singleSpell.indexOf(keywordLower)

        if (index_first_spell >= 0) {
            span_index_begin = index_first_spell
            span_index_end = index_first_spell + keywordLower.length
        } else {
            var fullSpellCheck = 0
            var keywordFullSpellCheck = keywordLower
            var flag_matched = false
            var flag_matched_end = true

            for (i in singleCharFullSpell.indices) {
                if (keywordFullSpellCheck.trim().isEmpty()) break
                val sp = singleCharFullSpell[i]

                if (sp.contains(keywordLower) && !flag_matched) {
                    span_index_begin = i
                    span_index_end = span_index_begin + 1
                    break
                }

                val index_2 = keywordFullSpellCheck.indexOf(sp)
                if (index_2 >= 0 && PinyinUtil.isChineseChar(chars_content[i])) {
                    flag_matched = true
                    if (span_index_begin == -1) span_index_begin = i
                    keywordFullSpellCheck = keywordFullSpellCheck.substring(index_2 + sp.length)
                    fullSpellCheck++
                    continue
                }

                val index_1 = sp.indexOf(keywordFullSpellCheck)
                if (flag_matched) {
                    if (index_1 >= 0) {
                        fullSpellCheck++
                    } else {
                        flag_matched_end = false
                    }
                    break
                }
            }

            if (fullSpellCheck > 0) span_index_end = span_index_begin + fullSpellCheck
            if (!flag_matched_end) {
                span_index_begin = -1
                span_index_end = -1
            }
        }

        if (span_index_begin >= 0 && span_index_end >= 0) {
            builder.setSpan(
                ForegroundColorSpan(color),
                span_index_begin,
                span_index_end,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        return builder
    }

    @JvmStatic
    fun dp2px(context: Context, dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            context.resources.displayMetrics
        ).toInt()
    }

    /**
     * 保存 Drawable 到相册
     */
    @JvmStatic
    fun saveDrawableToGallery(context: Context, drawable: Drawable, appName: String) {
        try {
            val bitmap = drawableToBitmap(drawable)
            val fileName = "Icon_${removeIllegalFileNameCharacters(appName)}_${System.currentTimeMillis()}.png"
            
            val values = android.content.ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/AppShare/Icons")
            }

            val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            if (uri != null) {
                val outputStream = context.contentResolver.openOutputStream(uri)
                if (outputStream != null) {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                    outputStream.close()
                    ToastManager.showToast(context, context.getString(R.string.toast_export_complete) + " " + fileName, Toast.LENGTH_SHORT)
                }
            } else {
                ToastManager.showToast(context, "Failed to create file", Toast.LENGTH_SHORT)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            ToastManager.showToast(context, "Error: ${e.message}", Toast.LENGTH_SHORT)
        }
    }

    /**
     * 设置应用语言
     * @param context 上下文
     * @param languageValue 语言值，参考 Constants.LANGUAGE_*
     */
    @JvmStatic
    fun setLanguage(context: Context, languageValue: Int) {
        val locale = when (languageValue) {
            Constants.LANGUAGE_CHINESE -> Locale.CHINESE
            Constants.LANGUAGE_ENGLISH -> Locale.ENGLISH
            else -> Locale.getDefault() // 跟随系统
        }

        Locale.setDefault(locale)
        val config = android.content.res.Configuration(context.resources.configuration)
        config.setLocale(locale)
        context.resources.updateConfiguration(config, context.resources.displayMetrics)

        // 同时更新 Application context
        val appContext = context.applicationContext
        appContext.resources.updateConfiguration(config, appContext.resources.displayMetrics)
    }

    /**
     * 获取当前设置的语言 Locale
     */
    @JvmStatic
    fun getAppLocale(context: Context): Locale {
        val languageValue = SPUtil.getGlobalSharedPreferences(context)
            .getInt(Constants.PREFERENCE_LANGUAGE, Constants.PREFERENCE_LANGUAGE_DEFAULT)
        return when (languageValue) {
            Constants.LANGUAGE_CHINESE -> Locale.CHINESE
            Constants.LANGUAGE_ENGLISH -> Locale.ENGLISH
            else -> Locale.getDefault()
        }
    }
}
