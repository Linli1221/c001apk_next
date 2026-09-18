package com.example.c001apk.util

import android.util.Log
import com.example.c001apk.MyApplication
import okhttp3.OkHttpClient
import java.security.KeyStore
import java.security.SecureRandom
import java.security.cert.CertificateFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

/**
 * SSL 证书校验（「设置 - 高级 - 校验 SSL 证书」开关，默认开）。
 *
 * 开启时**不信任设备上的任何证书库**（系统 CA、用户安装的 CA 都不信任），
 * 只认应用内置的 Mozilla CA 全集（res/raw/cacert_000.pem ~ cacert_120.pem，121 张，
 * 来源 https://curl.se/ca/cacert.pem），由 TrustManagerFactory 做完整证书链校验：
 * 签名链、有效期、域名匹配全部覆盖。
 *
 * - 内置 CA 让旧系统缺新根证书时也能正常校验（不依赖系统证书库的新鲜度）；
 * - 抓包工具（Charles/Fiddler/mitmproxy）把 CA 装进系统或用户证书库都无效。
 *
 * 关闭时回落到系统默认校验（仍然不信任用户安装的 CA，见
 * res/xml/network_security_config.xml）。
 *
 * 注意：开关修改后需**重启应用**才生效（OkHttpClient 是单例，构建时固化了 TrustManager）。
 */
object SslVerify {

    private const val TAG = "SslVerify"

    /** 内置信任库对应的 TrustManager，懒加载（首次网络请求时构建一次） */
    internal val trustManager: X509TrustManager by lazy {
        val keyStore = KeyStore.getInstance(KeyStore.getDefaultType()).apply { load(null) }
        val certFactory = CertificateFactory.getInstance("X.509")
        val context = MyApplication.context
        val resources = context.resources
        var index = 0
        var count = 0
        while (true) {
            // 资源名固定三位数：cacert_000 ~ cacert_120
            val resId = resources.getIdentifier(
                "cacert_%03d".format(index), "raw", context.packageName
            )
            if (resId == 0) break
            resources.openRawResource(resId).use { input ->
                certFactory.generateCertificates(input).forEach { cert ->
                    keyStore.setCertificateEntry("bundled_ca_$index", cert)
                }
            }
            index++
            count++
        }
        check(count > 0) { "内置 CA 证书库为空，请检查 res/raw/cacert_*.pem" }
        Log.i(TAG, "内置 CA 信任库加载完成：$count 张证书")

        val tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        tmf.init(keyStore)
        tmf.trustManagers.filterIsInstance<X509TrustManager>().first()
    }

    /** 应用到 OkHttp 客户端；开关关闭时原样返回（走系统默认校验） */
    fun apply(builder: OkHttpClient.Builder): OkHttpClient.Builder {
        if (!PrefManager.isVerifySsl) return builder

        val sslContext = SSLContext.getInstance("TLS").apply {
            init(null, arrayOf<TrustManager>(trustManager), SecureRandom())
        }
        return builder.sslSocketFactory(sslContext.socketFactory, trustManager)
    }
}
