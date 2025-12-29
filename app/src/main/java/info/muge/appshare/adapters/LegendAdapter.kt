package info.muge.appshare.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import info.muge.appshare.R

/**
 * 图例适配器
 */
class LegendAdapter : RecyclerView.Adapter<LegendAdapter.ViewHolder>() {

    // 预定义颜色
    private val chartColors = intArrayOf(
        0xFF6B6B.toInt(),
        0x4ECDC4.toInt(),
        0x45B7D1.toInt(),
        0x96CEB4.toInt(),
        0xFFEEAD.toInt(),
        0xD4A5A5.toInt(),
        0x9B59B6.toInt(),
        0x3498DB.toInt(),
        0xE67E22.toInt(),
        0x2ECC71.toInt(),
        0x1ABC9C.toInt(),
        0xE74C3C.toInt(),
    )

    data class LegendItem(
        val label: String,
        val count: Int,
        val color: Int = Color.WHITE
    )

    private var items: List<LegendItem> = listOf()
    private var onItemClickListener: ((String) -> Unit)? = null

    fun setOnItemClickListener(listener: (String) -> Unit) {
        onItemClickListener = listener
    }

    fun submitList(newItems: List<LegendItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val colorIndicator: View = view.findViewById(R.id.legend_color)
        val label: TextView = view.findViewById(R.id.legend_label)
        val count: TextView = view.findViewById(R.id.legend_count)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chart_legend, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.label.text = item.label
        holder.count.text = "(${item.count})"

        // 设置颜色
        val color = chartColors[position % chartColors.size]
        holder.colorIndicator.setBackgroundColor(color)

        holder.itemView.setOnClickListener {
            onItemClickListener?.invoke(item.label)
        }
    }

    override fun getItemCount(): Int = items.size
}
