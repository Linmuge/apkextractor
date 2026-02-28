package info.muge.appshare.utils

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build
import android.util.Log
import java.io.BufferedInputStream
import java.io.File
import java.io.RandomAccessFile
import java.security.MessageDigest
import java.security.cert.X509Certificate
import java.util.Date
import java.util.jar.JarFile

/**
 * APK 签名信息数据类
 */
data class ApkSignatureInfo(
    // 签名方案版本
    val signatureSchemes: Set<SignatureScheme> = emptySet(),
    // 证书信息
    val subject: String = "",
    val issuer: String = "",
    val serialNumber: String = "",
    val notBefore: Date? = null,
    val notAfter: Date? = null,
    // 指纹
    val md5: String = "",
    val sha1: String = "",
    val sha256: String = "",
    // 是否有效
    val isValid: Boolean = true,
    val errorMessage: String = ""
)

/**
 * 签名方案枚举
 */
enum class SignatureScheme(val version: Int, val displayName: String) {
    V1(1, "JAR Signing (v1)"),
    V2(2, "APK Signature Scheme v2"),
    V3(3, "APK Signature Scheme v3"),
    V31(31, "APK Signature Scheme v3.1"),
    V4(4, "APK Signature Scheme v4");

    companion object {
        fun fromVersion(version: Int): SignatureScheme? {
            return entries.find { it.version == version }
        }
    }
}

/**
 * APK 签名工具类
 * 提供签名方案检测、证书信息提取等功能
 */
object ApkSignatureUtil {

    private const val TAG = "ApkSignatureUtil"

    // APK Signing Block 魔数
    private const val APK_SIGNING_BLOCK_MAGIC = "APK Sig Block 42"
    private const val APK_SIGNING_BLOCK_MAGIC_SIZE = 16

    // 签名方案 ID (使用 Long 类型以支持超出 Int 范围的值)
    private const val SIGNATURE_SCHEME_V2_ID: Long = 0x7109871a
    private const val SIGNATURE_SCHEME_V3_ID: Long = 0xf05368c0L
    private const val SIGNATURE_SCHEME_V31_ID: Long = 0x1b93ad61

    /**
     * 获取完整的 APK 签名信息
     */
    fun getFullSignatureInfo(apkPath: String, packageInfo: PackageInfo? = null): ApkSignatureInfo {
        return try {
            val schemes = detectSignatureSchemes(apkPath)
            val certInfo = getCertificateInfo(apkPath)
            val fingerprints = packageInfo?.let { getSignatureFingerprints(it) }
                ?: getSignatureFingerprintsFromApk(apkPath)

            ApkSignatureInfo(
                signatureSchemes = schemes,
                subject = certInfo.first,
                issuer = certInfo.second,
                serialNumber = certInfo.third,
                notBefore = certInfo.fourth,
                notAfter = certInfo.fifth,
                md5 = fingerprints.first,
                sha1 = fingerprints.second,
                sha256 = fingerprints.third,
                isValid = certInfo.first.isNotEmpty()
            )
        } catch (e: Exception) {
            LogUtil.logException("Failed to get signature info", e, TAG)
            ApkSignatureInfo(
                isValid = false,
                errorMessage = e.message ?: "Unknown error"
            )
        }
    }

    /**
     * 检测 APK 使用的签名方案
     */
    fun detectSignatureSchemes(apkPath: String): Set<SignatureScheme> {
        val schemes = mutableSetOf<SignatureScheme>()

        try {
            val file = File(apkPath)
            if (!file.exists()) return schemes

            // v1 签名检测：检查 META-INF/*.SF/DSA/RSA 文件
            if (hasV1Signature(apkPath)) {
                schemes.add(SignatureScheme.V1)
            }

            // v2/v3 签名检测：检查 APK Signing Block
            val signingBlockSchemes = detectSigningBlockSchemes(file)
            schemes.addAll(signingBlockSchemes)

        } catch (e: Exception) {
            LogUtil.e("Error detecting signature schemes", e, TAG)
        }

        return schemes
    }

    /**
     * 检测是否有 v1 签名
     */
    private fun hasV1Signature(apkPath: String): Boolean {
        return try {
            JarFile(apkPath).use { jarFile ->
                jarFile.entries().asSequence().any { entry ->
                    val name = entry.name.uppercase()
                    name.startsWith("META-INF/") &&
                            (name.endsWith(".SF") || name.endsWith(".DSA") || name.endsWith(".RSA") || name.endsWith(".EC"))
                }
            }
        } catch (e: Exception) {
            LogUtil.e("Error checking v1 signature", e, TAG)
            false
        }
    }

