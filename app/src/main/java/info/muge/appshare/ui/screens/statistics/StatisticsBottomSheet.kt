package info.muge.appshare.ui.screens.statistics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import info.muge.appshare.items.AppItem
import info.muge.appshare.ui.dialogs.DialogStyles
import info.muge.appshare.ui.theme.AppDimens
import java.util.Locale

private enum class SheetSortMode {
    SIZE_DESC,
    SIZE_ASC,
    NAME_ASC
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun StatisticsAppListBottomSheet(
    title: String,
    apps: List<AppItem>,
    onDismiss: () -> Unit,
    onAppClick: (AppItem) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val appListState = rememberLazyListState()

    var query by remember { mutableStateOf("") }
    var sortMode by remember { mutableStateOf(SheetSortMode.SIZE_DESC) }

    val filteredApps = remember(apps, query, sortMode) {
        val queryLower = query.trim().lowercase(Locale.getDefault())
        val filtered = if (queryLower.isEmpty()) {
            apps
        } else {
            apps.filter {
                it.getAppName().lowercase(Locale.getDefault()).contains(queryLower) ||
                    it.getPackageName().lowercase(Locale.getDefault()).contains(queryLower)
            }
        }

        when (sortMode) {
            SheetSortMode.SIZE_DESC -> filtered.sortedByDescending { it.getSize() }
            SheetSortMode.SIZE_ASC -> filtered.sortedBy { it.getSize() }
            SheetSortMode.NAME_ASC -> filtered.sortedBy { it.getAppName().lowercase(Locale.getDefault()) }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        sheetGesturesEnabled = true,
        shape = DialogStyles.getBottomSheetShape(sheetState = sheetState),
        tonalElevation = 0.dp,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = AppDimens.Space.md, bottom = AppDimens.Space.xs)
                    .size(width = 48.dp, height = 4.dp)
                    .clip(RoundedCornerShape(AppDimens.Radius.full))
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )
        }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // 固定顶部：标题 + 搜索 + 排序
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppDimens.Space.lg)
            ) {
                // 标题行
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(AppDimens.Radius.full))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(
                                horizontal = 12.dp,
                                vertical = 6.dp
                            )
                    ) {
                        Text(
                            text = "${filteredApps.size} 个",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.height(AppDimens.Space.md))

                // 搜索框
                TextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    placeholder = {
                        Text(
                            "搜索应用名称或包名",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    textStyle = MaterialTheme.typography.bodyMedium,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(AppDimens.Radius.xl)
                )

                Spacer(modifier = Modifier.height(AppDimens.Space.sm))

                // 排序选项
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppDimens.Space.sm)
                ) {
                    FilterChip(
                        selected = sortMode == SheetSortMode.SIZE_DESC,
                        onClick = { sortMode = SheetSortMode.SIZE_DESC },
                        label = { Text("大小↓", style = MaterialTheme.typography.labelMedium) },
                        shape = RoundedCornerShape(AppDimens.Radius.full)
                    )
                    FilterChip(
                        selected = sortMode == SheetSortMode.SIZE_ASC,
                        onClick = { sortMode = SheetSortMode.SIZE_ASC },
                        label = { Text("大小↑", style = MaterialTheme.typography.labelMedium) },
                        shape = RoundedCornerShape(AppDimens.Radius.full)
                    )
                    FilterChip(
                        selected = sortMode == SheetSortMode.NAME_ASC,
                        onClick = { sortMode = SheetSortMode.NAME_ASC },
                        label = { Text("名称", style = MaterialTheme.typography.labelMedium) },
                        shape = RoundedCornerShape(AppDimens.Radius.full)
                    )
                }

                Spacer(modifier = Modifier.height(AppDimens.Space.sm))
            }

            // 可滚动应用列表
            LazyColumn(
                state = appListState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .imePadding(),
                contentPadding = PaddingValues(
                    start = AppDimens.Space.lg,
                    end = AppDimens.Space.lg,
                    bottom = AppDimens.Space.xl
                ),
                verticalArrangement = Arrangement.spacedBy(AppDimens.Space.sm),
                overscrollEffect = null
            ) {
                if (filteredApps.isEmpty()) {
                    item {
                        Text(
                            text = if (query.isBlank()) "暂无应用数据" else "没有匹配的应用",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 40.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    items(filteredApps) { app ->
                        StatisticsAppListItem(
                            app = app,
                            onClick = { onAppClick(app) }
                        )
                    }
                }
            }
        }
    }
}
