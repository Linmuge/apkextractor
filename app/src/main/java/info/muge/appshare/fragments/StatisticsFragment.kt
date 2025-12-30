package info.muge.appshare.fragments

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.toColorInt
import androidx.fragment.app.Fragment
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.google.android.material.chip.Chip
import info.muge.appshare.Global
import info.muge.appshare.R
import info.muge.appshare.items.AppItem
import info.muge.appshare.ui.StatisticsAppListBottomSheet

/**
 * 统计页面 Fragment - Material Design 3 Expressive
 * 显示应用数据统计的可视化图表
 */
class StatisticsFragment : Fragment() {

    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var pieChart: PieChart
    private lateinit var barChart: BarChart
    private lateinit var emptyLayout: View
    private lateinit var chartToggleButton: com.google.android.material.button.MaterialButton

    // 图表类型：true = 饼状图, false = 条形图
    private var isPieChart = true

    // 统计类型
    private enum class StatisticsType {
        TARGET_SDK, MIN_SDK, COMPILE_SDK, KOTLIN, ABI, PAGE_SIZE_16K, APP_BUNDLE, INSTALLER, APP_TYPE
    }

    private var currentType = StatisticsType.TARGET_SDK

    // 统计数据缓存
    private val statisticsCache = mutableMapOf<StatisticsType, Map<String, List<AppItem>>>()

    // MD3 Expressive 色彩方案
    private val chartColors = intArrayOf(
        "#FF6B6B".toColorInt(),  // 珊瑚红
        "#4ECDC4".toColorInt(),  // 青绿
        "#45B7D1".toColorInt(),  // 天蓝
        "#96CEB4".toColorInt(),  // 鼠尾草绿
        "#FFEAA7".toColorInt(),  // 奶油黄
        "#DDA0DD".toColorInt(),  // 梅花紫
        "#98D8C8".toColorInt(),  // 薄荷绿
        "#F7DC6F".toColorInt(),  // 淡黄
        "#BB8FCE".toColorInt(),  // 淡紫
        "#85C1E9".toColorInt(),  // 淡蓝
        "#F8B500".toColorInt(),  // 琥珀
        "#FF6F61".toColorInt()   // 珊瑚
    )

    private val handler = Handler(Looper.getMainLooper())

    // 从主题获取颜色
    private fun getThemeColor(attr: Int): Int {
        val typedValue = TypedValue()
        requireContext().theme.resolveAttribute(attr, typedValue, true)
        return typedValue.data
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_statistics, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 初始化视图
        swipeRefresh = view.findViewById(R.id.statistics_swipe_refresh)
        pieChart = view.findViewById(R.id.pie_chart)
        barChart = view.findViewById(R.id.bar_chart)
        emptyLayout = view.findViewById(R.id.statistics_empty_layout)
        chartToggleButton = view.findViewById(R.id.btn_chart_toggle)

        // 设置图表
        setupCharts()

        // 设置点击事件
        setupClickListeners(view)

        // 设置下拉刷新
        swipeRefresh.setOnRefreshListener {
            loadStatisticsData()
        }

        // 初始加载数据
        loadStatisticsData()
    }

