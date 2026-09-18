package com.example.c001apk.util

import android.util.Log
import com.example.c001apk.MyApplication
import okhttp3.OkHttpClient
import java.security.KeyStore
import java.security.SecureRandom
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

/**
 * SSL 证书校验（「设置 - 高级 - 校验 SSL 证书」开关，默认开）。
 *
 * 开启时的校验策略（两道关，都要过）：
 *
 * 1. **平台校验**：走系统证书库做标准 PKIX 校验（签名链、有效期在平台侧校验，
 *    域名由 OkHttp 的 hostnameVerifier 校验）。这一步保证与系统一致，
 *    能正常处理 SHA-1 遗留根等历史包袱（如 api.coolapk.com 的
 *    DigiCert Global Root CA —— Conscrypt 只允许**系统库**里的锚点使用 SHA-1，
 *    所以这一步不能省，否则服务器塞的老根会让整条链被拒）。
 *
 * 2. **锚点白名单**：证书链的信任锚必须落在应用内置的 Mozilla CA 全集里
 *    （res/raw/cacert_000.pem ~ cacert_120.pem，121 张，来自 https://curl.se/ca/cacert.pem）。
 *    用户自己装的抓包证书（Charles/Fiddler/mitmproxy）、厂商预置的私有根、
 *    被劫持后替换的 CA，都不在 Mozilla 库里 → 一律拒绝。
 *
 * 另外：平台校验失败时（例如旧系统缺新根证书），回退到「只用内置 CA 做完整 PKIX 校验」，
 * 内置库齐全，所以老系统也不会掉链子。
 *
 * 关闭开关则回落到系统默认校验（配合 res/xml/network_security_config.xml，
 * 仍然不信任用户安装的 CA）。
 *
 * 注意：开关修改后需**重启应用**才生效（OkHttpClient 是单例，构建时固化了 TrustManager）。
 */
object SslVerify {

    private const val TAG = "SslVerify"

    /** 内置 Mozilla CA 根证书列表（懒加载，首次网络请求时构建一次） */
    private val bundledAnchors: List<X509Certificate> by lazy {
        val certFactory = CertificateFactory.getInstance("X.509")
        val context = MyApplication.context
        val resources = context.resources
        val anchors = mutableListOf<X509Certificate>()
        var index = 0
        while (true) {
            // 资源名固定三位数：cacert_000 ~ cacert_120
            val resId = resources.getIdentifier(
                "cacert_%03d".format(index), "raw", context.packageName
            )
            if (resId == 0) break
            resources.openRawResource(resId).use { input ->
                certFactory.generateCertificates(input).forEach { cert ->
                    (cert as? X509Certificate)?.let { anchors.add(it) }
                }
            }
            index++
        }
        check(anchors.isNotEmpty()) { "内置 CA 证书库为空，请检查 res/raw/cacert_*.pem" }
        Log.i(TAG, "内置 Mozilla CA 库加载完成：${anchors.size} 张根证书")
        anchors
    }

    /** 只用内置 CA 做 PKIX 校验的 TrustManager */
    private val bundledTrustManager: X509TrustManager by lazy {
        val keyStore = KeyStore.getInstance(KeyStore.getDefaultType()).apply { load(null) }
        bundledAnchors.forEachIndexed { i, cert ->
            keyStore.setCertificateEntry("bundled_ca_$i", cert)
        }
        val tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        tmf.init(keyStore)
        tmf.trustManagers.filterIsInstance<X509TrustManager>().first()
    }

    /** 系统默认 TrustManager（系统证书库） */
    private val platformTrustManager: X509TrustManager by lazy {
        val tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        tmf.init(null as KeyStore?)
        tmf.trustManagers.filterIsInstance<X509TrustManager>().first()
    }

    /**
     * 证书链是否锚定在内置 Mozilla CA 库里。
     * - 服务器带了根证书：链中某张证书就是内置根；
     * - 服务器省略了根：链顶证书由内置根签发。
     */
    private fun anchoredInBundle(chain: Array<X509Certificate>): Boolean {
        for (cert in chain) {
            if (bundledAnchors.contains(cert)) return true
        }
        val top = chain.lastOrNull() ?: return false
        return bundledAnchors.any { anchor ->
            anchor.subjectX500Principal == top.issuerX500Principal &&
                    runCatching { top.verify(anchor.publicKey) }.isSuccess
        }
    }

    private val verifyingTrustManager: X509TrustManager by lazy {
        object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {
                platformTrustManager.checkClientTrusted(chain, authType)
            }

            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {
                // 第 1 关：平台校验（能兼容 SHA-1 遗留根等历史包袱）
                val platformOk = runCatching {
                    platformTrustManager.checkServerTrusted(chain, authType)
                }.isSuccess

                if (platformOk) {
                    // 第 2 关：锚点必须在内置 Mozilla CA 库里
                    if (!anchoredInBundle(chain)) {
                        throw CertificateException(
                            "证书链的信任锚不在内置 Mozilla CA 库中，疑似中间人证书"
                        )
                    }
                    return
                }

                // 平台校验收不了（旧系统缺新根证书等）：回退到只用内置 CA 的完整 PKIX 校验，
                // 校验不过会抛 CertificateException
                Log.w(TAG, "平台校验未通过，改用内置 Mozilla CA 库校验")
                bundledTrustManager.checkServerTrusted(chain, authType)
            }

            override fun getAcceptedIssuers(): Array<X509Certificate> =
                bundledTrustManager.acceptedIssuers
        }
    }

    /** 应用到 OkHttp 客户端；开关关闭时原样返回（走系统默认校验） */
    fun apply(builder: OkHttpClient.Builder): OkHttpClient.Builder {
        if (!PrefManager.isVerifySsl) return builder

        val sslContext = SSLContext.getInstance("TLS").apply {
            init(null, arrayOf<TrustManager>(verifyingTrustManager), SecureRandom())
        }
        return builder.sslSocketFactory(sslContext.socketFactory, verifyingTrustManager)
    }
}