    /**
     * 检测 APK Signing Block 中的签名方案
     */
    private fun detectSigningBlockSchemes(file: File): Set<SignatureScheme> {
        val schemes = mutableSetOf<SignatureScheme>()

        try {
            RandomAccessFile(file, "r").use { raf ->
                // 查找 APK Signing Block
                val signingBlockInfo = findApkSigningBlock(raf) ?: return schemes
                val (blockOffset, blockSize) = signingBlockInfo

                // 读取 Signing Block 中的键值对
                raf.seek(blockOffset)
                val blockData = ByteArray(blockSize)
                raf.readFully(blockData)

                // 解析键值对
                var offset = 8 // 跳过 size 和 magic 前8字节
                while (offset < blockData.size - APK_SIGNING_BLOCK_MAGIC_SIZE) {
                    val pairSize = readIntLE(blockData, offset).toInt()
                    if (pairSize <= 0 || offset + 4 + pairSize > blockData.size) break

                    val pairId = readIntLE(blockData, offset + 4)

                    when (pairId) {
                        SIGNATURE_SCHEME_V2_ID -> schemes.add(SignatureScheme.V2)
                        SIGNATURE_SCHEME_V3_ID -> schemes.add(SignatureScheme.V3)
                        SIGNATURE_SCHEME_V31_ID -> schemes.add(SignatureScheme.V31)
                    }

                    offset += 4 + pairSize
                }
            }
        } catch (e: Exception) {
            LogUtil.e("Error detecting signing block schemes", e, TAG)
        }

        return schemes
    }

    /**
     * 查找 APK Signing Block 的位置
     */
    private fun findApkSigningBlock(raf: RandomAccessFile): Pair<Long, Int>? {
        try {
            // 尝试查找 EOCD（End of Central Directory）
            // EOCD 的大小至少是 22 字节。由于 ZIP 注释最长为 65535 字节，因此 EOCD 的起始位置
            // 最多距离文件末尾 65557 字节。
            val maxCommentSize = 65535
            val minEocdSize = 22
            val maxEocdDistance = maxCommentSize + minEocdSize
            
            val fileSize = raf.length()
            val searchSize = if (fileSize < maxEocdDistance) fileSize.toInt() else maxEocdDistance
            
            val buffer = ByteArray(searchSize)
            raf.seek(fileSize - searchSize)
            raf.readFully(buffer)
            
            var eocdOffsetInFile = -1L
            // 从后往前在内存中搜索 EOCD 魔数 (0x06054b50 的小端序列: 50 4b 05 06)
            for (i in (searchSize - minEocdSize) downTo 0) {
                if (buffer[i] == 0x50.toByte() && buffer[i+1] == 0x4b.toByte() &&
                    buffer[i+2] == 0x05.toByte() && buffer[i+3] == 0x06.toByte()) {
                    eocdOffsetInFile = fileSize - searchSize + i
                    break
                }
            }
            
            if (eocdOffsetInFile < 0) return null
            raf.seek(eocdOffsetInFile)

            // 读取 EOCD
            raf.skipBytes(12) // 跳过不需要的字段
            val cdOffset = raf.readInt().toLong() and 0xFFFFFFFFL

            // 检查是否有 APK Signing Block
            if (cdOffset < 32) return null

            // 读取 Signing Block 大小
            raf.seek(cdOffset - 24)
            val blockSizeLow = raf.readInt().toLong() and 0xFFFFFFFFL
            val blockSizeHigh = raf.readInt().toLong() and 0xFFFFFFFFL
            val blockSize = blockSizeLow or (blockSizeHigh shl 32)

            if (blockSize <= 0 || blockSize > cdOffset - 8) return null

            // 验证魔数
            raf.seek(cdOffset - 16)
            val magic = ByteArray(16)
            raf.readFully(magic)
            if (!magic.contentEquals(APK_SIGNING_BLOCK_MAGIC.toByteArray())) return null

            val blockOffset = cdOffset - blockSize - 8
            return Pair(blockOffset, blockSize.toInt() + 8)

        } catch (e: Exception) {
            LogUtil.e("Error finding APK signing block", e, TAG)
            return null
        }
    }

