package com.example.c001apk.util

import android.content.Context
import android.util.Base64
import com.example.c001apk.constant.Constants
import com.example.c001apk.util.Utils.getBase64
import com.example.c001apk.util.Utils.getMD5
import com.example.c001apk.util.Utils.randomAndroidVersionRelease
import com.example.c001apk.util.Utils.randomBrand
import com.example.c001apk.util.Utils.randomDeviceModel
import com.example.c001apk.util.Utils.randomManufacturer
import com.example.c001apk.util.Utils.randomSdkInt
import org.mindrot.jbcrypt.BCrypt
import java.security.MessageDigest
import java.util.Random


object TokenDeviceUtils {

    fun randHexString(@Suppress("SameParameterValue") n: Int): String {
        Random().setSeed(System.currentTimeMillis())
        return (0 until n).joinToString("") {
            Random().nextInt(256).toString(16)
        }.uppercase()
    }

    /** 本机真实机型；[deviceCode] 即上报给服务器的设备串 */
    data class RealDevice(
        val manufacturer: String,
        val brand: String,
        val model: String,
        val buildNumber: String,
        val androidVersion: String,
        val sdkInt: String,
        val deviceCode: String,
    )

    /** 读系统属性得到本机机型（机型检测的入口） */
    fun detectRealDevice(): RealDevice {
        val manufacturer = sanitize(android.os.Build.MANUFACTURER, Constants.DEFAULT_MANUFACTURER)
        val brand = sanitize(android.os.Build.BRAND, Constants.DEFAULT_BRAND)
        val model = sanitize(android.os.Build.MODEL, Constants.DEFAULT_MODEL)
        val buildNumber = sanitize(android.os.Build.DISPLAY, Constants.DEFAULT_BUILDNUMBER)
        val androidVersion = sanitize(
            android.os.Build.VERSION.RELEASE, Constants.DEFAULT_ANDROID_VERSION
        )
        val sdkInt = android.os.Build.VERSION.SDK_INT.toString()
        return RealDevice(
            manufacturer = manufacturer,
            brand = brand,
            model = model,
            buildNumber = buildNumber,
            androidVersion = androidVersion,
            sdkInt = sdkInt,
            deviceCode = buildDeviceCode(manufacturer, brand, model, buildNumber),
        )
    }

    /** 字段里不能出现分隔符 `;`，空值回落到默认值 */
    private fun sanitize(value: String?, fallback: String): String =
        value?.trim()?.replace(";", "")?.takeIf { it.isNotEmpty() } ?: fallback

    /**
     * 用给定的机型字段重建设备串：**szlmId、MAC、尾部 64hex 一律沿用官方串**
     * （只替换厂商/品牌/型号/版本号这四个字段）。
     *
     * 为什么只换这四个字段：实测（`_rev/test_device_model.py`）保留官方 szlmId/尾字段、
     * 只改机型时，16.4.0（2607021）在 feed/detail、main/indexV8、feed/createFeed 全部正常；
     * 而早期「随机 szlmId + 随机 MAC + null 尾字段」的整串新造会被风控要求验证码
     * （`err_request_captcha_v2`）。服务端正是按这里的品牌/型号认机型，
     * 帖子/回复下方的 `device_title`（「来自 xxx」）就是它的映射结果。
     */
    fun buildDeviceCode(
        manufacturer: String,
        brand: String,
        model: String,
        buildNumber: String,
        base: String = Constants.DEFAULT_DEVICE_CODE,
    ): String {
        val parts = DeviceCode.decode(base).split(";").toMutableList()
        if (parts.size < 8) return base
        parts[4] = " $manufacturer"
        parts[5] = " $brand"
        parts[6] = " $model"
        parts[7] = " $buildNumber"
        return DeviceCode.encode(parts.joinToString(";"))
    }

