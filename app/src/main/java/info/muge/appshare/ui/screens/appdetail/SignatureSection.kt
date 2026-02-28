package info.muge.appshare.ui.screens.appdetail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import info.muge.appshare.utils.ApkSignatureInfo
import info.muge.appshare.utils.ApkSignatureUtil
import info.muge.appshare.utils.EnvironmentUtil
import info.muge.appshare.utils.SignatureScheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 签名数据类（保留原有兼容性）
 */
data class SignatureInfo(
    val subject: String = "",
    val issuer: String = "",
    val serial: String = "",
    val notBefore: String = "",
    val notAfter: String = "",
    val md5: String = "",
    val sha1: String = "",
    val sha256: String = "",
    val signatureSchemes: Set<SignatureScheme> = emptySet()
)

/**
 * 签名内容 - 增强版，显示签名方案版本
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

                // 使用增强的签名工具获取完整信息
                val fullInfo = ApkSignatureUtil.getFullSignatureInfo(sourceDir, packageInfo)

                // 兼容原有格式
                val signInfos = EnvironmentUtil.getAPKSignInfo(sourceDir)

                SignatureInfo(
                    subject = fullInfo.subject.ifEmpty { signInfos.getOrElse(0) { "" } },
                    issuer = fullInfo.issuer.ifEmpty { signInfos.getOrElse(1) { "" } },
                    serial = fullInfo.serialNumber.ifEmpty { signInfos.getOrElse(2) { "" } },
                    notBefore = fullInfo.notBefore?.toString() ?: signInfos.getOrElse(3) { "" },
                    notAfter = fullInfo.notAfter?.toString() ?: signInfos.getOrElse(4) { "" },
                    md5 = fullInfo.md5.ifEmpty { EnvironmentUtil.getSignatureMD5StringOfPackageInfo(packageInfo) },
                    sha1 = fullInfo.sha1.ifEmpty { EnvironmentUtil.getSignatureSHA1OfPackageInfo(packageInfo) },
                    sha256 = fullInfo.sha256.ifEmpty { EnvironmentUtil.getSignatureSHA256OfPackageInfo(packageInfo) },
                    signatureSchemes = fullInfo.signatureSchemes
                )
            }
            signatureInfo = result
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isLoading = false
        }
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

            // 签名方案版本卡片
            if (info.signatureSchemes.isNotEmpty()) {
                DetailCard {
                    Text(
                        text = stringResource(R.string.signature_scheme_title),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                    )
                    SignatureSchemeRow(info.signatureSchemes)
                }
                Spacer(modifier = Modifier.height(AppDimens.Space.md))
            }

            // 证书信息卡片
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

/**
 * 签名方案显示行
 */
@Composable
private fun SignatureSchemeRow(schemes: Set<SignatureScheme>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        schemes.sortedBy { it.version }.forEach { scheme ->
            androidx.compose.material3.FilterChip(
                selected = true,
                onClick = { },
                label = {
                    Text(
                        text = "v${if (scheme.version == 31) "3.1" else scheme.version}",
                        style = MaterialTheme.typography.labelMedium
                    )
                },
                modifier = Modifier.padding(end = 4.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
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
