package info.muge.appshare.ui

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import info.muge.appshare.R

/**
 * 半透明行为
 */
class TranslucentBehavior(private val context: Context, attrs: AttributeSet?) : 
    CoordinatorLayout.Behavior<Toolbar>(context, attrs) {

    /**
     * 标题栏的高度
     */
    private var mToolbarHeight = 0

    override fun layoutDependsOn(
        parent: CoordinatorLayout,
        child: Toolbar,
        dependency: View
    ): Boolean {
        return dependency is TextView
    }

    /**
     * 必须要加上 layout_anchor，对方也要layout_collapseMode才能使用
     */
    override fun onDependentViewChanged(
        parent: CoordinatorLayout,
        child: Toolbar,
        dependency: View
    ): Boolean {
        // 初始化高度
        if (mToolbarHeight == 0) {
            mToolbarHeight = child.bottom * 2 // 为了更慢的
        }
        
        // 计算toolbar从开始移动到最后的百分比
        var percent = dependency.y / mToolbarHeight

        // 百分大于1，直接赋值为1
        if (percent >= 1) {
            percent = 1f
        }

        // 计算alpha通道值
        val alpha = percent * 255

        // 设置背景颜色
        child.setBackgroundColor(context.resources.getColor(R.color.colorTitle))

        return true
    }
}

