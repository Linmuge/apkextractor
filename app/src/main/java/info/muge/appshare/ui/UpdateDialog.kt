package info.muge.appshare.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Color
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetDialog
import info.muge.appshare.R
import info.muge.appshare.activities.MainActivity
import info.muge.appshare.databinding.DialogUpdateBinding
import info.muge.appshare.utils.SPUtil
import info.muge.appshare.utils.anko.browse
import info.muge.appshare.utils.anko.startActivity
import info.muge.appshare.utils.colorPrimary


private var updateDialog: BottomSheetDialog? = null

fun Activity.showPrivacyDialog() {
    // 使用 Material3 BottomSheetDialog 作为隐私弹窗，从底部弹出
    val dialog = BottomSheetDialog(
        this,
        com.google.android.material.R.style.ThemeOverlay_Material3_BottomSheetDialog
    ).apply {
        // 不允许点击空白区域或返回键关闭，保持与原实现一致
        setCancelable(false)
        // 复用原有布局与初始化逻辑
        setContentView(getView())
    }

    // 保存引用，便于在按钮点击时关闭
    updateDialog = dialog
    dialog.show()
}

@SuppressLint("ClickableViewAccessibility")
private fun Activity.getView(): View {
    val view = DialogUpdateBinding.inflate(LayoutInflater.from(this))
    view.apply {
        tvDesc.text = ""
        tvCancel.setOnClickListener {
            updateDialog?.dismiss()
            this@getView.finish()
        }
        tvUpdate.setOnClickListener {
            SPUtil.getGlobalSharedPreferences(this@getView).edit().putBoolean("start", false).apply()
            updateDialog?.dismiss()
            this@getView.startActivity<MainActivity>()
            this@getView.finish()
        }

        // 拼接字符串
        val spanBuilder = SpannableStringBuilder("感谢您使用${getString(R.string.app_name)}\n我们非常重视您的个人信息及隐私保护,在您使用我们的产品前，请您认真阅读  ")

        // 获取主题的 primary 颜色用于链接
        val typedValue = android.util.TypedValue()
        val linkColor = if (theme.resolveAttribute(
                colorPrimary,
                typedValue,
                true
            )) {
            typedValue.data
        } else {
            Color.parseColor("#4285F4") // 备用颜色
        }

        // 服务协议
        var span = SpannableString("服务协议")
        span.setSpan(object : ClickableSpan() {
            override fun onClick(widget: View) {
                browse("https://link.appshare.muge.info/appkit/user.html")
            }
        }, 0, span.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        span.setSpan(ForegroundColorSpan(linkColor), 0, span.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        spanBuilder.append(span)

        spanBuilder.append(" 与 ")

        // 隐私政策
        span = SpannableString("隐私政策")
        span.setSpan(object : ClickableSpan() {
            override fun onClick(widget: View) {
                browse("https://link.appshare.muge.info/appkit/privacy.html")
            }
        }, 0, span.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        span.setSpan(ForegroundColorSpan(linkColor), 0, span.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        spanBuilder.append(span)

        spanBuilder.append(
            " 的全部内容，同意后开始使用我们的产品。\n\n" +
            "郑重承诺：\n" +
            "1. 本应用不申请网络权限，无法连接互联网。\n" +
            "2. 您所有的操作、生成的APK/图片均只保存在您的手机本地。\n" +
            "3. 我们不收集、不上传任何用户数据。\n\n" +
            "权限说明：\n" +
            "1. android.permission.QUERY_ALL_PACKAGES\n" +
            "2. com.android.permission.GET_INSTALLED_APPS\n" +
            "以上权限仅用于获取本机已安装应用列表以展示及导出，数据绝不上传。"
        )

        // 赋值给TextView
        tvDesc.movementMethod = LinkMovementMethod.getInstance()
        tvDesc.text = spanBuilder

        // 设置高亮颜色透明，因为点击会变色
        tvDesc.highlightColor = ContextCompat.getColor(applicationContext, android.R.color.transparent)
    }
    return view.root
}
