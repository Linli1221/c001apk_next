package com.example.c001apk.util

import android.util.Base64
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets.UTF_8

object DeviceCode {
    fun encode(deviceInfo: String): String {
        val charset: Charset = UTF_8
        val bytes = deviceInfo.toByteArray(charset)
        val encodeToString = Base64.encodeToString(bytes, 0)
        val replace = StringBuilder(encodeToString).reverse().toString()
        return Regex("\\r\\n|\\r|\\n|=").replace(replace, "")
    }

    /**
     * [encode] 的逆运算：反转 -> base64 解码。
     *
     * 解出来形如 `szlmId; ; ; mac; 厂商; 品牌; 型号; 版本号; 尾部64hex`，
     * 其中「厂商/品牌/型号」就是服务端用来认机型（帖子下方「来自 xxx」）的字段。
     */
    fun decode(deviceCode: String): String {
        val reversed = deviceCode.reversed()
        val padding = (4 - reversed.length % 4) % 4
        val bytes = Base64.decode(reversed + "=".repeat(padding), Base64.DEFAULT)
        return String(bytes, UTF_8)
    }
}