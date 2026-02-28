package info.muge.appshare.utils

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

/**
 * 图表截图工具类
 * 用于将 Compose 组件捕获为图片并分享
 */
object ChartCaptureUtil {

    /**
     * 将 Bitmap 保存到缓存文件
     * @return 文件路径，失败返回 null
     */
    fun saveBitmapToCache(
        context: Context,
        bitmap: Bitmap,
        fileName: String = "chart_${System.currentTimeMillis()}.png"
    ): String? {
        return try {
            val cacheDir = File(context.cacheDir, "share")
            if (!cacheDir.exists()) {
                cacheDir.mkdirs()
            }
            val file = File(cacheDir, fileName)

            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            file.absolutePath
        } catch (e: Exception) {
            LogUtil.e("Failed to save bitmap", e, "ChartCaptureUtil")
            null
        }
    }

    /**
     * 创建分享图片的 Intent
     */
    fun createShareIntent(context: Context, filePath: String, title: String = "Share Chart"): Intent? {
        return try {
            val file = File(filePath)
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } catch (e: Exception) {
            LogUtil.e("Failed to create share intent", e, "ChartCaptureUtil")
            null
        }
    }

    /**
     * 从 View 创建 Bitmap（传统 View 系统使用）
     */
    fun captureViewToBitmap(view: android.view.View): Bitmap {
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        view.draw(canvas)
        return bitmap
    }

    /**
     * 创建带水印的图片
     */
    fun createWatermarkedBitmap(
        sourceBitmap: Bitmap,
        watermarkText: String,
        textColor: Int = android.graphics.Color.GRAY
    ): Bitmap {
        val result = Bitmap.createBitmap(
            sourceBitmap.width,
            sourceBitmap.height + 60,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(result)

        // 绘制原图
        canvas.drawBitmap(sourceBitmap, 0f, 0f, null)

        // 绘制水印背景
        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#F5F5F5")
            style = android.graphics.Paint.Style.FILL
        }
        canvas.drawRect(
            0f,
            sourceBitmap.height.toFloat(),
            sourceBitmap.width.toFloat(),
            result.height.toFloat(),
            paint
        )

        // 绘制水印文字
        val textPaint = android.graphics.Paint().apply {
            color = textColor
            textSize = 28f
            isAntiAlias = true
            textAlign = android.graphics.Paint.Align.CENTER
        }
        canvas.drawText(
            watermarkText,
            sourceBitmap.width / 2f,
            sourceBitmap.height + 40f,
            textPaint
        )

        return result
    }

    /**
     * 合并多个 Bitmap 为一个长图
     */
    fun mergeBitmapsVertically(bitmaps: List<Bitmap>): Bitmap? {
        if (bitmaps.isEmpty()) return null

        val totalHeight = bitmaps.sumOf { it.height }
        val maxWidth = bitmaps.maxOf { it.width }

        val result = Bitmap.createBitmap(maxWidth, totalHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)

        var currentTop = 0f
        bitmaps.forEach { bitmap ->
            canvas.drawBitmap(bitmap, 0f, currentTop, null)
            currentTop += bitmap.height
        }

        return result
    }
}
