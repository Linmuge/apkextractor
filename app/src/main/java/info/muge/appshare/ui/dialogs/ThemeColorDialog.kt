package info.muge.appshare.ui.dialogs

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp

/**
 * 预设主题颜色
 */
data class ThemeColorOption(
    val name: String,
    val color: Color
)

val presetThemeColors = listOf(
    ThemeColorOption("蓝色", Color(0xFF4285F4)),
    ThemeColorOption("靛蓝", Color(0xFF3F51B5)),
    ThemeColorOption("紫色", Color(0xFF9C27B0)),
    ThemeColorOption("深紫", Color(0xFF673AB7)),
    ThemeColorOption("粉色", Color(0xFFE91E63)),
    ThemeColorOption("红色", Color(0xFFF44336)),
    ThemeColorOption("橙色", Color(0xFFFF9800)),
    ThemeColorOption("黄色", Color(0xFFFFC107)),
    ThemeColorOption("青绿", Color(0xFF009688)),
    ThemeColorOption("绿色", Color(0xFF4CAF50)),
    ThemeColorOption("青色", Color(0xFF00BCD4)),
    ThemeColorOption("棕色", Color(0xFF795548))
)

/**
 * 主题颜色选择对话框
 */
@Composable
fun ThemeColorDialog(
    currentColor: Color,
    onDismiss: () -> Unit,
    onConfirm: (Color) -> Unit
) {
    var selectedColor by remember { mutableStateOf(currentColor) }
    val animatedBorderColor by animateColorAsState(
        targetValue = selectedColor,
        animationSpec = tween(300),
        label = "borderColor"
    )

    AppBottomSheet(
        title = "选择主题色",
        onDismiss = onDismiss,
        content = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = "选择一个颜色作为应用的主题色",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(20.dp))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(presetThemeColors) { option ->
                        val isSelected = colorsMatch(selectedColor, option.color)
                        ColorCircle(
                            color = option.color,
                            name = option.name,
                            isSelected = isSelected,
                            onClick = { selectedColor = option.color }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        },
        actions = {
            AppBottomSheetDualActions(
                confirmText = "确定",
                dismissText = "取消",
                onConfirm = { onConfirm(selectedColor) },
                onDismiss = onDismiss
            )
        }
    )
}

@Composable
private fun ColorCircle(
    color: Color,
    name: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(color)
                .then(
                    if (isSelected) {
                        Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                    } else {
                        Modifier
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "已选择",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * 比较两个颜色是否匹配（忽略 alpha 差异）
 */
private fun colorsMatch(a: Color, b: Color): Boolean {
    return a.toArgb() == b.toArgb()
}