    private fun setupCharts() {
        val onSurfaceColor = getThemeColor(com.google.android.material.R.attr.colorOnSurface)
        val surfaceColor = getThemeColor(com.google.android.material.R.attr.colorSurface)
        val outlineVariantColor = getThemeColor(com.google.android.material.R.attr.colorOutlineVariant)

        // 设置饼状图
        pieChart.apply {
            setUsePercentValues(true)
            description.isEnabled = false
            setExtraOffsets(5f, 10f, 5f, 10f)
            isDrawHoleEnabled = true
            setHoleColor(surfaceColor)
            setTransparentCircleColor(surfaceColor)
            setDrawCenterText(true)
            centerText = "统计"
            setCenterTextSize(18f)
            setCenterTextColor(onSurfaceColor)
            rotationAngle = 0f
            isRotationEnabled = true
            isHighlightPerTapEnabled = true
            dragDecelerationFrictionCoef = 0.95f

            // 图例样式
            legend.apply {
                isEnabled = true
                verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
                horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
                orientation = Legend.LegendOrientation.HORIZONTAL
                setDrawInside(false)
                xEntrySpace = 10f
                yEntrySpace = 5f
                textColor = onSurfaceColor
                textSize = 12f
                form = Legend.LegendForm.CIRCLE
                formSize = 10f
            }

            setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
                override fun onValueSelected(e: com.github.mikephil.charting.data.Entry, h: Highlight?) {
                    if (e is PieEntry) {
                        showAppListForLabel(e.label)
                    }
                }

                override fun onNothingSelected() {}
            })
        }

        // 设置条形图
        barChart.apply {
            description.isEnabled = false
            setDrawGridBackground(false)
            setDrawBarShadow(false)
            setFitBars(true)

            // 图例样式
            legend.apply {
                isEnabled = false
            }

            // X轴样式
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                granularity = 1f
                textColor = onSurfaceColor
                textSize = 11f
            }

            // 左Y轴样式
            axisLeft.apply {
                setDrawGridLines(true)
                gridColor = outlineVariantColor
                gridLineWidth = 0.5f
                textColor = onSurfaceColor
                axisMinimum = 0f
                textSize = 11f
            }

            axisRight.isEnabled = false

            setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
                override fun onValueSelected(e: com.github.mikephil.charting.data.Entry, h: Highlight?) {
                    if (e is BarEntry) {
                        val label = (e.data as? String) ?: ""
                        if (label.isNotEmpty()) {
                            showAppListForLabel(label)
                        }
                    }
                }

                override fun onNothingSelected() {}
            })
        }
    }

    private fun setupClickListeners(view: View) {
        // 统计类型选择
        view.findViewById<Chip>(R.id.chip_target_sdk).setOnClickListener {
            selectType(StatisticsType.TARGET_SDK)
        }

        view.findViewById<Chip>(R.id.chip_min_sdk).setOnClickListener {
            selectType(StatisticsType.MIN_SDK)
        }

        view.findViewById<Chip>(R.id.chip_compile_sdk).setOnClickListener {
            selectType(StatisticsType.COMPILE_SDK)
        }

        view.findViewById<Chip>(R.id.chip_kotlin).setOnClickListener {
            selectType(StatisticsType.KOTLIN)
        }

        view.findViewById<Chip>(R.id.chip_abi).setOnClickListener {
            selectType(StatisticsType.ABI)
        }

        view.findViewById<Chip>(R.id.chip_16k).setOnClickListener {
            selectType(StatisticsType.PAGE_SIZE_16K)
        }

        view.findViewById<Chip>(R.id.chip_app_bundle).setOnClickListener {
            selectType(StatisticsType.APP_BUNDLE)
        }

        view.findViewById<Chip>(R.id.chip_installer).setOnClickListener {
            selectType(StatisticsType.INSTALLER)
        }

        view.findViewById<Chip>(R.id.chip_app_type).setOnClickListener {
            selectType(StatisticsType.APP_TYPE)
        }

        // 图表类型切换
        chartToggleButton.setOnClickListener {
            isPieChart = !isPieChart
            updateChartToggleButton()
            updateChartVisibility()
            updateChartData()
        }
    }

    private fun updateChartToggleButton() {
        val iconRes = if (isPieChart) {
            R.drawable.ic_chart_pie
        } else {
            R.drawable.ic_chart_pie // 可以创建不同的图标
        }
        chartToggleButton.setIconResource(iconRes)
    }

    private fun selectType(type: StatisticsType) {
        currentType = type
        updateChipSelection()
        loadStatisticsData()
    }

    private fun updateChipSelection() {
        view?.let { v ->
            v.findViewById<Chip>(R.id.chip_target_sdk).isChecked =
                currentType == StatisticsType.TARGET_SDK
            v.findViewById<Chip>(R.id.chip_min_sdk).isChecked =
                currentType == StatisticsType.MIN_SDK
            v.findViewById<Chip>(R.id.chip_compile_sdk).isChecked =
                currentType == StatisticsType.COMPILE_SDK
            v.findViewById<Chip>(R.id.chip_kotlin).isChecked =
                currentType == StatisticsType.KOTLIN
            v.findViewById<Chip>(R.id.chip_abi).isChecked = currentType == StatisticsType.ABI
            v.findViewById<Chip>(R.id.chip_16k).isChecked =
                currentType == StatisticsType.PAGE_SIZE_16K
            v.findViewById<Chip>(R.id.chip_app_bundle).isChecked =
                currentType == StatisticsType.APP_BUNDLE
            v.findViewById<Chip>(R.id.chip_installer).isChecked =
                currentType == StatisticsType.INSTALLER
            v.findViewById<Chip>(R.id.chip_app_type).isChecked =
                currentType == StatisticsType.APP_TYPE
        }
    }

    private fun updateChartVisibility() {
        if (isPieChart) {
            pieChart.visibility = View.VISIBLE
            barChart.visibility = View.GONE
        } else {
            pieChart.visibility = View.GONE
            barChart.visibility = View.VISIBLE
        }
    }

    private fun loadStatisticsData() {
        swipeRefresh.isRefreshing = true

        Thread {
            val statistics = collectStatistics(currentType)
            statisticsCache[currentType] = statistics

            handler.post {
                if (!isAdded) return@post
                updateChartData()
                swipeRefresh.isRefreshing = false
            }
        }.start()
    }

    private fun collectStatistics(type: StatisticsType): Map<String, List<AppItem>> {
        val result = mutableMapOf<String, MutableList<AppItem>>()

        synchronized(Global.app_list) {
            Global.app_list.forEach { app ->
                try {
                    val packageInfo = app.getPackageInfo()
                    val appInfo = packageInfo.applicationInfo ?: return@forEach

                    val key = when (type) {
                        StatisticsType.TARGET_SDK -> {
                            "API ${appInfo.targetSdkVersion}"
                        }

                        StatisticsType.MIN_SDK -> {
                            "API ${appInfo.minSdkVersion}"
                        }

                        StatisticsType.COMPILE_SDK -> {
                            "API ${appInfo.targetSdkVersion}"
                        }

                        StatisticsType.KOTLIN -> {
                            if (hasKotlinClasses(app)) "有 Kotlin" else "无 Kotlin"
                        }

                        StatisticsType.ABI -> {
                            getAbis(app).firstOrNull() ?: "无 Native"
                        }

                        StatisticsType.PAGE_SIZE_16K -> {
                            if (is16kPageSize(app)) "16K" else "非 16K"
                        }

                        StatisticsType.APP_BUNDLE -> {
                            if (isAppBundle(app)) "Bundle" else "APK"
                        }

                        StatisticsType.INSTALLER -> {
                            app.getInstallSource()
                        }

                        StatisticsType.APP_TYPE -> {
                            if (app.isRedMarked()) "系统应用" else "用户应用"
                        }
                    }

                    result.getOrPut(key) { mutableListOf() }.add(app)
                } catch (e: Exception) {
                    // 忽略错误
                }
            }
        }

        return result
    }

    private fun updateChartData() {
        val statistics = statisticsCache[currentType] ?: emptyMap()

        if (statistics.isEmpty()) {
            emptyLayout.visibility = View.VISIBLE
            pieChart.visibility = View.GONE
            barChart.visibility = View.GONE
            return
        }

        emptyLayout.visibility = View.GONE
        pieChart.visibility = if (isPieChart) View.VISIBLE else View.GONE
        barChart.visibility = if (isPieChart) View.GONE else View.VISIBLE

        // 准备数据，按数量排序
        val sortedEntries = statistics.entries
            .sortedByDescending { it.value.size }

        if (isPieChart) {
            updatePieChart(sortedEntries)
        } else {
            updateBarChart(sortedEntries)
        }
    }

    private fun updatePieChart(sortedEntries: List<Map.Entry<String, List<AppItem>>>) {
        val onSurfaceColor = getThemeColor(com.google.android.material.R.attr.colorOnSurface)
        
        val entries = sortedEntries.mapIndexed { index, entry ->
            PieEntry(entry.value.size.toFloat(), entry.key, index)
        }

        val dataSet = PieDataSet(entries, "").apply {
            setDrawIcons(false)
            sliceSpace = 2f
            setColors(*chartColors)
            selectionShift = 8f
            // 饼状图内部的文字颜色使用白色以保证可读性
            valueTextColor = android.graphics.Color.WHITE
        }

        val data = PieData(dataSet).apply {
            setValueFormatter(PercentFormatter(pieChart))
            setValueTextSize(12f)
            setDrawValues(true)
        }

        pieChart.data = data
        pieChart.centerText = "总计\n${sortedEntries.sumOf { it.value.size }}"
        pieChart.setCenterTextColor(onSurfaceColor)
        pieChart.invalidate()
    }

    private fun updateBarChart(sortedEntries: List<Map.Entry<String, List<AppItem>>>) {
        val onSurfaceColor = getThemeColor(com.google.android.material.R.attr.colorOnSurface)
        val maxCount = sortedEntries.maxOfOrNull { it.value.size }?.toFloat() ?: 0f

        val entries = sortedEntries.mapIndexed { index, entry ->
            BarEntry(index.toFloat(), entry.value.size.toFloat(), entry.key)
        }

        val labels = sortedEntries.map { it.key }

        val dataSet = BarDataSet(entries, "").apply {
            setColors(*chartColors)
            setDrawValues(true)
            valueTextSize = 11f
            valueTextColor = onSurfaceColor
        }

        val data = BarData(dataSet)
        barChart.data = data

        barChart.xAxis.textColor = onSurfaceColor
        barChart.axisLeft.textColor = onSurfaceColor
        barChart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        barChart.xAxis.labelCount = labels.size.coerceAtMost(5)
        barChart.axisLeft.axisMaximum = maxCount * 1.1f

        barChart.invalidate()
    }

    private fun showAppListForLabel(label: String) {
        val apps = statisticsCache[currentType]?.get(label) ?: emptyList()
        if (apps.isEmpty()) {
            "暂无应用".let {
                android.widget.Toast.makeText(
                    requireContext(),
                    it,
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
            return
        }

        StatisticsAppListBottomSheet.newInstance(label, apps)
            .show(childFragmentManager, "app_list_bottom_sheet")
    }

    // 辅助方法
    private fun hasKotlinClasses(app: AppItem): Boolean {
        return try {
            val nativeLibDir = app.getSourcePath().replace("base.apk", "lib")
            val libFile = java.io.File(nativeLibDir)
            libFile.exists() && libFile.listFiles()?.any { dir ->
                dir.listFiles()?.any { file ->
                    file.name.contains("kotlin", ignoreCase = true)
                } == true
            } == true
        } catch (e: Exception) {
            false
        }
    }

    private fun getAbis(app: AppItem): List<String> {
        return try {
            val nativeLibDir = app.getSourcePath().replace("base.apk", "lib")
            val libFile = java.io.File(nativeLibDir)
            if (libFile.exists()) {
                libFile.listFiles()?.map { it.name } ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun is16kPageSize(app: AppItem): Boolean {
        return try {
            val nativeLibDir = app.getSourcePath().replace("base.apk", "lib")
            val libFile = java.io.File(nativeLibDir)
            libFile.exists() && libFile.listFiles()?.map { it.name }?.contains("arm64-v8a") == true
        } catch (e: Exception) {
            false
        }
    }

    private fun isAppBundle(app: AppItem): Boolean {
        return try {
            app.getSourcePath().contains("split_config") ||
                    !app.getSourcePath().endsWith("base.apk")
        } catch (e: Exception) {
            false
        }
    }

    override fun onResume() {
        super.onResume()
        // 刷新数据
        loadStatisticsData()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacksAndMessages(null)
    }
}
