package info.muge.appshare.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val ALPHABET = ('A'..'Z').toList() + '#'

/**
 * 字母索引条组件
 * 在列表右侧显示 A-Z + # 字母列表，支持触摸和滑动快速跳转
 */
@Composable
fun AlphabetIndexBar(
    onLetterSelected: (Char) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    var isDragging by remember { mutableStateOf(false) }
    var currentLetter by remember { mutableStateOf<Char?>(null) }
    var barHeightPx by remember { mutableStateOf(0) }

    val bgAlpha by animateFloatAsState(
        targetValue = if (isDragging) 0.12f else 0f,
        animationSpec = tween(150),
        label = "bgAlpha"
    )

    Box(modifier = modifier) {
        // 选中字母气泡提示
        if (isDragging && currentLetter != null) {
            val letterIndex = ALPHABET.indexOf(currentLetter!!)
            val itemHeight = if (ALPHABET.isNotEmpty() && barHeightPx > 0) {
                barHeightPx.toFloat() / ALPHABET.size
            } else 0f
            val offsetY = (letterIndex * itemHeight + itemHeight / 2 - with(density) { 20.dp.toPx() }).toInt()

            Box(
                modifier = Modifier
                    .offset { IntOffset(-with(density) { 44.dp.roundToPx() }, offsetY) }
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = currentLetter.toString(),
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // 字母列表
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .width(24.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = bgAlpha))
                .onSizeChanged { barHeightPx = it.height }
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragStart = { offset ->
                            isDragging = true
                            val index = ((offset.y / size.height) * ALPHABET.size)
                                .toInt()
                                .coerceIn(0, ALPHABET.lastIndex)
                            currentLetter = ALPHABET[index]
                            onLetterSelected(ALPHABET[index])
                        },
                        onDragEnd = {
                            isDragging = false
                            currentLetter = null
                        },
                        onDragCancel = {
                            isDragging = false
                            currentLetter = null
                        },
                        onVerticalDrag = { _, _ ->
                            // 使用 pointerInput 的 currentEvent 来获取当前位置
                        }
                    )
                }
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            if (isDragging) {
                                val y = event.changes.firstOrNull()?.position?.y ?: continue
                                val index = ((y / size.height) * ALPHABET.size)
                                    .toInt()
                                    .coerceIn(0, ALPHABET.lastIndex)
                                val letter = ALPHABET[index]
                                if (letter != currentLetter) {
                                    currentLetter = letter
                                    onLetterSelected(letter)
                                }
                            }
                        }
                    }
                }
                .padding(vertical = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            ALPHABET.forEach { letter ->
                Text(
                    text = letter.toString(),
                    fontSize = 9.sp,
                    fontWeight = if (letter == currentLetter) FontWeight.Bold else FontWeight.Normal,
                    color = if (letter == currentLetter) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
