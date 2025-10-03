package info.muge.appshare.ui

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import info.muge.appshare.R

/**
 * 签名视图
 */
class SignatureView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    val root: ViewGroup
    val tv_sub_value: TextView
    val tv_iss_value: TextView
    val tv_serial_value: TextView
    val tv_start: TextView
    val tv_end: TextView
    val tv_md5: TextView
    val tv_sha1: TextView
    val tv_sha256: TextView

    val linearLayout_sub: LinearLayout
    val linearLayout_iss: LinearLayout
    val linearLayout_serial: LinearLayout
    val linearLayout_start: LinearLayout
    val linearLayout_end: LinearLayout
    val linearLayout_md5: LinearLayout
    val linearLayout_sha1: LinearLayout
    val linearLayout_sha256: LinearLayout

    init {
        inflate(context, R.layout.layout_card_signature, this)
        
        root = findViewById(R.id.detail_signature_root)
        tv_sub_value = findViewById(R.id.detail_signature_sub_value)
        tv_iss_value = findViewById(R.id.detail_signature_iss_value)
        tv_serial_value = findViewById(R.id.detail_signature_serial_value)
        tv_start = findViewById(R.id.detail_signature_start_value)
        tv_end = findViewById(R.id.detail_signature_end_value)
        tv_md5 = findViewById(R.id.detail_signature_md5_value)
        tv_sha1 = findViewById(R.id.detail_signature_sha1_value)
        tv_sha256 = findViewById(R.id.detail_signature_sha256_value)

        linearLayout_sub = findViewById(R.id.detail_signature_sub)
        linearLayout_iss = findViewById(R.id.detail_signature_iss)
        linearLayout_serial = findViewById(R.id.detail_signature_serial)
        linearLayout_start = findViewById(R.id.detail_signature_start)
        linearLayout_end = findViewById(R.id.detail_signature_end)
        linearLayout_md5 = findViewById(R.id.detail_signature_md5)
        linearLayout_sha1 = findViewById(R.id.detail_signature_sha1)
        linearLayout_sha256 = findViewById(R.id.detail_signature_sha256)
    }
}

