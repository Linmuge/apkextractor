package info.muge.apkextractor.utils

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.provider.MediaStore
import android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI
import android.provider.Settings
import android.text.InputFilter
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import info.muge.apkextractor.MyApplication
import java.io.IOException
import java.util.*


fun Any.toast(){
    Toast.makeText(MyApplication.instance, this.toString(), Toast.LENGTH_SHORT).show()
}
fun View.setVisibilityState(state: Boolean){
    visibility = if (state)View.VISIBLE else View.GONE
}
fun View.gone(){
    visibility = View.GONE
}
fun View.visible(){
    visibility = View.VISIBLE
}
fun View.invisible(){
    visibility = View.INVISIBLE
}
fun View.hideKeyboard(context: Activity) {
    (context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(
            windowToken,
            0
    )
}

fun EditText.showSoftInputFromWindow() {
    isFocusable = true
    isFocusableInTouchMode = true
    requestFocus()
}

fun Context.showKeyboard() {
    (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).toggleSoftInput(
            0,
            InputMethodManager.HIDE_NOT_ALWAYS
    )
}

fun EditText.showKeyboard(context: Context) {
    (context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).showSoftInput(
            this,
            InputMethodManager.SHOW_FORCED
    )
}

fun String.copy(){
    val manager = MyApplication.instance.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    manager.setPrimaryClip(ClipData.newPlainText(this, this))
}
/*保存 图片*/
fun Activity.saveResource(resourceId: Int, name: String, listener: () -> Unit){
    val image = BitmapFactory.decodeResource(resources, resourceId)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
        val values = ContentValues()
        values.put(MediaStore.Images.Media.DESCRIPTION, name)
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, name)
        values.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        values.put(MediaStore.Images.Media.TITLE, name)
        val resolver = MyApplication.instance.contentResolver
        val uri = resolver.insert(EXTERNAL_CONTENT_URI, values)
        try {
            resolver.openOutputStream(uri!!).use { out ->
                if (!image.compress(Bitmap.CompressFormat.PNG, 100, out)) {
                    throw IOException("Failed to compress")
                }
            }
            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            values.putNull(MediaStore.MediaColumns.DATE_EXPIRES)
            resolver.update(uri, values, null, null)
            listener()
        }catch (e: Exception){
            e.printStackTrace()
            "保存失败，请手动截图前往对应App".toast()
        }

    }else{
        val bitmap = BitmapFactory.decodeResource(resources, resourceId)
        MediaStore.Images.Media.insertImage(contentResolver, bitmap, name, "")
        "图片已保存到相册".toast()
        listener()
    }
}
fun EditText.enableEdit(){
    isFocusableInTouchMode = true
    isFocusable = true
    requestFocus()
}
fun EditText.disableEdit(){
    isFocusable = false;
    isFocusableInTouchMode = false
}

/**
 * 禁止EditText输入空格和换行符
 *
 * @param editText EditText输入框
 */
fun EditText.setEditTextInputSpace() {
    val filter = InputFilter { source, start, end, dest, dstart, dend ->
        if (source == " " || source.toString().contentEquals("\n")) {
            ""
        } else {
            null
        }
    }
    this.filters = arrayOf(filter)
}

 fun View.vibrate(){
     performHapticFeedback(HapticFeedbackConstants.LONG_PRESS, HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING)
}

