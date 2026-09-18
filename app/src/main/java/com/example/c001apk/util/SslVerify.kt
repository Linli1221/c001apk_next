package com.example.c001apk.util

import okhttp3.CertificatePinner
import okhttp3.OkHttpClient
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

/**
 * SSL 证书校验（「设置 - 高级 - 校验 SSL 证书」开关，默认开）。
 *
 * 开启时**不信任系统证书库**：TLS 层放行整条证书链，真正的身份校验交给
 * [CertificatePinner]——只有证书链里出现下面固定的公钥（SPKI SHA-256）才放行，
 * 用户自己装的根证书（抓包工具）即使被系统信任也过不了。
 *
 * 关闭时什么都不做，回落成系统默认的 CA + 主机名校验。
 *
 * 固定的是证书链里的 **CA 公钥**（RapidSSL 中间 CA / DigiCert 根），
 * 不是叶子证书公钥，所以酷安换服务器证书（同一 CA 签发）不会把自己锁死。
 */
object SslVerify {

    private const val API_HOST = "api.coolapk.com"
    private const val API2_HOST = "api2.coolapk.com"

    /** 证书链里允许出现的公钥（SHA-256 SPKI，Base64），命中任意一个即通过 */
    private val PINS = listOf(
        // RapidSSL TLS RSA CA G1（中间 CA，2027-11 到期）
        "sha256/E3tYcwo9CiqATmKtpMLW5V+pzIq+ZoDmpXSiJlXGmTo=",
        // DigiCert Global Root G2（交叉签名的根，2031 到期）
        "sha256/i7WTqTvh0OioIruIfFR4kMPnBqrS2rdiVPl/s2uC/CY=",
        // DigiCert Global Root CA（老根，2031 到期）
        "sha256/r/mIkG3eEpVdm+u/ko/cwxzOMo1bk4TyHIlByibiA5E=",
    )

    private val pinner: CertificatePinner by lazy {
        CertificatePinner.Builder().apply {
            listOf(API_HOST, API2_HOST).forEach { host ->
                PINS.forEach { add(host, it) }
            }
        }.build()
    }

    private class AcceptAllTrustManager : X509TrustManager {
        override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit

        override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit

        override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
    }

    /**
     * 给 OkHttpClient.Builder 装上自定义校验；开关关闭时原样返回（用系统校验）。
     *
     * 注：这些 client 只访问 [API_HOST] / [API2_HOST] 两个域名，所以放行整链是安全的；
     * 主机名校验仍然由 OkHttp 默认的 OkHostnameVerifier 完成。
     */
    fun apply(builder: OkHttpClient.Builder): OkHttpClient.Builder {
        if (!PrefManager.isVerifySsl) return builder
        val trustManager: X509TrustManager = AcceptAllTrustManager()
        val socketFactory = SSLContext.getInstance("TLS")
            .apply { init(null, arrayOf<TrustManager>(trustManager), SecureRandom()) }
            .socketFactory
        return builder
            .sslSocketFactory(socketFactory, trustManager)
            .certificatePinner(pinner)
    }
}
