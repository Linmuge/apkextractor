package info.muge.appshare.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import info.muge.appshare.R

/**
 * 条形图适配器
 */
class BarChartAdapter : RecyclerView.Adapter<BarChartAdapter.ViewHolder>() {

    data class BarData(
        val label: String,
        val count: Int,
        val maxCount: Int
    )

    private var items: List<BarData> = listOf()
    private var onItemClickListener: ((String) -> Unit)? = null

    fun setOnItemClickListener(listener: (String) -> Unit) {
        onItemClickListener = listener
    }

    fun submitList(newItems: List<BarData>) {
        items = newItems
        notifyDataSetChanged()
    }

    class ViewHolder(view: android.view.View) : RecyclerView.ViewHolder(view) {
        val label: TextView = view.findViewById(R.id.bar_chart_label)
        val progress: ProgressBar = view.findViewById(R.id.bar_chart_progress)
        val count: TextView = view.findViewById(R.id.bar_chart_count)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_bar_chart, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.label.text = item.label
        holder.count.text = item.count.toString()

        // 计算进度百分比
        val progress = if (item.maxCount > 0) {
            (item.count.toFloat() / item.maxCount * 100).toInt()
        } else {
            0
        }
        holder.progress.max = 100
        holder.progress.progress = progress

        holder.itemView.setOnClickListener {
            onItemClickListener?.invoke(item.label)
        }
    }

    override fun getItemCount(): Int = items.size
}
