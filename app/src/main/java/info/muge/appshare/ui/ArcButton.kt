package info.muge.appshare.ui

import android.content.Context
import android.graphics.Paint
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RoundRectShape
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import info.muge.appshare.R


class ArcButton : AppCompatTextView {

    //默认的圆角半径
    private var radius = 1000000000f


    constructor(context: Context):this(context,null,0)
    constructor(context: Context,attributes: AttributeSet):this(context,attributes,0)
    constructor(context: Context, attributes: AttributeSet?, defStyle:Int):super(context,attributes,defStyle){
        initUI()
    }

    private fun initUI() {
        val outRectr = floatArrayOf(
            radius,
            radius,
            radius,
            radius,
            radius,
            radius,
            radius,
            radius
        )
        //创建状态管理器
        val roundRectShape0 = RoundRectShape(outRectr, null, null)
        val shapeDrawable = ShapeDrawable()
        shapeDrawable.shape = roundRectShape0
        shapeDrawable.paint.style = Paint.Style.FILL
        shapeDrawable.paint.color = if (background is ColorDrawable) {
            val cd = background as ColorDrawable
            cd.color
        }else{
            ContextCompat.getColor(context, R.color.colorAccent)
        }
        //设置我们的背景，就是xml里面的selector
        background = shapeDrawable
    }

    /**
     * 设置圆角矩形
     *
     * @param radius
     */
    fun setRadius(radius: Float) {
        this.radius = radius
        initUI()
    }
}
