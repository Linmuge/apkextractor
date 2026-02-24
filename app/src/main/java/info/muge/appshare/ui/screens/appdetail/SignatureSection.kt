package info.muge.appshare.ui.screens.appdetail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import info.muge.appshare.R
import info.muge.appshare.items.AppItem
import info.muge.appshare.ui.theme.AppDimens
import info.muge.appshare.utils.EnvironmentUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 签名数据类
 */
data class SignatureInfo(
    val subject: String = "",
    val issuer: String = "",
    val serial: String = "",
    val notBefore: String = "",
    val notAfter: String = "",
    val md5: String = "",
    val sha1: String = "",
    val sha256: String = ""
)

/**
 * 签名内容 - 与原 SignatureFragment 完全一致
 */
@Composable
fun SignatureContent(appItem: AppItem) {
    val context = LocalContext.current
    var signatureInfo by remember { mutableStateOf<SignatureInfo?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(appItem) {
        try {
            val result = withContext(Dispatchers.IO) {
                val packageInfo = appItem.getPackageInfo()
                val sourceDir = packageInfo.applicationInfo?.sourceDir ?: ""
                val signInfos = EnvironmentUtil.getAPKSignInfo(sourceDir)
                val md5 = EnvironmentUtil.getSignatureMD5StringOfPackageInfo(packageInfo)
                val sha1 = EnvironmentUtil.getSignatureSHA1OfPackageInfo(packageInfo)
                val sha256 = EnvironmentUtil.getSignatureSHA256OfPackageInfo(packageInfo)
                SignatureInfo(
                    subject = signInfos.getOrElse(0) { "" },
                    issuer = signInfos.getOrElse(1) { "" },
                    serial = signInfos.getOrElse(2) { "" },
                    notBefore = signInfos.getOrElse(3) { "" },
                    notAfter = signInfos.getOrElse(4) { "" },
                    md5 = md5,
                    sha1 = sha1,
                    sha256 = sha256
                )
            }
            signatureInfo = result
        } catch (_: Exception) { }
        isLoading = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(AppDimens.Space.lg)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 48.dp)
            )
        } else {
            val info = signatureInfo ?: return
            DetailCard {
                SignatureItem(
                    label = stringResource(R.string.activity_detail_signature_issuer),
                    value = info.subject,
                    onClick = { copyToClipboard(context, info.subject) }
                )
                SignatureItem(
                    label = stringResource(R.string.activity_detail_signature_subject),
                    value = info.issuer,
                    onClick = { copyToClipboard(context, info.issuer) }
                )
                SignatureItem(
                    label = stringResource(R.string.activity_detail_signature_serial),
                    value = info.serial,
                    onClick = { copyToClipboard(context, info.serial) }
                )
                SignatureItem(
                    label = stringResource(R.string.activity_detail_signature_start),
                    value = info.notBefore,
                    onClick = { copyToClipboard(context, info.notBefore) }
                )
                SignatureItem(
                    label = stringResource(R.string.activity_detail_signature_end),
                    value = info.notAfter,
                    onClick = { copyToClipboard(context, info.notAfter) }
                )
                SignatureItem(
                    label = stringResource(R.string.activity_detail_signature_md5),
                    value = info.md5,
                    onClick = { copyToClipboard(context, info.md5) }
                )
                SignatureItem(
                    label = stringResource(R.string.activity_detail_signature_sha1),
                    value = info.sha1,
                    onClick = { copyToClipboard(context, info.sha1) }
                )
                SignatureItem(
                    label = stringResource(R.string.activity_detail_signature_sha256),
                    value = info.sha256,
                    onClick = { copyToClipboard(context, info.sha256) }
                )
            }
        }
    }
}

@Composable
private fun SignatureItem(
    label: String,
    value: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 10.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
