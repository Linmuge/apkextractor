package info.muge.appshare.ui.dialogs

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * 弹窗样式规范
 */
object DialogStyles {

    /**
     * 获取规范的 BottomSheet 形状：全屏直角，非全屏大圆角
     */
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun getBottomSheetShape(sheetState: SheetState): Shape {
        return if (sheetState.targetValue == SheetValue.Expanded) {
            RectangleShape
        } else {
            RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        }
    }
}
