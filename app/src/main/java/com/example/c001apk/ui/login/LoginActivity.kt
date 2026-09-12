package com.example.c001apk.ui.login

import android.content.Intent
import android.os.Bundle
import android.text.InputFilter
import android.text.InputFilter.LengthFilter
import android.text.InputType
import android.text.Spanned
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.view.isVisible
import com.example.c001apk.R
import com.example.c001apk.databinding.ActivityLoginBinding
import com.example.c001apk.ui.base.BaseActivity
import com.example.c001apk.ui.main.MainActivity
import com.example.c001apk.util.ActivityCollector
import com.example.c001apk.util.CookieUtil.isGetCaptcha
import com.example.c001apk.util.CookieUtil.isGetSmsLoginParam
import com.example.c001apk.util.CookieUtil.isPreGetLoginParam
import com.example.c001apk.util.CookieUtil.isTryLogin
import com.example.c001apk.util.LoginUtils.createRandomNumber
import com.example.c001apk.util.PrefManager
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LoginActivity : BaseActivity<ActivityLoginBinding>() {

    private val viewModel by viewModels<LoginViewModel>()
    private var isLoginPass = true

    /** 网页登录返回后，若已拿到登录态就复用表单登录的收尾逻辑 */
    private val webLoginLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (PrefManager.isLogin) afterLogin()
        }

    private val filter =
        InputFilter { source: CharSequence, _: Int, _: Int, _: Spanned?, _: Int, _: Int ->
            if (source == " ")
                return@InputFilter ""
            else
                return@InputFilter null
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setSupportActionBar(binding.toolBar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        initObserve()

        isPreGetLoginParam = true
        viewModel.onPreGetLoginParam()

        binding.apply {
            account.filters = arrayOf(filter)
            password.filters = arrayOf(filter)
            sms.filters = arrayOf(filter)
            captchaText.filters = arrayOf(filter)
        }

        binding.login.setOnClickListener {
            if (isLoginPass) {
                if (binding.account.text.toString() == "" || binding.password.text.toString() == "")
                    Toast.makeText(this, "用户名或密码为空", Toast.LENGTH_SHORT).show()
                else
                    tryLogin()
            } else {
                if (binding.account.text.toString() == "" || binding.sms.text.toString() == "")
                    Toast.makeText(this, "手机号或验证码为空", Toast.LENGTH_SHORT).show()
                else
                    tryLogin()
            }

        }

        binding.getSMS.setOnClickListener {
            sendSmsToken()
        }

        binding.captchaImg.setOnClickListener {
            getCaptcha()
        }

    }

    private fun initObserve() {
        viewModel.getCaptcha.observe(this) { event ->
            event.getContentIfNotHandledOrReturnNull()?.let {
                Log.e("LoginActivity", "服务端登录返回: $it")
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                when (it) {
                    "图形验证码不能为空" -> {
                        binding.captchaImg.isVisible = true
                        binding.captchaLayout.isVisible = true
                        getCaptcha()
                    }

                    "图形验证码错误" -> getCaptcha()

                    "密码错误" -> {
                        if (binding.captchaImg.isVisible)
                            getCaptcha()
                    }

                    else -> {
                        // 服务端要求验证码（例如短信二次验证），而界面此前并没有展示输入框 → 兜底显示。
                        // 文案不固定（"验证码已发送"/"请填写短信验证码"/"安全验证"…），这里放宽匹配，
                        // 避免服务端换了话术后用户看不到输入框。
                        if (it.contains("验证") || it.contains("码") || it.contains("短信")) {
                            binding.smsLayout.isVisible = true
                            binding.getSMS.isVisible = true
                        }
                    }
                }
            }
        }

        viewModel.showCaptcha.observe(this) { event ->
            event.getContentIfNotHandledOrReturnNull()?.let {
                binding.captchaImg.setImageBitmap(it)
            }
        }

        viewModel.toastText.observe(this) { event ->
            event.getContentIfNotHandledOrReturnNull()?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.afterLogin.observe(this) { event ->
            event.getContentIfNotHandledOrReturnNull()?.let {
                afterLogin()
            }
        }
    }

    private fun sendSmsToken() {
        val mobile = binding.account.text.toString()
        if (mobile.isEmpty()) {
            Toast.makeText(this, "请先填写手机号", Toast.LENGTH_SHORT).show()
            return
        }
        Toast.makeText(this, "正在发送验证码...", Toast.LENGTH_SHORT).show()
        viewModel.onSendSmsToken(mobile)
    }

    private fun tryLogin() {
        Toast.makeText(this, "正在登录...", Toast.LENGTH_SHORT).show()
        isTryLogin = true
        viewModel.loginData["submit"] = "1"
        viewModel.loginData["randomNumber"] = createRandomNumber()
        viewModel.loginData["requestHash"] = viewModel.requestHash
        viewModel.loginData["login"] = binding.account.text.toString()
        viewModel.loginData["password"] =
            if (isLoginPass) binding.password.text.toString() else ""
        viewModel.loginData["captcha"] = binding.captchaText.text.toString()
        // 短信验证码：短信登录、以及服务端要求二次验证时都要带上
        viewModel.loginData["code"] = binding.sms.text.toString()
        viewModel.onTryLogin()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.login_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()

            R.id.loginPass -> {
                isLoginPass = true
                binding.account.inputType = InputType.TYPE_CLASS_TEXT
                binding.account.filters = arrayOf(LengthFilter(99), filter)
                binding.passLayout.isVisible = true
                binding.smsLayout.isVisible = false
                binding.getSMS.isVisible = false
            }

            R.id.loginPhone -> {
                isLoginPass = false
                binding.account.inputType = InputType.TYPE_CLASS_NUMBER
                binding.account.filters = arrayOf(LengthFilter(11), filter)
                binding.getSMS.isVisible = true
                binding.smsLayout.isVisible = true
                binding.passLayout.isVisible = false
                isGetSmsLoginParam = true
            }

            R.id.loginWeb -> webLoginLauncher.launch(
                Intent(this, WebLoginActivity::class.java)
            )
        }
        return true
    }

    private fun getCaptcha() {
        isGetCaptcha = true
        viewModel.onGetCaptcha()
    }

    private fun afterLogin() {
        ActivityCollector.recreateActivity(MainActivity::class.java.name)
        Toast.makeText(this, "登录成功", Toast.LENGTH_SHORT).show()
        finish()
    }

}
