package info.muge.appshare.ui.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun AppBottomSheet(
    title: String,
    onDismiss: () -> Unit,
    dismissible: Boolean = true,
    content: @Composable () -> Unit,
    actions: (@Composable () -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    ModalBottomSheet(
        onDismissRequest = {
            if (dismissible) {
                onDismiss()
            }
        },
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        tonalElevation = 0.dp,
        dragHandle = {
            Surface(
                modifier = Modifier
                    .padding(top = 10.dp)
                    .height(4.dp)
                    .fillMaxWidth(0.14f),
                shape = RoundedCornerShape(999.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            ) {}
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp)
            ) {
                content()
            }

            actions?.let {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 18.dp, bottom = 10.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    it()
                }
            }
        }
    }
}

@Composable
fun AppBottomSheetActions(
    onConfirm: () -> Unit,
    onDismiss: (() -> Unit)? = null,
    confirmText: String,
    dismissText: String? = null,
    confirmEnabled: Boolean = true
) {
    if (onDismiss != null && dismissText != null) {
        TextButton(onClick = onDismiss) {
            Text(dismissText)
        }
    }
    Button(
        onClick = onConfirm,
        enabled = confirmEnabled
    ) {
        Text(confirmText)
    }
}

@Composable
fun AppBottomSheetDualActions(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    confirmText: String,
    dismissText: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        OutlinedButton(
            onClick = onDismiss,
            modifier = Modifier.weight(1f)
        ) {
            Text(dismissText)
        }
        Button(
            onClick = onConfirm,
            modifier = Modifier.weight(1f)
        ) {
            Text(confirmText)
        }
    }
}