    fun getDeviceCode(regenerate: Boolean): String {
        if (regenerate) {
            PrefManager.apply {
                MANUFACTURER = randomManufacturer()
                BRAND = randomBrand()
                MODEL = randomDeviceModel()
                BUILDNUMBER = randHexString(16)
                SDK_INT = randomSdkInt()
                ANDROID_VERSION = randomAndroidVersionRelease()
                USER_AGENT =
                    "Dalvik/2.1.0 (Linux; U; Android $ANDROID_VERSION; ${MODEL} ${BUILDNUMBER}) (#Build; ${BRAND}; ${MODEL}; ${BUILDNUMBER}; $ANDROID_VERSION) +CoolMarket/${VERSION_NAME}-${VERSION_CODE}-${Constants.MODE}"
            }
        }
        // 随机也只随「机型字段」，szlmId/MAC/尾字段仍是官方那一组，否则会被风控拦
        return buildDeviceCode(
            PrefManager.MANUFACTURER,
            PrefManager.BRAND,
            PrefManager.MODEL,
            PrefManager.BUILDNUMBER
        )
    }

    fun String.getTokenV2(): String {
        val timeStamp = (System.currentTimeMillis() / 1000f).toString()

        val base64TimeStamp = timeStamp.getBase64()
        val md5TimeStamp = timeStamp.getMD5()
        val md5DeviceCode = this.getMD5()

        val token = "${Constants.APP_LABEL}?$md5TimeStamp$$md5DeviceCode&${Constants.APP_ID}"
        val base64Token = token.getBase64()
        val md5Base64Token = base64Token.getMD5()
        val md5Token = token.getMD5()

        val bcryptSalt = "${"$2a$10$$base64TimeStamp/$md5Token".substring(0, 31)}u"
        val bcryptResult = BCrypt.hashpw(md5Base64Token, bcryptSalt)

        return "v2${bcryptResult.replaceRange(0, 3, "$2y").getBase64()}"
    }

    /**
     * 酷安 X-App-Token v3（逆向自官方 libauth.so 的 getToken，与 _rev/v3_token.py 等价）。
     *
     *   off   = 4 * ((t + versionCode) % 100) + 128
     *   B2    = base64_decode(KEY[off, off + 128])                  // 96 字节，可能含任意二进制
     *   S     = packageName & B2 & md5hex(device) & t & versionCode // 全程按原始字节拼接
     *   salt  = base64(hex(t) + "/" + md5hex(S))[:22]
     *   key   = md5hex(base64(S))    // bcrypt 的 password，注意不是 md5hex(S)
     *   token = "v3" + base64(bcrypt(key, "$2y$04$" + salt))
     *
     * 约束：versionCode 必须与同一请求的 X-App-Code 一致，device 必须与 X-App-Device 一致。
     */
    fun String.getTokenV3(
        versionCode: String,
        timestamp: Long = System.currentTimeMillis() / 1000L
    ): String {
        val vc = versionCode.toLongOrNull() ?: Constants.VERSION_CODE.toLong()
        val off = (4 * ((timestamp + vc) % 100) + 128).toInt()

        val b2 = Base64.decode(KEY, off, 128, Base64.NO_WRAP)
        val s = Constants.APP_ID.toByteArray()
            .plus("&".toByteArray())
            .plus(b2)
            .plus("&".toByteArray())
            .plus(toByteArray().md5Hex().toByteArray())
            .plus("&".toByteArray())
            .plus(timestamp.toString().toByteArray())
            .plus("&".toByteArray())
            .plus(vc.toString().toByteArray())

        val salt = Base64.encodeToString(
            "${timestamp.toString(16)}/${s.md5Hex()}".toByteArray(),
            Base64.NO_WRAP
        ).substring(0, 22)

        // jbcrypt 不认 $2y 盐：用 $2a 生成后改写前缀（哈希本体相同，与官方实现逐字节一致）
        val hash = "\$2y" + BCrypt.hashpw(
            Base64.encodeToString(s, Base64.NO_WRAP).toByteArray().md5Hex(),
            "\$2a\$04\$$salt"
        ).substring(3)

        return "v3" + Base64.encodeToString(hash.toByteArray(), Base64.NO_WRAP)
    }

