package info.muge.appshare.utils

import android.content.Context
import android.graphics.Color
import android.util.TypedValue
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt

/**
 * 颜色辅助工具类
 * 提供便捷的方法来获取主题颜色
 */
object ColorHelper {

    /**
     * 获取主题的 Primary 颜色
     *
     * @param context 上下文
     * @return Primary 颜色值
     */
    @ColorInt
    fun getPrimaryColor(context: Context): Int {
        return getThemeColor(
            context,
            androidx.appcompat.R.attr.colorPrimary,
            Color.parseColor("#4285F4")
        )
    }

    /**
     * 获取主题的 Secondary 颜色
     *
     * @param context 上下文
     * @return Secondary 颜色值
     */
    @ColorInt
    fun getSecondaryColor(context: Context): Int {
        return getThemeColor(
            context,
            android.R.attr.colorSecondary,
            Color.parseColor("#565E71")
        )
    }

    /**
     * 获取主题的 Surface 颜色
     *
     * @param context 上下文
     * @return Surface 颜色值
     */
    @ColorInt
    fun getSurfaceColor(context: Context): Int {
        return getThemeColor(
            context,
            android.R.attr.colorBackground,
            Color.WHITE
        )
    }

    /**
     * 获取主题的 OnSurface 颜色
     *
     * @param context 上下文
     * @return OnSurface 颜色值
     */
    @ColorInt
    fun getOnSurfaceColor(context: Context): Int {
        return getThemeColor(
            context,
            android.R.attr.textColorPrimary,
            Color.BLACK
        )
    }

    /**
     * 获取主题的 Error 颜色
     *
     * @param context 上下文
     * @return Error 颜色值
     */
    @ColorInt
    fun getErrorColor(context: Context): Int {
        return getThemeColor(
            context,
            android.R.attr.colorError,
            Color.parseColor("#BA1A1A")
        )
    }

    /**
     * 获取主题颜色的通用方法
     *
     * @param context 上下文
     * @param attr 主题属性
     * @param defaultColor 默认颜色（当无法获取主题颜色时使用）
     * @return 颜色值
     */
    @ColorInt
    fun getThemeColor(
        context: Context,
        @AttrRes attr: Int,
        @ColorInt defaultColor: Int
    ): Int {
        val typedValue = TypedValue()
        return if (context.theme.resolveAttribute(attr, typedValue, true)) {
            typedValue.data
        } else {
            defaultColor
        }
    }
    
    /**
     * 检查颜色是否为深色
     * 
     * @param color 要检查的颜色
     * @return 如果是深色返回 true，否则返回 false
     */
    fun isDarkColor(@ColorInt color: Int): Boolean {
        val darkness = 1 - (0.299 * Color.red(color) + 
                           0.587 * Color.green(color) + 
                           0.114 * Color.blue(color)) / 255
        return darkness >= 0.5
    }
    
    /**
     * 获取颜色的对比色（黑色或白色）
     * 用于在有色背景上显示文字
     * 
     * @param backgroundColor 背景颜色
     * @return 对比色（黑色或白色）
     */
    @ColorInt
    fun getContrastColor(@ColorInt backgroundColor: Int): Int {
        return if (isDarkColor(backgroundColor)) {
            Color.WHITE
        } else {
            Color.BLACK
        }
    }
    
    /**
     * 调整颜色的透明度
     * 
     * @param color 原始颜色
     * @param alpha 透明度 (0-255)
     * @return 调整后的颜色
     */
    @ColorInt
    fun setAlpha(@ColorInt color: Int, alpha: Int): Int {
        return Color.argb(
            alpha.coerceIn(0, 255),
            Color.red(color),
            Color.green(color),
            Color.blue(color)
        )
    }
}

