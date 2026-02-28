package info.muge.appshare.data

/**
 * 自定义应用分组数据类
 */
data class AppGroup(
    val id: String,
    val name: String,
    val color: Long = 0xFF2196F3, // 默认蓝色
    val packageNames: Set<String> = emptySet(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    /**
     * 获取分组中应用的数量
     */
    val size: Int
        get() = packageNames.size

    /**
     * 检查包名是否在此分组中
     */
    fun contains(packageName: String): Boolean = packageNames.contains(packageName)

    /**
     * 添加应用到分组
     */
    fun addPackage(packageName: String): AppGroup {
        return copy(
            packageNames = packageNames + packageName,
            updatedAt = System.currentTimeMillis()
        )
    }

    /**
     * 从分组移除应用
     */
    fun removePackage(packageName: String): AppGroup {
        return copy(
            packageNames = packageNames - packageName,
            updatedAt = System.currentTimeMillis()
        )
    }

    /**
     * 更新分组名称
     */
    fun rename(newName: String): AppGroup {
        return copy(
            name = newName,
            updatedAt = System.currentTimeMillis()
        )
    }

    companion object {
        /**
         * 生成新的分组 ID
         */
        fun generateId(): String = "group_${System.currentTimeMillis()}"
    }
}

/**
 * 预定义颜色选项
 */
object GroupColors {
    val colors = listOf(
        0xFF2196F3, // Blue
        0xFF4CAF50, // Green
        0xFFFF9800, // Orange
        0xFFE91E63, // Pink
        0xFF9C27B0, // Purple
        0xFF00BCD4, // Cyan
        0xFFFFEB3B, // Yellow
        0xFF795548, // Brown
        0xFF607D8B, // Blue Grey
        0xFFF44336  // Red
    )

    fun getColor(index: Int): Long {
        return colors[index % colors.size]
    }
}