    private fun ByteArray.md5Hex(): String {
        val digest = MessageDigest.getInstance("MD5").digest(this)
        val sb = StringBuilder(digest.size * 2)
        for (b in digest) {
            val v = b.toInt() and 0xff
            sb.append(HEX_CHARS[v ushr 4]).append(HEX_CHARS[v and 0x0f])
        }
        return sb.toString()
    }

    private const val HEX_CHARS = "0123456789abcdef"

    /** libauth.so 中 930 字节的 KEY 表（此处为其 base64 文本，跨版本稳定） */
    private const val KEY_B64 =
        "VFRCVU9GUXNRMEVsTFVNa1dERWpRU0VzVXlFbUxETkFVeTFEUEZZdUl5MGlNVU01SXpCVUpGY3RNeWtoTVVRMUpTMURPU1V4TXpoUUxWTXdWMDBzSXloU01UUXNXVEZEUkZFd1EwUlZNU1FrVVMwaktTTXR" +
        "KRGhZTFZRa1VqRXpLU1FzTTBCVkxUUXdVaTAwTlNNc00yQlhMVk1sSkMxRE5GbE5MQ1EwV1RFekxTUXhRMEVoTEVNbElqRXpLRmN3TXowbExpTkVWekVqUFNZdU5Ea2tMRk13Vml4VEtGa3RRemhYTEZNc1" +
        "dDNGpMU1l0VTJCVFRUQTBMRkV1TXpoVExqTWhJakZETUZNc0kwQlZMQ1FrVUN3ekpTSXNJekJTTUVNOFVDeEVMU0l0STJCUUxVUW9VeTBqTEZNc0l5VWtNRlF3VmswdFJDeFlNVE1sSkM0ME5TRXdVMFJRT" +
        "EZNaElpMGtOU013VTJCUUxqTmdVeXdqT0ZFc05DeFRMRU13V0N4VUxTRXRJeVJUTEROQkl5MDBKRlpOTVVNc1VEQkVKRmd0TkMwa0xEUWtXQzFETUZNeFJDa2pMVk0wVml4VE1GZ3hJeTBtTFZNeEpTMHpK" +
        "RlF4TTJCUUxTUTRXVEJETUZNdUkwVWhUU3d6UEZndFJEQlJNRE5FVVMwak9GWXVKRGtsTVNRMFV6QXpOU013UkRra01UTWtWU3hVTUZRdFJDeFlNVE5GSXl4RUxGWXNJelVoTGlRMFYwMHhOQ3hWTENNMFZ" +
        "5NHpKRlV4TkN4VExGUTRWeTRqWUZZeEpEVWpMVU13VXpGRFBGSXNNMEVsTERNc1ZEQTBPU011TkNrbUxFUWtWaXhFTVNKTkxUTW9VaXd6TEZjdFUwUlFMaVF3VlRGREpTVXdVeWtqTVVNaEl5MHpJU1VzST" +
        "BFak1UTXdVUzFVTkZjeFJERWxMVVF0SlM0a05GSXNJemhaVFRCVFlGRXdReVJZTFNNbElTNDBMRmdzVkRSWkxVUW9XVEJEUUZZdFJDeFdMak5FVkRCRE1Ga3RORFVsTEZRNFZTMVVKRmd3TXpSV0xEUTRVV" +
        "TB3UXkwbUxVUTRXUzFUTVNNc1V5VWlNVU5BVVN4VUxGWXVJMFJVTGlNc1dERkVMRk14STJCUUxWTTBXUzRrT0Zjd00wUlhMVk1vV0MwaktGRXhMQ00wVml4VE5GQXRSRFJXTFVRbEpDMVRKRkF3TkN4Z1lB"

    private val KEY: ByteArray by lazy { Base64.decode(KEY_B64, Base64.NO_WRAP) }

