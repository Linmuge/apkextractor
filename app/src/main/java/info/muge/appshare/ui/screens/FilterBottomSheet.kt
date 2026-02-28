package info.muge.appshare.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import info.muge.appshare.ui.dialogs.DialogStyles
import info.muge.appshare.ui.theme.AppDimens

/**
 * 筛选底部弹窗
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FilterBottomSheet(
    currentConfig: FilterConfig,
    availableInstallers: List<String>,
    onApply: (FilterConfig) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var appType by remember { mutableStateOf(currentConfig.appType) }
    var sizeRange by remember { mutableStateOf(currentConfig.sizeRange) }
    var selectedInstallers by remember { mutableStateOf(currentConfig.installerSources) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = AppDimens.Space.lg)
                .padding(bottom = AppDimens.Space.xxl)
        ) {
            // 标题行
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "筛选",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = {
                    appType = AppTypeFilter.ALL
                    sizeRange = SizeRange.ALL
                    selectedInstallers = emptySet()
                    onApply(
                        FilterConfig(
                            appType = AppTypeFilter.ALL,
                            sizeRange = SizeRange.ALL,
                            installerSources = emptySet()
                        )
                    )
                }) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "重置",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(AppDimens.Space.lg))

            // 应用类型
            SectionLabel("应用类型")
            Spacer(modifier = Modifier.height(AppDimens.Space.sm))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(AppDimens.Space.sm)
            ) {
                AppTypeFilter.entries.forEach { type ->
                    FilterChip(
                        selected = appType == type,
                        onClick = { appType = type },
                        label = { Text(type.label, style = MaterialTheme.typography.labelMedium) },
                        shape = RoundedCornerShape(AppDimens.Radius.full)
                    )
                }
            }

            Spacer(modifier = Modifier.height(AppDimens.Space.lg))

            // 大小范围
            SectionLabel("大小范围")
            Spacer(modifier = Modifier.height(AppDimens.Space.sm))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(AppDimens.Space.sm),
                verticalArrangement = Arrangement.spacedBy(AppDimens.Space.xs)
            ) {
                SizeRange.entries.forEach { range ->
                    FilterChip(
                        selected = sizeRange == range,
                        onClick = { sizeRange = range },
                        label = { Text(range.label, style = MaterialTheme.typography.labelMedium) },
                        shape = RoundedCornerShape(AppDimens.Radius.full)
                    )
                }
            }

            // 安装来源（仅在有数据时显示）
            if (availableInstallers.isNotEmpty()) {
                Spacer(modifier = Modifier.height(AppDimens.Space.lg))

                SectionLabel("安装来源")
                Spacer(modifier = Modifier.height(AppDimens.Space.sm))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(AppDimens.Space.sm),
                    verticalArrangement = Arrangement.spacedBy(AppDimens.Space.xs)
                ) {
                    availableInstallers.forEach { installer ->
                        FilterChip(
                            selected = selectedInstallers.contains(installer),
                            onClick = {
                                selectedInstallers = if (selectedInstallers.contains(installer)) {
                                    selectedInstallers - installer
                                } else {
                                    selectedInstallers + installer
                                }
                            },
                            label = {
                                Text(
                                    text = installer,
                                    style = MaterialTheme.typography.labelMedium,
                                    maxLines = 1
                                )
                            },
                            shape = RoundedCornerShape(AppDimens.Radius.full)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(AppDimens.Space.xxl))

            // 按钮行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppDimens.Space.md)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(AppDimens.Radius.full)
                ) {
                    Text("取消")
                }
                Button(
                    onClick = {
                        onApply(
                            FilterConfig(
                                appType = appType,
                                sizeRange = sizeRange,
                                installerSources = selectedInstallers
                            )
                        )
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(AppDimens.Radius.full)
                ) {
                    Text("应用筛选")
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary
    )
}
