package info.muge.appshare

import android.graphics.drawable.Drawable

/**
 * 显示项接口
 */
interface DisplayItem {
    /**
     * @return 项目图标
     */
    fun getIconDrawable(): Drawable

    /**
     * @return 项目标题
     */
    fun getTitle(): String

    /**
     * @return 项目描述
     */
    fun getDescription(): String

    /**
     * @return 项目大小，单位字节
     */
    fun getSize(): Long

    /**
     * @return 是否需要红色高亮标注
     */
    fun isRedMarked(): Boolean
}

