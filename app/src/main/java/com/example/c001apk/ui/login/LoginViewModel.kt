package com.example.c001apk.ui.login

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.c001apk.adapter.FooterState
import com.example.c001apk.logic.model.LoginResponse
import com.example.c001apk.logic.repository.NetworkRepo
import com.example.c001apk.util.CookieUtil
import com.example.c001apk.util.Event
import com.example.c001apk.util.LoginUtils.createRandomNumber
import com.example.c001apk.util.LoginUtils.createRequestHash
import com.example.c001apk.util.PrefManager
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jsoup.Jsoup
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val networkRepo: NetworkRepo
): ViewModel() {

    var uid: String? = null
    var url: String? = null
    var listSize: Int = -1
    var type: String? = null
    var isInit: Boolean = true
    var page = 1
    var lastItem: String? = null
    var isRefreshing: Boolean = false
    var isLoadMore: Boolean = false
    var isEnd: Boolean = false

    var requestHash: String? = null
    val footerState = MutableLiveData<FooterState>()
    var loginData = HashMap<String, String?>()
    val toastText = MutableLiveData<Event<String>>()
    val showCaptcha = MutableLiveData<Event<Bitmap>>()
    val getCaptcha = MutableLiveData<Event<String>>()
    val afterLogin = MutableLiveData<Event<Boolean>>()

    fun onPreGetLoginParam() {
        viewModelScope.launch(Dispatchers.IO) {
            networkRepo.preGetLoginParam()
                .collect { result ->
                    val response = result.getOrNull()
                    val body = response?.body()?.string()

                    body?.apply {
                        requestHash = Jsoup.parse(this).createRequestHash()
                    }
                    response?.apply {
                        try {
                            val cookies = response.headers().values("Set-Cookie")
                            val session = cookies[0]
                            val sessionID = session.substring(0, session.indexOf(";"))
                            CookieUtil.SESSID = sessionID
                        } catch (e: Exception) {
                            toastText.postValue(Event("无法获取cookie"))
                            e.printStackTrace()
                            return@collect
                        }
                        CookieUtil.isGetLoginParam = true
                        onGetLoginParam()
                    }
                }
        }
    }

    private fun onGetLoginParam() {
        viewModelScope.launch(Dispatchers.IO) {
            networkRepo.getLoginParam()
                .collect { result ->
                    val response = result.getOrNull()
                    val body = response?.body()?.string()
                    body?.apply {
                        requestHash = Jsoup.parse(this).createRequestHash()
                    }
                    response?.apply {
                        try {
                            val cookies = response.headers().values("Set-Cookie")
                            val session = cookies[0]
                            val sessionID = session.substring(0, session.indexOf(";"))
                            CookieUtil.SESSID = sessionID
                        } catch (e: Exception) {
                            toastText.postValue(Event("无法获取cookie"))
                            e.printStackTrace()
                        }
                    }
                }
        }
    }

    fun onGetCaptcha() {
        val timeStamp = System.currentTimeMillis().toString()
        viewModelScope.launch(Dispatchers.IO) {
            networkRepo.getCaptcha("/auth/showCaptchaImage?$timeStamp")
                .collect { result ->
                    val response = result.getOrNull()
                    response?.let {
                        val responseBody = response.body()
                        val bitmap = BitmapFactory.decodeStream(responseBody?.byteStream())
                        showCaptcha.postValue(Event(bitmap))
                    }
                }
        }
    }

    /**
     * 短信登录：先取 requestHash，再请服务端下发短信验证码。
     * 走官方网页的 /auth/login?type=mobile 接口，与 LoginCookiesInterceptor 里的
     * isGetSmsLoginParam / isGetSmsToken 两个标记配套。
     */
    fun onSendSmsToken(mobile: String) {
        viewModelScope.launch(Dispatchers.IO) {
            CookieUtil.isGetSmsLoginParam = true
            networkRepo.getSmsLoginParam()
                .collect { result ->
                    val body = result.getOrNull()?.body()?.string()
                    if (!body.isNullOrEmpty()) {
                        Jsoup.parse(body).createRequestHash()
                            .takeIf { it.isNotEmpty() }
                            ?.let { requestHash = it }
                    }

                    val data = HashMap<String, String?>()
                    data["submit"] = "1"
                    data["login"] = mobile
                    data["mobile"] = mobile
                    data["requestHash"] = requestHash
                    data["randomNumber"] = createRandomNumber()

                    CookieUtil.isGetSmsToken = true
                    networkRepo.getSmsToken(data)
                        .collect { smsResult ->
                            val text = smsResult.getOrNull()?.body()?.string().orEmpty()
                            val message = runCatching {
                                Gson().fromJson(text, LoginResponse::class.java)?.message
                            }.getOrNull()
                            toastText.postValue(
                                Event(
                                    when {
                                        !message.isNullOrBlank() -> message
                                        text.isBlank() -> "验证码发送失败，可改用网页登录"
                                        else -> "验证码已发送"
                                    }
                                )
                            )
                        }
                }
        }
    }

    fun onTryLogin() {
        viewModelScope.launch(Dispatchers.IO) {
            networkRepo.tryLogin(loginData)
                .collect { result ->
                    val response = result.getOrNull()
                    response?.body()?.let {
                        val login: LoginResponse = Gson().fromJson(
                            response.body()?.string(),
                            LoginResponse::class.java
                        )
                        if (login.status == 1) {
                            // 1) 优先取响应体里直接给出的登录态
                            var uid: String? = login.uid
                            var name: String? = login.username
                            var token: String? = login.token

                            // 2) 回退：老接口把登录态放在 Set-Cookie 里（顺序不保证，只在缺失时补）
                            if (uid.isNullOrEmpty() || name.isNullOrEmpty() || token.isNullOrEmpty()) {
                                val cookies = runCatching {
                                    response.headers().values("Set-Cookie")
                                }.getOrDefault(emptyList())
                                if (uid.isNullOrEmpty() && cookies.size >= 3)
                                    uid = cookies[cookies.size - 3]
                                        .substringAfter("uid=").substringBefore(";")
                                if (name.isNullOrEmpty() && cookies.size >= 2)
                                    name = cookies[cookies.size - 2]
                                        .substringAfter("username=").substringBefore(";")
                                if (token.isNullOrEmpty() && cookies.isNotEmpty())
                                    token = cookies[cookies.size - 1]
                                        .substringAfter("token=").substringBefore(";")
                            }

                            val uidValue = uid?.takeIf { it.isNotEmpty() }
                            val nameValue = name?.takeIf { it.isNotEmpty() }
                            val tokenValue = token?.takeIf { it.isNotEmpty() }
                            if (uidValue != null && nameValue != null && tokenValue != null) {
                                PrefManager.isLogin = true
                                PrefManager.uid = uidValue
                                PrefManager.username = nameValue
                                PrefManager.token = tokenValue
                                this@LoginViewModel.uid = uidValue
                                onGetProfile()
                            } else {
                                toastText.postValue(Event("登录凭证解析失败，可改用网页登录"))
                            }
                        } else {
                            login.message?.let {
                                getCaptcha.postValue(Event(it))
                            }
                        }
                    }
                }
        }
    }

    private fun onGetProfile() {
        viewModelScope.launch(Dispatchers.IO) {
            networkRepo.getProfile(uid.toString())
                .collect { result ->
                    val data = result.getOrNull()
                    data?.data.let {
                        PrefManager.userAvatar = data?.data?.userAvatar.toString()
                        PrefManager.level = data?.data?.level.toString()
                        PrefManager.experience = data?.data?.experience.toString()
                        PrefManager.nextLevelExperience = data?.data?.nextLevelExperience.toString()
                        afterLogin.postValue(Event(true))
                    }
                }
        }


    }

}