    /**
     * 返回可用于请求的设备指纹。
     *
     * 规则（2026-09-13 起，「上报真实机型」默认开启）：
     *  1. 用户在「设置 - 参数」里显式改过 → 完全按他的来，不再干预；
     *  2. 否则按 [PrefManager.reportRealDevice] 决定上报本机机型还是官方那一组，
     *     只要本地值和服务端期望值不一致就自动重建（系统升级、换机都能自己跟上）。
     *
     * 两种取值都**只换机型字段**，szlmId/MAC/尾字段始终是官方串里的值；整串新造
     * （随机 szlmId + 随机 MAC + null 尾字段）会被风控要求人机验证
     * （`err_request_captcha_v2`），详情页等接口直接加载失败。
     */
    fun getLastingDeviceCode(): String {
        // 用户显式指定过自定义设备串 → 完全按他的来，不再干预
        if (PrefManager.customFingerprint) return PrefManager.xAppDevice
        if (PrefManager.reportRealDevice) {
            // 上报真实机型：帖子/回复下方显示「来自 <本机机型>」（服务端按品牌+型号映射）
            val device = detectRealDevice()
            if (PrefManager.xAppDevice != device.deviceCode) applyRealDeviceFingerprint(device)
        } else if (PrefManager.DEVICE_FINGERPRINT_VERSION != FINGERPRINT_VERSION ||
            PrefManager.xAppDevice != Constants.DEFAULT_DEVICE_CODE
        ) {
            applyDefaultFingerprint()
        }
        return PrefManager.xAppDevice
    }

    /** 把设备指纹重置为官方认可的一组（device 与 UA 必须配套，缺一不可） */
    fun applyDefaultFingerprint() {
        PrefManager.apply {
            customFingerprint = false
            xAppDevice = Constants.DEFAULT_DEVICE_CODE
            MANUFACTURER = Constants.DEFAULT_MANUFACTURER
            BRAND = Constants.DEFAULT_BRAND
            MODEL = Constants.DEFAULT_MODEL
            BUILDNUMBER = Constants.DEFAULT_BUILDNUMBER
            SDK_INT = Constants.DEFAULT_SDK_INT
            ANDROID_VERSION = Constants.DEFAULT_ANDROID_VERSION
            USER_AGENT = defaultUserAgent()
            DEVICE_FINGERPRINT_VERSION = FINGERPRINT_VERSION
        }
    }

    /** 机型检测结果落到设备串 + 展示字段（device 与 UA 必须配套，缺一不可） */
    fun applyRealDeviceFingerprint(device: RealDevice = detectRealDevice()) {
        PrefManager.apply {
            customFingerprint = false
            xAppDevice = device.deviceCode
            MANUFACTURER = device.manufacturer
            BRAND = device.brand
            MODEL = device.model
            BUILDNUMBER = device.buildNumber
            SDK_INT = device.sdkInt
            ANDROID_VERSION = device.androidVersion
            USER_AGENT = userAgentOf(
                device.brand, device.model, device.buildNumber, device.androidVersion
            )
            DEVICE_FINGERPRINT_VERSION = FINGERPRINT_VERSION
        }
    }

    /** 与 [Constants.DEFAULT_DEVICE_CODE] 配套的 UA；版本号跟随当前客户端设置，保持头之间自洽 */
    fun defaultUserAgent(): String = userAgentOf(
        Constants.DEFAULT_BRAND,
        Constants.DEFAULT_MODEL,
        Constants.DEFAULT_BUILDNUMBER,
        Constants.DEFAULT_ANDROID_VERSION
    )

    /** 按给定机型字段拼 UA；版本号跟随当前客户端设置，保证与 X-App-Device 自洽 */
    fun userAgentOf(brand: String, model: String, buildNumber: String, android: String): String =
        "Dalvik/2.1.0 (Linux; U; Android $android; $model $buildNumber) " +
            "(#Build; $brand; $model; $buildNumber; $android) " +
            "+CoolMarket/${PrefManager.VERSION_NAME}-${PrefManager.VERSION_CODE}-${Constants.MODE}"

    /** 设备指纹版本号；改动默认指纹时 +1，老安装会在下次请求时自动升级 */
    private const val FINGERPRINT_VERSION = 2

    fun getLastingInstallTime(context: Context): String {
        val sp = context.getSharedPreferences(context.packageName, Context.MODE_PRIVATE)
        return sp.getString("INSTALL_TIME", null).let {
            it ?: System.currentTimeMillis().toString().apply {
                sp.edit().putString("INSTALL_TIME", this).apply()
            }
        }
    }

}