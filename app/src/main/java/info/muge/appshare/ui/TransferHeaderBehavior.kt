package info.muge.appshare.ui

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout

/**
 * 传输头部行为
 */
class TransferHeaderBehavior(context: Context, attrs: AttributeSet?) : 
    CoordinatorLayout.Behavior<ImageView>(context, attrs) {

    /**
     * 处于中心时候原始X轴
     */
    private var mOriginalHeaderX = 0

    /**
     * 处于中心时候原始Y轴
     */
    private var mOriginalHeaderY = 0

    override fun layoutDependsOn(
        parent: CoordinatorLayout,
        child: ImageView,
        dependency: View
    ): Boolean {
        return dependency is Toolbar
    }

    override fun onDependentViewChanged(
        parent: CoordinatorLayout,
        child: ImageView,
        dependency: View
    ): Boolean {
        // 计算X轴坐标
        if (mOriginalHeaderX == 0) {
            mOriginalHeaderX = dependency.width / 2 - child.width / 2
        }
        // 计算Y轴坐标
        if (mOriginalHeaderY == 0) {
            mOriginalHeaderY = dependency.height - child.height
        }
        
        // X轴百分比
        var mPercentX = dependency.y / mOriginalHeaderX
        if (mPercentX >= 1) {
            mPercentX = 1f
        }
        
        // Y轴百分比
        var mPercentY = dependency.y / mOriginalHeaderY
        if (mPercentY >= 1) {
            mPercentY = 1f
        }

        var x = mOriginalHeaderX - mOriginalHeaderX * mPercentX
        if (x <= child.width) {
            x = child.width.toFloat()
        }
        // TODO 头像的放大和缩小没做

        child.x = x
        child.y = mOriginalHeaderY - mOriginalHeaderY * mPercentY
        return true
    }
}

