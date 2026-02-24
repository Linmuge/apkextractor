package info.muge.appshare.ui.screens

import android.text.format.Formatter
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import info.muge.appshare.R
import info.muge.appshare.items.AppItem
import info.muge.appshare.ui.theme.AppDimens
import info.muge.appshare.utils.PermissionExts
import java.util.Locale

/**
 * 加载中内容 - MD3 风格
 */
@Composable
internal fun LoadingContent(
    current: Int,
    total: Int
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 进度环
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(120.dp)
        ) {
            CircularProgressIndicator(
                progress = { if (total > 0) current.toFloat() / total else 0f },
                modifier = Modifier.size(100.dp),
                strokeWidth = 6.dp,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                color = MaterialTheme.colorScheme.primary
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (total > 0) "${(current * 100 / total)}%" else "...",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "$current/$total",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.dialog_loading_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(AppDimens.Space.sm))

        Text(
            text = stringResource(R.string.loading_scanning_apps),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * 空内容 - MD3 风格
 */
@Composable
internal fun EmptyContent() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.size(80.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Apps,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(AppDimens.Space.lg))

        Text(
            text = stringResource(R.string.word_content_blank),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(AppDimens.Space.sm))

        Text(
            text = stringResource(R.string.empty_pull_to_refresh),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * 搜索无结果 - MD3 风格
 */
@Composable
internal fun SearchEmptyContent() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.size(80.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.SearchOff,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(AppDimens.Space.lg))

        Text(
            text = stringResource(R.string.word_content_blank),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(AppDimens.Space.sm))

        Text(
            text = stringResource(R.string.search_no_result),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
internal fun PermissionBottomBar(
    onPermissionGranted: () -> Unit
) {
    val context = LocalContext.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainer,
        shadowElevation = AppDimens.Elevation.none,
        tonalElevation = AppDimens.Elevation.none
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppDimens.Space.lg, vertical = AppDimens.Space.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(AppDimens.Radius.md),
                color = MaterialTheme.colorScheme.errorContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Android,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(AppDimens.Space.md))
            Text(
                text = stringResource(R.string.permission_read_app_list),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )

            FilledTonalButton(
                onClick = {
                    PermissionExts.requestreadInstallApps(context as android.app.Activity) {
                        onPermissionGranted()
                    }
                }
            ) {
                Text(stringResource(R.string.permission_grant_short))
            }
        }
    }
}

/**
 * 多选卡片 - MD3 风格
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun MultiSelectCard(
    selectedCount: Int,
    selectedSize: Long,
    onSelectAll: () -> Unit,
    onInvertSelection: () -> Unit,
    onDeselectAll: () -> Unit,
    onCopyPackageNames: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = AppDimens.Radius.xl, topEnd = AppDimens.Radius.xl),
        color = MaterialTheme.colorScheme.secondaryContainer,
        shadowElevation = AppDimens.Elevation.none,
        tonalElevation = AppDimens.Elevation.none
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppDimens.Space.xl, vertical = AppDimens.Space.lg),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppDimens.Space.sm)
            ) {
                Surface(
                    shape = RoundedCornerShape(AppDimens.Radius.sm),
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Text(
                        text = "$selectedCount",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(horizontal = AppDimens.Space.md, vertical = AppDimens.Space.xs)
                    )
                }

                Text(
                    text = stringResource(R.string.unit_item),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )

                Text(
                    text = "·",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.5f)
                )

                Text(
                    text = Formatter.formatFileSize(LocalContext.current, selectedSize),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            Spacer(modifier = Modifier.height(AppDimens.Space.md))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(AppDimens.Space.sm),
                verticalArrangement = Arrangement.spacedBy(AppDimens.Space.xs)
            ) {
                FilledTonalButton(onClick = onSelectAll) {
                    Text(
                        text = stringResource(R.string.select_all_change),
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                FilledTonalButton(onClick = onInvertSelection) {
                    Text(
                        text = stringResource(R.string.invert_selection),
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                FilledTonalButton(onClick = onDeselectAll) {
                    Text(
                        text = stringResource(R.string.deselect_all),
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                FilledTonalButton(onClick = onCopyPackageNames) {
                    Text(
                        text = stringResource(R.string.copy_package_names),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

/**
 * 列表项 - 线性模式，MD3 风格
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun LinearAppItem(
    app: AppItem,
    isSelected: Boolean,
    isMultiSelectMode: Boolean,
    highlightKeyword: String?,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppDimens.Space.md, vertical = AppDimens.Space.xs)
            .clip(RoundedCornerShape(AppDimens.Radius.lg))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(AppDimens.Radius.lg),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = AppDimens.Elevation.none)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppDimens.Space.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(app.getIcon())
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(AppDimens.Radius.md)),
                    contentScale = ContentScale.Crop
                )

                androidx.compose.animation.AnimatedVisibility(
                    visible = isSelected && isMultiSelectMode,
                    enter = scaleIn(spring(stiffness = Spring.StiffnessMedium)),
                    exit = scaleOut(spring(stiffness = Spring.StiffnessMedium))
                ) {
                    Surface(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(AppDimens.Radius.md)),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp)
            ) {
                Text(
                    text = highlightText(app.getAppName(), highlightKeyword),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (app.isRedMarked()) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = highlightText(app.getPackageName(), highlightKeyword),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }

            if (isMultiSelectMode) {
                Surface(
                    shape = RoundedCornerShape(AppDimens.Radius.sm),
                    color = if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier
                            .size(28.dp)
                            .padding(4.dp),
                        tint = if (isSelected) MaterialTheme.colorScheme.onPrimary
                               else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0f)
                    )
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(AppDimens.Radius.sm),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest
                ) {
                    Text(
                        text = Formatter.formatFileSize(LocalContext.current, app.getSize()),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

/**
 * 列表项 - 网格模式，MD3 风格
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun GridAppItem(
    app: AppItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .padding(AppDimens.Space.sm)
            .clip(RoundedCornerShape(AppDimens.Radius.xl))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(AppDimens.Radius.xl),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = AppDimens.Elevation.none)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(app.getIcon())
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(AppDimens.Radius.md)),
                        contentScale = ContentScale.Crop
                    )

                    androidx.compose.animation.AnimatedVisibility(
                        visible = isSelected,
                        enter = scaleIn(spring(stiffness = Spring.StiffnessMedium)),
                        exit = scaleOut(spring(stiffness = Spring.StiffnessMedium))
                    ) {
                        Surface(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(AppDimens.Radius.md)),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = app.getAppName(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (app.isRedMarked()) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * 高亮文本
 */
@Composable
internal fun highlightText(text: String, keyword: String?): androidx.compose.ui.text.AnnotatedString {
    if (keyword.isNullOrEmpty()) {
        return androidx.compose.ui.text.AnnotatedString(text)
    }

    return buildAnnotatedString {
        var startIndex = 0
        var index = text.lowercase(Locale.getDefault()).indexOf(keyword.lowercase(Locale.getDefault()))

        while (index != -1) {
            append(text.substring(startIndex, index))
            withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)) {
                append(text.substring(index, index + keyword.length))
            }
            startIndex = index + keyword.length
            index = text.lowercase(Locale.getDefault()).indexOf(keyword.lowercase(Locale.getDefault()), startIndex)
        }
        append(text.substring(startIndex))
    }
}
