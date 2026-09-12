package com.example.c001apk.constant

import com.example.c001apk.MyApplication.Companion.context
import com.example.c001apk.util.PrefManager
import rikka.core.util.ResourceUtils

object Constants {
    const val REQUEST_WITH = "XMLHttpRequest"
    const val LOCALE = "zh-CN"
    const val APP_ID = "com.coolapk.market"
    var DARK_MODE =
        if (ResourceUtils.isNightMode(context.resources.configuration)) "1"
        else "0"
    const val CHANNEL = "coolapk"
    const val MODE = "universal"
    const val APP_LABEL = "token://com.coolapk.market/dcf01e569c1e3db93a3d0fcf191a622c"
    const val VERSION_NAME = "16.4.0"
    const val API_VERSION = "16"
    const val VERSION_CODE = "2607021"
    val USER_AGENT =
        "Dalvik/2.1.0 (Linux; U; Android ${PrefManager.ANDROID_VERSION}; ${PrefManager.MODEL} ${PrefManager.BUILDNUMBER}) (#Build; ${PrefManager.BRAND}; ${PrefManager.MODEL}; ${PrefManager.BUILDNUMBER}; ${PrefManager.ANDROID_VERSION}) +CoolMarket/${PrefManager.VERSION_NAME}-${PrefManager.VERSION_CODE}-${MODE}"

    // "${System.getProperty("http.agent")} (#Build; ${android.os.Build.BRAND}; ${android.os.Build.MODEL}; ${android.os.Build.DISPLAY}; ${android.os.Build.VERSION.RELEASE}) +CoolMarket/${VERSION_NAME}-${VERSION_CODE}-${MODE}"
    /**
     * 默认设备指纹（官方客户端上报、服务端认可的一组）。
     *
     * 为什么必须固定：随机伪造的 X-App-Device 会被酷安风控要求人机验证
     * （`err_request_captcha_v2`「当前访问需要验证码」），详情页等接口直接加载失败。
     * 实测见 `_rev/diff_headers.py` / `_rev/test_device_format.py`：
     * 同一套请求头里只把 device 换成下面这个，16.4.0 / 16.6.1 都能正常返回数据；
     * 而任何新造的 device（哪怕字段格式完全对齐）一律被要求验证码。
     * device 与 UA 必须配套，见 TokenDeviceUtils.applyDefaultFingerprint()。
     */
    const val DEFAULT_DEVICE_CODE =
        "lVDMjRWN2IzYjVTN3MmN2EDOiNWZjdjNhJTNkNTO3EWR4UzQzIkQ1EzN1YTMERTQFdjQ0cTMCF0QxYjNEJzM4AyOpEDMONEKzAzNuUjLw4iNx8FMxEjWKBFI7ATMxolSQByOzVHbQVmbPByOzVHbQVmbPByOgsDI7AyO3c2Xa9WaThFbxZTb0pXQplVW3FWMIJWWrZHZBdFNol1NMVFR"
    const val DEFAULT_MANUFACTURER = "OnePlus"
    const val DEFAULT_BRAND = "OnePlus"
    const val DEFAULT_MODEL = "PJZ110"
    const val DEFAULT_BUILDNUMBER = "PJZ110_16.0.5.703(CN01)"
    const val DEFAULT_ANDROID_VERSION = "16"
    const val DEFAULT_SDK_INT = "36"

    const val SZLM_ID = "数字联盟ID不能为空"
    const val LOADING_FAILED = "加载失败"
    const val LOADING_EMPTY = "什么也没有"
    const val LOADING_END = "没有更多了"
}