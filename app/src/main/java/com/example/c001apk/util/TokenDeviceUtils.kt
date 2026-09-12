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
        val szlmId = if (PrefManager.SZLMID == "") randHexString(16) else PrefManager.SZLMID
        val mac = Utils.randomMacAddress()
        val manuFactor = PrefManager.MANUFACTURER
        val brand = PrefManager.BRAND
        val model = PrefManager.MODEL
        val buildNumber = PrefManager.BUILDNUMBER
        return DeviceCode.encode("$szlmId; ; ; $mac; $manuFactor; $brand; $model; $buildNumber; null")
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
     * 这里**不再随机生成**：随机伪造的 device 会被酷安风控要求人机验证
     * （`err_request_captcha_v2`「当前访问需要验证码」），详情页等接口直接加载失败。
     * 只有在本地指纹版本落后时，才重置成官方认可的一组（见 `Constants.DEFAULT_*`）；
     * 之后用户仍可在「设置 - 参数」里手动覆盖，不会被再次重置。
     */
    fun getLastingDeviceCode(): String {
        // 用户显式指定过自定义设备串 → 完全按他的来，不再干预
        if (PrefManager.customFingerprint) return PrefManager.xAppDevice
        if (PrefManager.DEVICE_FINGERPRINT_VERSION != FINGERPRINT_VERSION) {
            applyDefaultFingerprint()
        } else if (PrefManager.xAppDevice != Constants.DEFAULT_DEVICE_CODE) {
            // 指纹版本已是最新、但设备串仍不是官方那一组 —— 说明被「设置 - 参数」页
            // （改机型/品牌/系统信息、改 SZLMID 等都会走 getDeviceCode）改坏了：
            // 这种串带随机 MAC / 新 szlmId，服务端一律要求验证码（err_request_captcha_v2）。
            // 只还原设备串本身，不动 MODEL/UA 等仅用于展示的字段。
            PrefManager.xAppDevice = Constants.DEFAULT_DEVICE_CODE
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

    /** 与 [Constants.DEFAULT_DEVICE_CODE] 配套的 UA；版本号跟随当前客户端设置，保持头之间自洽 */
    fun defaultUserAgent(): String =
        "Dalvik/2.1.0 (Linux; U; Android ${Constants.DEFAULT_ANDROID_VERSION}; " +
            "${Constants.DEFAULT_MODEL} ${Constants.DEFAULT_BUILDNUMBER}) " +
            "(#Build; ${Constants.DEFAULT_BRAND}; ${Constants.DEFAULT_MODEL}; " +
            "${Constants.DEFAULT_BUILDNUMBER}; ${Constants.DEFAULT_ANDROID_VERSION}) " +
            "+CoolMarket/${PrefManager.VERSION_NAME}-${PrefManager.VERSION_CODE}-${Constants.MODE}"

    /** 设备指纹版本号；改动默认指纹时 +1，老安装会在下次请求时自动升级 */
    private const val FINGERPRINT_VERSION = 1

    fun getLastingInstallTime(context: Context): String {
        val sp = context.getSharedPreferences(context.packageName, Context.MODE_PRIVATE)
        return sp.getString("INSTALL_TIME", null).let {
            it ?: System.currentTimeMillis().toString().apply {
                sp.edit().putString("INSTALL_TIME", this).apply()
            }
        }
    }

}