package com.example.c001apk

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatDelegate
import com.example.c001apk.ui.others.BugHandlerActivity
import com.example.c001apk.util.PrefManager
import com.example.c001apk.util.TokenDeviceUtils
import dagger.hilt.android.HiltAndroidApp
import net.mikaelzero.mojito.Mojito
import net.mikaelzero.mojito.loader.glide.GlideImageLoader
import net.mikaelzero.mojito.view.sketch.SketchImageLoadFactory
import kotlin.system.exitProcess

@HiltAndroidApp
class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        context = applicationContext

        // SSL 校验失败 → 风险环境警告弹窗（跟踪前台 Activity）
        SslErrorPrompter.install(this)

        AppCompatDelegate.setDefaultNightMode(PrefManager.darkTheme)

        // 兜底：数字联盟 ID 为空时本地生成一个随机 ID 并保存
        //（仅用于 DID cookie 等场景；设备串里的 szlmId 仍沿用官方那一组，不受影响）
        if (PrefManager.SZLMID.isEmpty())
            PrefManager.SZLMID = TokenDeviceUtils.randHexString(16)

        Mojito.initialize(
            GlideImageLoader.with(this),
            SketchImageLoadFactory()
        )

        Thread.setDefaultUncaughtExceptionHandler { _, paramThrowable ->
            val exceptionMessage = android.util.Log.getStackTraceString(paramThrowable)

            val intent = Intent(this, BugHandlerActivity::class.java)
            intent.putExtra("exception_message", exceptionMessage)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)

            android.os.Process.killProcess(android.os.Process.myPid())
            exitProcess(10)
        }
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        lateinit var context: Context
    }

}