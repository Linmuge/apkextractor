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
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import info.muge.appshare.R
import info.muge.appshare.activities.MainActivity
import info.muge.appshare.databinding.DialogUpdateBinding
import info.muge.appshare.utils.SPUtil
import info.muge.appshare.utils.anko.browse
import info.muge.appshare.utils.anko.startActivity


private var updateDialog:AlertDialog?=null

fun Activity.showPrivacyDialog(){
    updateDialog = AlertDialog.Builder(this, R.style.updateDialog)
        .setView(getView())
        .setCancelable(false)
        .create()

    updateDialog?.show()
}
@SuppressLint("ClickableViewAccessibility")
private fun Activity.getView():View{
    val view = DialogUpdateBinding.inflate(LayoutInflater.from(this))
    view.apply {
        tvDesc.text = ""
        tvCancel.setOnClickListener {
            updateDialog?.dismiss()
            this@getView.finish()
        }
        tvUpdate.setOnClickListener {
            SPUtil.getGlobalSharedPreferences(this@getView).edit().putBoolean("start",false).apply()
            updateDialog?.dismiss()
            this@getView.startActivity<MainActivity>()
            this@getView.finish()
        }

        //拼接字符串

        val spanBuilder = SpannableStringBuilder("感谢您使用AppShare\n我们非常重视您的个人信息及隐私保护,在您使用我们的产品前，请您认真阅读  ")

        /**

         * 服务协议

         */

        var span = SpannableString("服务协议")

//服务协议点击事件

        span.setSpan(object : ClickableSpan() {
            override fun onClick(widget: View) {

                browse("https://link.appshare.muge.info/appkit/user.html")
            }

        }, 0, span.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

//设置颜色、下划线

        span.setSpan(

            ForegroundColorSpan(Color.parseColor("#4285F4")),

            0,

            span.length,

            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE

        )

        spanBuilder.append(span)

        spanBuilder.append(" 与 ")

        /**

         * 隐私政策

         */

        span = SpannableString("隐私政策")

        span.setSpan(object : ClickableSpan() {
            override fun onClick(widget: View) {
                browse("https://link.appshare.muge.info/appkit/privacy.html")

            }

        }, 0, span.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        span.setSpan(

            ForegroundColorSpan(Color.parseColor("#4285F4")),

            0,

            span.length,

            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE

        )

        spanBuilder.append(span)


        spanBuilder.append(
            " 的全部内容，同意后开始使用我们的产品\n若选择不同意，将无法使用我们的产品和服务，并会退出应用\n以下是我们使用的唯一一条权限\nandroid.permission.QUERY_ALL_PACKAGES\n此条权限仅获取本机已安装应用信息用来展示在首页或应用详情页，且只存储在本地")
// 赋值给TextView

        tvDesc.movementMethod = LinkMovementMethod.getInstance()

        tvDesc.text = spanBuilder

//设置高亮颜色透明，因为点击会变色

        tvDesc.highlightColor = ContextCompat.getColor(applicationContext, R.color.color1)

    }
    return view.root
}