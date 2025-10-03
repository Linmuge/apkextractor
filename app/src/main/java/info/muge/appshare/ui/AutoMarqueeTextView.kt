package info.muge.appshare.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView

/**
 * 自动跑马灯TextView
 */
class AutoMarqueeTextView : AppCompatTextView {

    constructor(context: Context) : super(context) {
        isFocusable = true // 在每个构造方法中，将TextView设置为可获取焦点
    }

    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle) {
        isFocusable = true
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        isFocusable = true
    }

    override fun isFocused(): Boolean {
        // 这个方法必须返回true，制造假象，当系统调用该方法的时候，会一直以为TextView已经获取了焦点
        return true
    }

    @SuppressLint("MissingSuperCall")
    override fun onFocusChanged(focused: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
        // 这个方法必须删除其方法体内的实现，也就是让他空实现，也就是说，TextView的焦点获取状态永远都不会改变
    }
}

