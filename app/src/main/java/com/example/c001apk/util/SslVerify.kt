package com.example.c001apk.util

import android.util.Base64
import okhttp3.OkHttpClient
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

/**
 * SSL 证书校验（「设置 - 高级 - 校验 SSL 证书」开关，默认开）。
 *
 * 开启时**不信任系统证书库**：在 [X509TrustManager.checkServerTrusted] 里自己算
 * 证书链上每个证书的公钥哈希（SPKI SHA-256），命中下面任意一个固定值才放行，
 * 用户自己装的根证书（抓包工具）即使被系统信任也过不了。
 * 主机名校验仍由 OkHttp 默认的 OkHostnameVerifier 完成。
 *
 * 之所以不用 OkHttp 的 CertificatePinner：它和自定义 sslSocketFactory 组合时，
 * 在部分设备（Conscrypt/TLS1.3）上从 session 取 peerCertificates 会拿到空链，
 * 导致所有请求报 "Certificate pinning failure!" 且日志里证书链为空（560718b 翻车原因）。
 *
 * 关闭时什么都不做，回落成系统默认的 CA + 主机名校验。
 *
 * 固定的是证书链里的 **CA 公钥**（RapidSSL 中间 CA / DigiCert 根），
 * 不是叶子证书公钥，所以酷安换服务器证书（同一 CA 签发）不会把自己锁死。
 */
object SslVerify {

    /** 证书链里允许出现的公钥（SHA-256 SPKI，Base64），命中任意一个即通过 */
    private val PINS = setOf(
        // RapidSSL TLS RSA CA G1（中间 CA，2027-11 到期）
        "sha256/E3tYcwo9CiqATmKtpMLW5V+pzIq+ZoDmpXSiJlXGmTo=",
        // DigiCert Global Root G2（交叉签名的根，2031 到期）
        "sha256/i7WTqTvh0OioIruIfFR4kMPnBqrS2rdiVPl/s2uC/CY=",
        // DigiCert Global Root CA（老根，2031 到期）
        "sha256/r/mIkG3eEpVdm+u/ko/cwxzOMo1bk4TyHIlByibiA5E=",
    )

    private fun pinOf(cert: X509Certificate): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(cert.publicKey.encoded)
        return "sha256/" + Base64.encodeToString(digest, Base64.NO_WRAP)
    }

    private class PinnedTrustManager : X509TrustManager {

        override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit

        override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()

        override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
            if (!chain.isNullOrEmpty() && chain.any { pinOf(it) in PINS }) return
            val received = chain.orEmpty().joinToString("\n") { "    ${pinOf(it)}  ${it.subjectX500Principal.name}" }
            throw CertificateException(
                "SSL pinning failure! chain size=${chain?.size ?: 0}\n" +
                    "  Received certificates:\n${if (received.isEmpty()) "    (none)" else received}\n" +
                    "  Pinned certificates:\n" + PINS.joinToString("\n") { "    $it" }
            )
        }
    }

    /** 给 OkHttpClient.Builder 装上自定义校验；开关关闭时原样返回（用系统校验）。 */
    fun apply(builder: OkHttpClient.Builder): OkHttpClient.Builder {
        if (!PrefManager.isVerifySsl) return builder
        val trustManager: X509TrustManager = PinnedTrustManager()
        val socketFactory = SSLContext.getInstance("TLS")
            .apply { init(null, arrayOf<TrustManager>(trustManager), SecureRandom()) }
            .socketFactory
        return builder.sslSocketFactory(socketFactory, trustManager)
    }
}
