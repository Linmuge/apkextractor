package info.muge.appshare.uis

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import com.google.android.material.materialswitch.MaterialSwitch
import info.muge.appshare.R

/**
 * 自定义开关组件
 * 符合 Material Design 3 规范
 * 
 * 使用示例：
 * ```xml
 * <info.muge.appshare.uis.SwitchView
 *     android:id="@+id/svExample"
 *     android:layout_width="match_parent"
 *     android:layout_height="wrap_content"
 *     app:title="开关标题"
 *     app:desc="开关描述说明"
 *     app:icon="@drawable/ic_setting" />
 * ```
 */
class SwitchView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val iconView: ImageView
    private val titleView: TextView
    private val descView: TextView
    private val switchView: MaterialSwitch
    private val textContainer: LinearLayout

    init {
        // 加载布局
        LayoutInflater.from(context).inflate(R.layout.switch_view, this, true)

        // 获取视图引用
        iconView = findViewById(R.id.switch_icon)
        titleView = findViewById(R.id.switch_title)
        descView = findViewById(R.id.switch_desc)
        switchView = findViewById(R.id.switch_button)
        textContainer = findViewById(R.id.switch_text_container)

        // 读取自定义属性
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.SwitchView,
            0, 0
        ).apply {
            try {
                // 设置标题
                getString(R.styleable.SwitchView_title)?.let {
                    titleView.text = it
                }

                // 设置描述（可选）
                getString(R.styleable.SwitchView_desc)?.let {
                    descView.text = it
                    descView.isVisible = true
                } ?: run {
                    descView.isVisible = false
                }

                // 设置图标（可选）
                getResourceId(R.styleable.SwitchView_icon, -1).let { iconRes ->
                    if (iconRes != -1) {
                        iconView.setImageResource(iconRes)
                        iconView.isVisible = true
                    } else {
                        iconView.isVisible = false
                    }
                }
            } finally {
                recycle()
            }
        }

        // 设置整个视图可点击，点击时切换开关状态
        setOnClickListener {
            switchView.isChecked = !switchView.isChecked
        }

        // 防止点击开关本身时触发两次
        switchView.setOnClickListener {
            // 不做任何操作，状态已经由系统改变
        }
    }

    /**
     * 设置开关状态
     */
    fun state(checked: Boolean) {
        switchView.isChecked = checked
    }

    /**
     * 获取开关状态
     */
    fun isChecked(): Boolean {
        return switchView.isChecked
    }

    /**
     * 设置开关变化监听器
     */
    fun setOnCheckedChangeListener(listener: (Boolean) -> Unit) {
        switchView.setOnCheckedChangeListener { _, isChecked ->
            listener(isChecked)
        }
    }

    /**
     * 设置标题
     */
    fun setTitle(title: String) {
        titleView.text = title
    }

    /**
     * 设置描述
     */
    fun setDesc(desc: String?) {
        if (desc.isNullOrEmpty()) {
            descView.isVisible = false
        } else {
            descView.text = desc
            descView.isVisible = true
        }
    }

    /**
     * 设置图标
     */
    fun setIcon(iconRes: Int) {
        if (iconRes != -1) {
            iconView.setImageResource(iconRes)
            iconView.isVisible = true
        } else {
            iconView.isVisible = false
        }
    }

    /**
     * 获取 MaterialSwitch 实例（用于高级配置）
     */
    fun getSwitch(): MaterialSwitch {
        return switchView
    }
}

