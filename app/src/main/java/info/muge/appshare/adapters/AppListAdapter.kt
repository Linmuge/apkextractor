package info.muge.appshare.adapters

import android.app.Activity
import android.graphics.Color
import android.text.format.Formatter
import android.view.View
import androidx.core.view.isVisible
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.drake.brv.BindingAdapter
import com.drake.brv.utils.models
import com.drake.brv.utils.setup
import info.muge.appshare.DisplayItem
import info.muge.appshare.R
import info.muge.appshare.databinding.ItemAppInfoGridBinding
import info.muge.appshare.databinding.ItemAppInfoLinearBinding
import info.muge.appshare.utils.EnvironmentUtil
import info.muge.appshare.utils.colorPrimary
import info.muge.appshare.utils.colorSurfaceContainer

/**
 * 应用列表适配器包装类
 * 使用BRV实现RecyclerView的多选模式、高亮关键字、视图切换等功能
 */
class AppListAdapter<T : DisplayItem>(
    private val activity: Activity,
    private val recyclerView: RecyclerView,
    private var viewMode: Int = 0,
    private val listener: AppListListener<T>
) {
    
    // 状态变量
    private var highlightKeyword: String? = null
    private var isMultiSelectMode: Boolean = false
    private var selectedPositions: BooleanArray? = null
    
    // BRV适配器
    private var adapter: BindingAdapter
    
    init {
        // 先设置 LayoutManager（不调用完整的 setLayoutManagerAndView，避免在 adapter 创建前操作）
        recyclerView.layoutManager = if (viewMode == 1) {
            GridLayoutManager(activity, 4)
        } else {
            LinearLayoutManager(activity).apply {
                orientation = LinearLayoutManager.VERTICAL
            }
        }

        // 配置BRV适配器
        adapter = recyclerView.setup {
            // 根据viewMode添加不同的布局类型
            addType<DisplayItem>(if (viewMode == 0) R.layout.item_app_info_linear else R.layout.item_app_info_grid)
            
            // 绑定数据
            onBind {
                val item = getModel<DisplayItem>() as T
                val position = layoutPosition

                // 获取高亮颜色
                val typedValue = android.util.TypedValue()
                val highlightColor = if (activity.theme.resolveAttribute(
                        androidx.appcompat.R.attr.colorPrimary,
                        typedValue,
                        true
                    )) {
                    typedValue.data
                } else {
                    Color.parseColor("#4285F4") // 备用颜色
                }

                // 根据布局类型使用不同的 ViewBinding
                // 使用 itemViewType 而不是 viewMode 来判断，因为 itemViewType 是当前 ViewHolder 实际使用的布局
                when (itemViewType) {
                    R.layout.item_app_info_linear -> {
                        // 列表模式 - 使用 ItemAppInfoLinearBinding
                        bindLinearItem(item, position, highlightColor)
                    }
                    R.layout.item_app_info_grid -> {
                        // 网格模式 - 使用 ItemAppInfoGridBinding
                        bindGridItem(item, position, highlightColor)
                    }
                }
            }
            
            // 点击事件
            onClick(R.id.item_app_root) {
                val item = getModel<DisplayItem>() as T
                val position = layoutPosition
                
                if (isMultiSelectMode) {
                    // 多选模式：切换选中状态
                    if (selectedPositions != null && position < selectedPositions!!.size) {
                        selectedPositions!![position] = !selectedPositions!![position]
                        adapter.notifyItemChanged(position)
                        listener.onMultiSelectItemChanged(getSelectedItems(), getSelectedFileLength())
                    }
                } else {
                    // 普通模式：触发点击事件
                    listener.onItemClicked(item, this, position)
                }
            }
            
            // 长按事件
            onLongClick(R.id.item_app_root) {
                if (!isMultiSelectMode) {
                    val position = layoutPosition
                    val dataSize = adapter.models?.size ?: 0
                    
                    // 初始化选中状态数组
                    selectedPositions = BooleanArray(dataSize)
                    selectedPositions!![position] = true
                    isMultiSelectMode = true
                    adapter.notifyDataSetChanged()
                    
                    listener.onMultiSelectModeOpened()
                    listener.onMultiSelectItemChanged(getSelectedItems(), getSelectedFileLength())
                }
                true
            }
        }
    }
    
    /**
     * 设置数据
     */
    var models: List<T>?
        get() = adapter.models as? List<T>
        set(value) {
            adapter.models = value
        }
    
    /**
     * 设置LayoutManager和视图模式
     */
    fun setLayoutManagerAndView(mode: Int) {
        if (this.viewMode == mode) return // 如果模式相同，不需要切换

        this.viewMode = mode

        // 保存当前数据
        val currentModels = adapter.models

        // 更新 LayoutManager
        recyclerView.layoutManager = if (mode == 1) {
            GridLayoutManager(activity, 4)
        } else {
            LinearLayoutManager(activity).apply {
                orientation = LinearLayoutManager.VERTICAL
            }
        }

        // 重新配置 adapter 的布局类型
        adapter.typePool.clear() // 清除旧的类型池
        adapter.addType<DisplayItem>(if (mode == 0) R.layout.item_app_info_linear else R.layout.item_app_info_grid)

        // 恢复数据并刷新
        adapter.models = currentModels
        adapter.notifyDataSetChanged()
    }
    
    /**
     * 设置高亮关键字
     */
    fun setHighlightKeyword(keyword: String?) {
        this.highlightKeyword = keyword
        adapter.notifyDataSetChanged()
    }
    
    /**
     * 设置多选模式
     */
    fun setMultiSelectMode(enabled: Boolean) {
        this.isMultiSelectMode = enabled
        if (!enabled) {
            selectedPositions = null
        }
        adapter.notifyDataSetChanged()
    }
    
    /**
     * 获取多选模式状态
     */
    fun isMultiSelectMode(): Boolean {
        return isMultiSelectMode
    }
    
    /**
     * 全选/取消全选
     */
    fun setSelectAll(selected: Boolean) {
        if (!isMultiSelectMode) return
        
        val dataSize = adapter.models?.size ?: 0
        selectedPositions = BooleanArray(dataSize) { selected }
        adapter.notifyDataSetChanged()
        listener.onMultiSelectItemChanged(getSelectedItems(), getSelectedFileLength())
    }
    
    /**
     * 切换全选状态
     */
    fun toggleSelectAll() {
        if (!isMultiSelectMode) return
        
        if (selectedPositions != null) {
            val hasUnselected = selectedPositions!!.any { !it }
            setSelectAll(hasUnselected)
        }
    }
    
    /**
     * 获取已选择的项目
     */
    fun getSelectedItems(): List<T> {
        val selectedList = mutableListOf<T>()
        val models = adapter.models as? List<T>
        
        if (selectedPositions != null && models != null) {
            for (i in models.indices) {
                if (i < selectedPositions!!.size && selectedPositions!![i]) {
                    selectedList.add(models[i])
                }
            }
        }
        
        return selectedList
    }
    
    /**
     * 获取已选择项目的总大小
     */
    private fun getSelectedFileLength(): Long {
        var totalSize = 0L
        val models = adapter.models as? List<T>
        
        if (selectedPositions != null && models != null) {
            for (i in models.indices) {
                if (i < selectedPositions!!.size && selectedPositions!![i]) {
                    totalSize += models[i].getSize()
                }
            }
        }
        
        return totalSize
    }
    
    /**
     * 通知数据变化
     */
    fun notifyDataSetChanged() {
        adapter.notifyDataSetChanged()
    }

    /**
     * 绑定列表模式的项目
     */
    private fun BindingAdapter.BindingViewHolder.bindLinearItem(
        item: T,
        position: Int,
        highlightColor: Int
    ) {
        // 使用 BRV 的 getBinding 方法获取 ViewBinding
        val binding = getBinding<ItemAppInfoLinearBinding>()

        // 设置图标
        binding.itemAppIcon.setImageDrawable(item.getIconDrawable())

        // 设置标题颜色
        val titleColor = if (item.isRedMarked()) {
            activity.resources.getColor(R.color.colorSystemAppTitleColor)
        } else {
            activity.resources.getColor(R.color.colorHighLightText)
        }
        binding.itemAppTitle.setTextColor(titleColor)

        // 设置标题文本（支持高亮）
        try {
            binding.itemAppTitle.text = EnvironmentUtil.getSpannableString(
                item.getTitle(),
                highlightKeyword,
                highlightColor
            )
        } catch (e: Exception) {
            e.printStackTrace()
            binding.itemAppTitle.text = item.getTitle()
        }

        // 设置描述（支持高亮）
        try {
            binding.itemAppDescription.text = EnvironmentUtil.getSpannableString(
                item.getDescription(),
                highlightKeyword,
                highlightColor
            )
        } catch (e: Exception) {
            e.printStackTrace()
            binding.itemAppDescription.text = item.getDescription()
        }

        // 设置大小
        binding.itemAppRight.text = Formatter.formatFileSize(activity, item.getSize())

        // 多选模式处理
        binding.itemAppRight.isVisible = !isMultiSelectMode
        binding.itemAppCb.isVisible = isMultiSelectMode

        if (isMultiSelectMode) {
            binding.itemAppCb.isChecked = selectedPositions?.getOrNull(position) ?: false
        }
    }

    /**
     * 绑定网格模式的项目
     */
    private fun BindingAdapter.BindingViewHolder.bindGridItem(
        item: T,
        position: Int,
        highlightColor: Int
    ) {
        // 使用 BRV 的 getBinding 方法获取 ViewBinding
        val binding = getBinding<ItemAppInfoGridBinding>()

        // 设置图标
        binding.itemAppIcon.setImageDrawable(item.getIconDrawable())

        // 设置标题颜色
        val titleColor = if (item.isRedMarked()) {
            activity.resources.getColor(R.color.colorSystemAppTitleColor)
        } else {
            activity.resources.getColor(R.color.colorHighLightText)
        }
        binding.itemAppTitle.setTextColor(titleColor)

        // 设置标题文本（支持高亮）
        try {
            binding.itemAppTitle.text = EnvironmentUtil.getSpannableString(
                item.getTitle(),
                highlightKeyword,
                highlightColor
            )
        } catch (e: Exception) {
            e.printStackTrace()
            binding.itemAppTitle.text = item.getTitle()
        }

        // 网格模式的背景处理
        if (isMultiSelectMode) {
            val selected = selectedPositions?.getOrNull(position) ?: false
            binding.itemAppRoot.setCardBackgroundColor(
                if (selected) context.colorPrimary else context.colorSurfaceContainer
            )
        } else {
            binding.itemAppRoot.setCardBackgroundColor(activity.colorSurfaceContainer)
        }
    }
}

/**
 * 列表操作监听器接口
 */
interface AppListListener<T> {
    /**
     * 项目被点击
     */
    fun onItemClicked(item: T, holder: BindingAdapter.BindingViewHolder, position: Int)
    
    /**
     * 多选项目变化
     */
    fun onMultiSelectItemChanged(selectedItems: List<T>, totalSize: Long)
    
    /**
     * 多选模式开启
     */
    fun onMultiSelectModeOpened()
}