    /**
     * 从 APK 文件获取证书信息
     */
    private fun getCertificateInfo(apkPath: String): Quintuple<String, String, String, Date?, Date?> {
        var subject = ""
        var issuer = ""
        var serial = ""
        var notBefore: Date? = null
        var notAfter: Date? = null

        try {
            JarFile(apkPath).use { jarFile ->
                val jarEntry = jarFile.getJarEntry("AndroidManifest.xml")
                if (jarEntry != null) {
                    val readBuffer = ByteArray(8192)
                    BufferedInputStream(jarFile.getInputStream(jarEntry)).use { input ->
                        while (input.read(readBuffer) != -1) { }
                    }
                    val certs = jarEntry.certificates
                    if (!certs.isNullOrEmpty()) {
                        val x509cert = certs[0] as X509Certificate
                        subject = x509cert.subjectDN.toString()
                        issuer = x509cert.issuerDN.toString()
                        serial = x509cert.serialNumber.toString()
                        notBefore = x509cert.notBefore
                        notAfter = x509cert.notAfter
                    }
                }
            }
        } catch (e: Exception) {
            LogUtil.e("Error getting certificate info", e, TAG)
        }

        return Quintuple(subject, issuer, serial, notBefore, notAfter)
    }

    /**
     * 从 PackageInfo 获取签名指纹
     */
    private fun getSignatureFingerprints(packageInfo: PackageInfo): Triple<String, String, String> {
        return try {
            val signatures: Array<out Signature>? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                // Android 9+ 使用新的签名 API
                val signingInfo = packageInfo.signingInfo
                signingInfo?.apkContentsSigners
            } else {
                @Suppress("DEPRECATION")
                packageInfo.signatures
            }

            if (!signatures.isNullOrEmpty()) {
                val signatureBytes = signatures[0].toByteArray()
                Triple(
                    getHexString(MessageDigest.getInstance("MD5").digest(signatureBytes)),
                    getHexString(MessageDigest.getInstance("SHA1").digest(signatureBytes)),
                    getHexString(MessageDigest.getInstance("SHA256").digest(signatureBytes))
                )
            } else {
                Triple("", "", "")
            }
        } catch (e: Exception) {
            LogUtil.e("Error getting signature fingerprints", e, TAG)
            Triple("", "", "")
        }
    }

    /**
     * 从 APK 文件直接获取签名指纹（作为备用方法）
     */
    private fun getSignatureFingerprintsFromApk(apkPath: String): Triple<String, String, String> {
        return try {
            JarFile(apkPath).use { jarFile ->
                val jarEntry = jarFile.getJarEntry("AndroidManifest.xml")
                if (jarEntry != null) {
                    val readBuffer = ByteArray(8192)
                    BufferedInputStream(jarFile.getInputStream(jarEntry)).use { input ->
                        while (input.read(readBuffer) != -1) { }
                    }
                    val certs = jarEntry.certificates
                    if (!certs.isNullOrEmpty()) {
                        val certBytes = certs[0].encoded
                        Triple(
                            getHexString(MessageDigest.getInstance("MD5").digest(certBytes)),
                            getHexString(MessageDigest.getInstance("SHA1").digest(certBytes)),
                            getHexString(MessageDigest.getInstance("SHA256").digest(certBytes))
                        )
                    } else {
                        Triple("", "", "")
                    }
                } else {
                    Triple("", "", "")
                }
            }
        } catch (e: Exception) {
            LogUtil.e("Error getting fingerprints from APK", e, TAG)
            Triple("", "", "")
        }
    }

    /**
     * 将字节数组转换为十六进制字符串
     */
    private fun getHexString(bytes: ByteArray): String {
        return bytes.joinToString(":") { byte ->
            "%02X".format(byte)
        }
    }

    /**
     * 小端序读取 int (返回 Long 以支持无符号比较)
     */
    private fun readIntLE(data: ByteArray, offset: Int): Long {
        return (data[offset].toLong() and 0xFF) or
                ((data[offset + 1].toLong() and 0xFF) shl 8) or
                ((data[offset + 2].toLong() and 0xFF) shl 16) or
                ((data[offset + 3].toLong() and 0xFF) shl 24)
    }
}

/**
 * 五元组辅助类
 */
private data class Quintuple<A, B, C, D, E>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E
)
