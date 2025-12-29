package info.muge.appshare.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import info.muge.appshare.R
import kotlin.math.min

/**
 * 饼状图View
 * 用于显示统计数据分布
 */
class PieChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var onTouchAction: ((Float, Float) -> Unit)? = null

    fun setOnTouchAction(action: (Float, Float) -> Unit) {
        onTouchAction = action
    }

    // 预定义颜色
    private val chartColors = intArrayOf(
        0xFF6B6B.toInt(),  // 红色
        0x4ECDC4.toInt(),  // 青色
        0x45B7D1.toInt(),  // 蓝色
        0x96CEB4.toInt(),  // 绿色
        0xFFEEAD.toInt(),  // 黄色
        0xD4A5A5.toInt(),  // 粉色
        0x9B59B6.toInt(),  // 紫色
        0x3498DB.toInt(),  // 深蓝
        0xE67E22.toInt(),  // 橙色
        0x2ECC71.toInt(),  // 深绿
        0x1ABC9C.toInt(),  // 青绿
        0xE74C3C.toInt(),  // 深红
    )

    data class PieData(
        val label: String,
        val value: Int,
        val color: Int = Color.WHITE
    )

    private var pieDataList: List<PieData> = listOf()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rectF = RectF()
    private var totalValue = 0

    init {
        paint.style = Paint.Style.FILL
    }

    /**
     * 设置饼图数据
     */
    fun setData(data: List<PieData>) {
        pieDataList = data
        totalValue = data.sumOf { it.value }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (pieDataList.isEmpty() || totalValue == 0) {
            // 绘制空状态
            drawEmptyChart(canvas)
            return
        }

        val size = min(width, height).toFloat()
        val centerX = width / 2f
        val centerY = height / 2f
        val radius = (size / 2 * 0.9f) // 留出一些边距

        rectF.set(
            centerX - radius,
            centerY - radius,
            centerX + radius,
            centerY + radius
        )

        var startAngle = -90f // 从顶部开始

        pieDataList.forEachIndexed { index, data ->
            val sweepAngle = (data.value.toFloat() / totalValue * 360)
            paint.color = chartColors[index % chartColors.size]
            canvas.drawArc(rectF, startAngle, sweepAngle, true, paint)
            startAngle += sweepAngle
        }

        // 绘制中心圆（创建空心效果）
        paint.color = ContextCompat.getColor(context, R.color.md_theme_light_surface)
        val innerRadius = radius * 0.5f
        canvas.drawCircle(centerX, centerY, innerRadius, paint)

        // 在中心绘制总数
        paint.color = ContextCompat.getColor(context, R.color.md_theme_light_onSurface)
        paint.textSize = radius * 0.4f
        paint.textAlign = Paint.Align.CENTER
        val textY = centerY - (paint.descent() + paint.ascent()) / 2
        canvas.drawText(totalValue.toString(), centerX, textY, paint)
    }

    /**
     * 绘制空状态图表
     */
    private fun drawEmptyChart(canvas: Canvas) {
        val size = min(width, height).toFloat()
        val centerX = width / 2f
        val centerY = height / 2f
        val radius = (size / 2 * 0.9f)

        rectF.set(
            centerX - radius,
            centerY - radius,
            centerX + radius,
            centerY + radius
        )

        // 绘制灰色背景圆
        paint.color = ContextCompat.getColor(context, R.color.md_theme_light_surfaceVariant)
        canvas.drawArc(rectF, 0f, 360f, true, paint)

        // 绘制中心圆
        paint.color = ContextCompat.getColor(context, R.color.md_theme_light_surface)
        val innerRadius = radius * 0.5f
        canvas.drawCircle(centerX, centerY, innerRadius, paint)
    }

    /**
     * 获取指定位置的数据项
     */
    fun getDataAtPosition(x: Float, y: Float): PieData? {
        if (pieDataList.isEmpty() || totalValue == 0) return null

        val centerX = width / 2f
        val centerY = height / 2f
        val dx = x - centerX
        val dy = y - centerY
        val distance = kotlin.math.sqrt(dx * dx + dy * dy)
        val radius = min(width, height).toFloat() / 2 * 0.9f

        // 检查是否在圆环内
        if (distance > radius || distance < radius * 0.5f) return null

        // 计算角度
        var angle = kotlin.math.atan2(dy, dx) * 180 / kotlin.math.PI.toFloat()
        angle = (angle + 90 + 360) % 360 // 调整起始角度

        var currentAngle = 0f
        pieDataList.forEach { data ->
            val sweepAngle = data.value.toFloat() / totalValue * 360
            if (angle >= currentAngle && angle < currentAngle + sweepAngle) {
                return data
            }
            currentAngle += sweepAngle
        }

        return null
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP) {
            onTouchAction?.invoke(event.x, event.y)
            performClick()
        }
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }
}
