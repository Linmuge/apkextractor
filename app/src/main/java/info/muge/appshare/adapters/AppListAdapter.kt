package info.muge.appshare.adapters

import android.app.Activity
import android.graphics.Color
import android.text.format.Formatter
import android.view.View
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.drake.brv.BindingAdapter
import com.drake.brv.utils.models
import com.drake.brv.utils.setup
import info.muge.appshare.DisplayItem
import info.muge.appshare.R
import info.muge.appshare.utils.EnvironmentUtil

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
        // 设置LayoutManager
        setLayoutManagerAndView(viewMode)
        
        // 配置BRV适配器
        adapter = recyclerView.setup {
            // 根据viewMode添加不同的布局类型
            addType<DisplayItem>(if (viewMode == 0) R.layout.item_app_info_linear else R.layout.item_app_info_grid)
            
            // 绑定数据
            onBind {
                val item = getModel<DisplayItem>() as T
                val position = layoutPosition
                
                // 获取视图
                val root = itemView.findViewById<View>(R.id.item_app_root)
                val icon = itemView.findViewById<ImageView>(R.id.item_app_icon)
                val title = itemView.findViewById<TextView>(R.id.item_app_title)
                
                // 设置图标
                icon.setImageDrawable(item.getIconDrawable())

                // 设置标题颜色
                val titleColor = if (item.isRedMarked()) {
                    activity.resources.getColor(R.color.colorSystemAppTitleColor)
                } else {
                    activity.resources.getColor(R.color.colorHighLightText)
                }
                title.setTextColor(titleColor)

                // 设置标题文本（支持高亮）
                try {
                    title.text = EnvironmentUtil.getSpannableString(
                        item.getTitle(),
                        highlightKeyword,
                        Color.parseColor("#4285F4")
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                    title.text = item.getTitle()
                }
                
                // 列表模式特有的视图
                if (this@AppListAdapter.viewMode == 0) {
                    val description = itemView.findViewById<TextView>(R.id.item_app_description)
                    val right = itemView.findViewById<TextView>(R.id.item_app_right)
                    val cb = itemView.findViewById<CheckBox>(R.id.item_app_cb)
                    
                    // 设置描述（支持高亮）
                    try {
                        description.text = EnvironmentUtil.getSpannableString(
                            item.getDescription(),
                            highlightKeyword,
                            Color.parseColor("#4285F4")
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                        description.text = item.getDescription()
                    }

                    // 设置大小
                    right.text = Formatter.formatFileSize(activity, item.getSize())
                    
                    // 多选模式处理
                    right.visibility = if (isMultiSelectMode) View.GONE else View.VISIBLE
                    cb.visibility = if (isMultiSelectMode) View.VISIBLE else View.GONE
                    
                    if (isMultiSelectMode) {
                        cb.isChecked = selectedPositions?.getOrNull(position) ?: false
                    }
                } else {
                    // 网格模式的背景处理
                    if (isMultiSelectMode) {
                        val selected = selectedPositions?.getOrNull(position) ?: false
                        root.setBackgroundColor(
                            activity.resources.getColor(
                                if (selected) R.color.colorSelectedBackground else R.color.colorCardArea
                            )
                        )
                    } else {
                        root.setBackgroundColor(activity.resources.getColor(R.color.colorCardArea))
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
        this.viewMode = mode
        recyclerView.layoutManager = if (mode == 1) {
            GridLayoutManager(activity, 4)
        } else {
            LinearLayoutManager(activity).apply {
                orientation = LinearLayoutManager.VERTICAL
            }
        }
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

