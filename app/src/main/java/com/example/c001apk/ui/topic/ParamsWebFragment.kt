package com.example.c001apk.ui.topic

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.SslErrorHandler
import android.webkit.SslError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import com.example.c001apk.databinding.FragmentParamsWebBinding
import com.example.c001apk.util.NetWorkUtil.openLink
import com.example.c001apk.util.PrefManager
import com.example.c001apk.util.SslErrorPrompter

/**
 * 产品页「参数」标签。
 *
 * 这个标签官方就是一个 H5 规格页（m.coolapk.com/mp/product/configInfo?id=xxx&drawNav=1，
 * 服务端直出整张参数表，免登录可用），原生接口里只有 productConfigList/listCard 两种卡片，
 * 没有对应的适配器，所以直接内嵌 WebView，与官方表现一致。
 */
class ParamsWebFragment : Fragment() {

    private var _binding: FragmentParamsWebBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentParamsWebBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val url = arguments?.getString("url").orEmpty()
        if (url.isEmpty()) {
            binding.progress.isVisible = false
            return
        }

        with(binding.webView.settings) {
            javaScriptEnabled = true
            domStorageEnabled = true
            blockNetworkImage = false
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            useWideViewPort = true
            loadWithOverviewMode = true
            defaultTextEncodingName = "UTF-8"
            userAgentString = PrefManager.USER_AGENT
            if (SDK_INT >= 32) {
                if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING))
                    WebSettingsCompat.setAlgorithmicDarkeningAllowed(this, true)
            } else if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
                val nightModeFlags =
                    resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
                if (nightModeFlags == Configuration.UI_MODE_NIGHT_YES)
                    WebSettingsCompat.setForceDark(this, WebSettingsCompat.FORCE_DARK_ON)
            }
        }

        // 与 WebViewActivity 保持一致，H5 里带上登录态
        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setCookie("m.coolapk.com", "DID=${PrefManager.SZLMID}")
            setCookie("m.coolapk.com", "forward=https://www.coolapk.com")
            setCookie("m.coolapk.com", "displayVersion=v14")
            setCookie("m.coolapk.com", "uid=${PrefManager.uid}")
            setCookie("m.coolapk.com", "username=${PrefManager.username}")
            setCookie("m.coolapk.com", "token=${PrefManager.token}")
        }

        binding.webView.webViewClient = object : WebViewClient() {
            /** SSL 证书校验不过（如抓包/中间人）：阻止加载并弹风险警告 */
            override fun onReceivedSslError(
                view: WebView?, handler: SslErrorHandler?, error: SslError?
            ) {
                SslErrorPrompter.onSslFailure(
                    error?.let { java.security.cert.CertificateException(it.toString()) }
                )
                handler?.cancel()
            }

            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean {
                val link = request.url.toString()
                // 页面内的自定义 scheme 交给 App 处理，http(s) 留在 WebView 里
                return if (link.startsWith("http")) false
                else {
                    openLink(requireContext(), link, null)
                    true
                }
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                binding.progress.isVisible = false
            }
        }

        binding.webView.loadUrl(url)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding?.webView?.apply {
            stopLoading()
            loadUrl("about:blank")
            destroy()
        }
        _binding = null
    }

    companion object {
        @JvmStatic
        fun newInstance(url: String) = ParamsWebFragment().apply {
            arguments = Bundle().apply { putString("url", url) }
        }
    }

}
