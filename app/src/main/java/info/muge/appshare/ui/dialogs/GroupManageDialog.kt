package info.muge.appshare.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import info.muge.appshare.R
import info.muge.appshare.data.AppGroup
import info.muge.appshare.data.GroupColors
import info.muge.appshare.ui.theme.AppDimens

/**
 * 分组管理对话框
 */
@Composable
fun GroupManageDialog(
    packageName: String? = null,
    appName: String = "",
    onDismiss: () -> Unit,
    onGroupCreated: (AppGroup) -> Unit = {},
    onGroupUpdated: (AppGroup) -> Unit = {},
    onGroupDeleted: (String) -> Unit = {}
) {
    val context = LocalContext.current
    var groups by remember { mutableStateOf(info.muge.appshare.data.AppGroupRepository.getAllGroups(context)) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var editingGroup by remember { mutableStateOf<AppGroup?>(null) }

    // 刷新分组列表
    fun refreshGroups() {
        groups = info.muge.appshare.data.AppGroupRepository.getAllGroups(context)
    }

    if (showCreateDialog) {
        CreateGroupDialog(
            onDismiss = { showCreateDialog = false },
            onCreated = { group ->
                info.muge.appshare.data.AppGroupRepository.addGroup(context, group)
                refreshGroups()
                onGroupCreated(group)
            }
        )
    }

    editingGroup?.let { group ->
        EditGroupDialog(
            group = group,
            onDismiss = { editingGroup = null },
            onUpdated = { updatedGroup ->
                info.muge.appshare.data.AppGroupRepository.updateGroup(context, updatedGroup)
                refreshGroups()
                onGroupUpdated(updatedGroup)
            },
            onDelete = {
                info.muge.appshare.data.AppGroupRepository.deleteGroup(context, group.id)
                refreshGroups()
                onGroupDeleted(group.id)
                editingGroup = null
            }
        )
    }

    AppBottomSheet(
        title = if (packageName != null) {
            stringResource(R.string.group_manage_app_title, appName)
        } else {
            stringResource(R.string.group_manage_title)
        },
        onDismiss = onDismiss,
        content = {
            if (groups.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = AppDimens.Space.xl),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(AppDimens.Space.md))
                    Text(
                        text = stringResource(R.string.group_empty_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(AppDimens.Space.sm)
                ) {
                    items(groups) { group ->
                        GroupItem(
                            group = group,
                            isSelected = packageName != null && group.contains(packageName),
                            showEditButton = packageName == null,
                            onClick = {
                                if (packageName != null) {
                                    // 切换应用在分组中的状态
                                    val updatedGroup = if (group.contains(packageName)) {
                                        group.removePackage(packageName)
                                    } else {
                                        group.addPackage(packageName)
                                    }
                                    info.muge.appshare.data.AppGroupRepository.updateGroup(context, updatedGroup)
                                    refreshGroups()
                                    onGroupUpdated(updatedGroup)
                                } else {
                                    editingGroup = group
                                }
                            },
                            onEditClick = {
                                editingGroup = group
                            }
                        )
                    }
                }
            }
        },
        actions = {
            AppBottomSheetDualActions(
                onConfirm = { showCreateDialog = true },
                onDismiss = onDismiss,
                confirmText = stringResource(R.string.group_create),
                dismissText = stringResource(R.string.action_cancel)
            )
        }
    )
}

/**
 * 分组列表项
 */
@Composable
private fun GroupItem(
    group: AppGroup,
    isSelected: Boolean,
    showEditButton: Boolean,
    onClick: () -> Unit,
    onEditClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AppDimens.Radius.md))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceContainerHigh
            )
            .clickable(onClick = onClick)
            .padding(AppDimens.Space.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 颜色标记
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(Color(group.color))
        )

        Spacer(modifier = Modifier.width(AppDimens.Space.md))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = group.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = stringResource(R.string.group_app_count, group.size),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        }

        if (showEditButton) {
            Spacer(modifier = Modifier.width(AppDimens.Space.sm))
            IconButton(
                onClick = onEditClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = stringResource(R.string.group_edit),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * 创建分组对话框
 */
@Composable
private fun CreateGroupDialog(
    onDismiss: () -> Unit,
    onCreated: (AppGroup) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedColorIndex by remember { mutableIntStateOf(0) }

    AppBottomSheet(
        title = stringResource(R.string.group_create_title),
        onDismiss = onDismiss,
        content = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.group_name_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(AppDimens.Space.lg))

                Text(
                    text = stringResource(R.string.group_color_label),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(AppDimens.Space.sm))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppDimens.Space.sm)
                ) {
                    GroupColors.colors.forEachIndexed { index, color ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(color))
                                .clickable { selectedColorIndex = index }
                        ) {
                            if (index == selectedColorIndex) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        actions = {
            AppBottomSheetDualActions(
                onConfirm = {
                    if (name.isNotBlank()) {
                        val group = AppGroup(
                            id = AppGroup.generateId(),
                            name = name.trim(),
                            color = GroupColors.colors[selectedColorIndex]
                        )
                        onCreated(group)
                    }
                },
                onDismiss = onDismiss,
                confirmText = stringResource(R.string.action_confirm),
                dismissText = stringResource(R.string.action_cancel)
            )
        }
    )
}

/**
 * 编辑分组对话框
 */
@Composable
private fun EditGroupDialog(
    group: AppGroup,
    onDismiss: () -> Unit,
    onUpdated: (AppGroup) -> Unit,
    onDelete: () -> Unit
) {
    var name by remember { mutableStateOf(group.name) }
    var selectedColor by remember { mutableLongStateOf(group.color) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AppBottomSheet(
            title = stringResource(R.string.group_delete_title),
            onDismiss = { showDeleteConfirm = false },
            content = {
                Text(
                    text = stringResource(R.string.group_delete_message, group.name),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            actions = {
                AppBottomSheetDualActions(
                    onConfirm = {
                        showDeleteConfirm = false
                        onDelete()
                    },
                    onDismiss = { showDeleteConfirm = false },
                    confirmText = stringResource(R.string.action_confirm),
                    dismissText = stringResource(R.string.action_cancel)
                )
            }
        )
    }

    AppBottomSheet(
        title = stringResource(R.string.group_edit_title),
        onDismiss = onDismiss,
        content = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.group_name_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(AppDimens.Space.lg))

                Text(
                    text = stringResource(R.string.group_color_label),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(AppDimens.Space.sm))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppDimens.Space.sm)
                ) {
                    GroupColors.colors.forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(color))
                                .clickable { selectedColor = color }
                        ) {
                            if (color == selectedColor) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .size(20.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(AppDimens.Space.xl))

                // 删除按钮
                TextButton(
                    onClick = { showDeleteConfirm = true },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(AppDimens.Space.xs))
                    Text(stringResource(R.string.group_delete))
                }
            }
        },
        actions = {
            AppBottomSheetDualActions(
                onConfirm = {
                    if (name.isNotBlank()) {
                        val updatedGroup = group.copy(
                            name = name.trim(),
                            color = selectedColor,
                            updatedAt = System.currentTimeMillis()
                        )
                        onUpdated(updatedGroup)
                    }
                },
                onDismiss = onDismiss,
                confirmText = stringResource(R.string.action_confirm),
                dismissText = stringResource(R.string.action_cancel)
            )
        }
    )
}

