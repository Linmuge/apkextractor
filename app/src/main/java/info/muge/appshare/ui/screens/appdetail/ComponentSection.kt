package info.muge.appshare.ui.screens.appdetail

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import info.muge.appshare.R
import info.muge.appshare.items.AppItem
import info.muge.appshare.ui.ToastManager
import info.muge.appshare.ui.theme.AppDimens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 组件列表内容
 */
@Composable
fun ComponentListContent(
    context: Context,
    appItem: AppItem,
    componentType: ComponentType
) {
    val components = remember { mutableStateListOf<ComponentItem>() }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(appItem, componentType) {
        isLoading = true
        components.clear()

        val items = withContext(Dispatchers.IO) {
            val packageInfo = appItem.getPackageInfo()
            val result = mutableListOf<ComponentItem>()

            when (componentType) {
                ComponentType.PERMISSION -> {
                    packageInfo.requestedPermissions?.forEach { permission ->
                        if (permission != null) {
                            result.add(ComponentItem(permission, null, false, false, null))
                        }
                    }
                }
                ComponentType.ACTIVITY -> {
                    packageInfo.activities?.forEach { activityInfo ->
                        result.add(ComponentItem(
                            activityInfo.name,
                            activityInfo.packageName,
                            true,
                            activityInfo.exported,
                            activityInfo.permission
                        ))
                    }
                }
                ComponentType.SERVICE -> {
                    packageInfo.services?.forEach { serviceInfo ->
                        result.add(ComponentItem(
                            serviceInfo.name,
                            serviceInfo.packageName,
                            true,
                            serviceInfo.exported,
                            serviceInfo.permission
                        ))
                    }
                }
                ComponentType.RECEIVER -> {
                    packageInfo.receivers?.forEach { receiverInfo ->
                        result.add(ComponentItem(
                            receiverInfo.name,
                            null,
                            false,
                            receiverInfo.exported,
                            receiverInfo.permission
                        ))
                    }
                }
                ComponentType.PROVIDER -> {
                    packageInfo.providers?.forEach { providerInfo ->
                        result.add(ComponentItem(
                            providerInfo.name,
                            null,
                            false,
                            providerInfo.exported,
                            providerInfo.readPermission
                        ))
                    }
                }
                ComponentType.STATIC_LOADER -> {
                    val bundle = appItem.getStaticReceiversBundle()
                    bundle.keySet().forEach { key ->
                        val filters = bundle.getStringArrayList(key)
                        val description = filters?.joinToString(", ") ?: ""
                        result.add(ComponentItem(key, description, false, false, null))
                    }
                }
            }
            result
        }

        components.addAll(items)
        isLoading = false
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center)
            )
        } else if (components.isEmpty()) {
            Text(
                text = "暂无内容",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                items(components) { item ->
                    ComponentItemCard(
                        item = item,
                        componentType = componentType,
                        onClick = { copyToClipboard(context, item.name) },
                        onLongClick = {
                            handleComponentLongClick(context, item, componentType)
                        },
                        canLongClick = item.canLaunch
                    )
                }
            }
        }
    }
}

data class ComponentItem(
    val name: String,
    val packageName: String?,
    val canLaunch: Boolean,
    val exported: Boolean,
    val permission: String?
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ComponentItemCard(
    item: ComponentItem,
    componentType: ComponentType,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    canLongClick: Boolean = false
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .then(
                if (canLongClick) {
                    Modifier.combinedClickable(
                        onClick = onClick,
                        onLongClick = onLongClick
                    )
                } else {
                    Modifier.clickable(onClick = onClick)
                }
            ),
        shape = RoundedCornerShape(AppDimens.Radius.md),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppDimens.Space.lg)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (componentType != ComponentType.PERMISSION && componentType != ComponentType.STATIC_LOADER) {
                    if (item.exported) {
                        Icon(
                            imageVector = Icons.Outlined.OpenInNew,
                            contentDescription = "Exported",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            if (!item.permission.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = item.permission,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

private fun handleComponentLongClick(
    context: Context,
    item: ComponentItem,
    componentType: ComponentType
) {
    if (!item.canLaunch || item.packageName.isNullOrEmpty()) return

    when (componentType) {
        ComponentType.ACTIVITY -> {
            try {
                val intent = Intent()
                intent.setClassName(item.packageName, item.name)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            } catch (e: Exception) {
                ToastManager.showToast(context, "无法启动该Activity", Toast.LENGTH_SHORT)
            }
        }
        ComponentType.SERVICE -> {
            try {
                val intent = Intent()
                intent.setClassName(item.packageName, item.name)
                context.startService(intent)
                ToastManager.showToast(context, "已尝试启动Service", Toast.LENGTH_SHORT)
            } catch (e: Exception) {
                ToastManager.showToast(context, "无法启动该Service", Toast.LENGTH_SHORT)
            }
        }
        else -> {}
    }
}
