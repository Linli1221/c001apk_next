package com.example.c001apk.ui.user

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.FrameLayout
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.c001apk.R
import com.example.c001apk.adapter.LoadingState
import com.example.c001apk.databinding.ActivityEditProfileBinding
import com.example.c001apk.logic.model.UserProfileResponse
import com.example.c001apk.ui.article.ArticleUploadFile
import com.example.c001apk.ui.article.CropImageActivity
import com.example.c001apk.ui.base.BaseActivity
import com.example.c001apk.util.ImageUtil.getImageDimensionsAndMD5
import com.example.c001apk.util.ImageUtil.showIMG
import com.example.c001apk.util.ImageUtil.toHex
import com.example.c001apk.util.Utils.richToString
import com.example.c001apk.util.makeToast
import com.example.c001apk.util.ossUpload
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * 编辑资料。顺序按官方：头像 → 个人主页背景图 → 用户名 → 性别 → 生日 → 城市 → 个人签名。
 *
 * 每项点一下改一下存一下（没有「保存」按钮）：
 * - 头像：相册选图 → 方形裁剪 600x600 → multipart 上传（imgFile）→ 返回新头像地址
 * - 背景图：相册选图 → 方形裁剪 1080x1080 → OSS 上传 → changeAvatarCover
 * - 性别 / 生日 / 城市 / 签名：changeProfile；生日和城市可以设成「保密」（resetProfile）
 * - 用户名：官方 App 本来也不给改，点了只提示
 */
@AndroidEntryPoint
class EditProfileActivity : BaseActivity<ActivityEditProfileBinding>() {

    private val viewModel: EditProfileViewModel by viewModels()

    private lateinit var pickAvatarLauncher: ActivityResultLauncher<PickVisualMediaRequest>
    private lateinit var pickCoverLauncher: ActivityResultLauncher<PickVisualMediaRequest>
    private lateinit var cropLauncher: ActivityResultLauncher<Intent>

    /** 裁剪结果回来时区分是头像还是背景图 */
    private var cropForAvatar = true

    private var dialog: AlertDialog? = null

    /** 当前资料（头像 / 背景图提前预览用） */
    private var profile: UserProfileResponse.Data? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ViewCompat.setOnApplyWindowInsetsListener(binding.topBar) { v, insets ->
            val top = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top
            v.setPadding(v.paddingLeft, v.paddingTop + top, v.paddingRight, v.paddingBottom)
            insets
        }

        initLauncher()
        initView()
        initObserve()

        viewModel.fetchProfile()
    }

    private fun initView() {
        binding.back.setOnClickListener { finish() }

        binding.rowAvatar.setOnClickListener { pickImage(true) }
        binding.rowCover.setOnClickListener { pickImage(false) }

        binding.rowNickname.setOnClickListener { makeToast("用户名修改功能暂不提供") }
        binding.rowGender.setOnClickListener { pickGender() }
        binding.rowBirthday.setOnClickListener { pickBirthday() }
        binding.rowCity.setOnClickListener { pickCity() }
        binding.rowBio.setOnClickListener { editBio() }
    }

    private fun initLauncher() {
        pickAvatarLauncher =
            registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
                if (uri != null) startCrop(uri, true)
            }
        pickCoverLauncher =
            registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
                if (uri != null) startCrop(uri, false)
            }
        cropLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode != RESULT_OK) return@registerForActivityResult
                val uri = result.data?.getStringExtra(CropImageActivity.RESULT_URI)
                    ?.let { Uri.parse(it) } ?: return@registerForActivityResult

                if (cropForAvatar) {
                    viewModel.onPostAvatar(uri, contentResolver)
                } else {
                    uploadCover(uri)
                }
            }
    }

    private fun initObserve() {
        viewModel.profile.observe(this) { data ->
            profile = data
            bindProfile(data)
        }

        viewModel.loadingState.observe(this) { state ->
            when (state) {
                LoadingState.Loading -> showDialog()
                LoadingState.LoadingDone -> closeDialog()
                else -> closeDialog()
            }
        }

        viewModel.toastText.observe(this) { event ->
            event.getContentIfNotHandledOrReturnNull()?.let {
                closeDialog()
                makeToast(it)
            }
        }

        // 改完了：拉一次最新资料刷界面（头像 / 背景图 / 各字段）
        viewModel.over.observe(this) { event ->
            event.getContentIfNotHandledOrReturnNull()?.let {
                closeDialog()
                makeToast("已保存")
                viewModel.fetchProfile()
            }
        }

        // 背景图 OSS 上传凭证回来 → 真正上传 → 回传地址
        viewModel.uploadImage.observe(this) { event ->
            event.getContentIfNotHandledOrReturnNull()?.let { responseData ->
                val uri = cropUri ?: return@let
                val res = getImageDimensionsAndMD5(contentResolver, uri)
                val type = res.first?.third ?: "image/jpeg"
                val md5Byte = res.second
                val name = "${UUID.randomUUID().toString().replace("-", "")}.jpg"

                lifecycleScope.launch(Dispatchers.IO) {
                    ossUpload(
                        this@EditProfileActivity, responseData,
                        arrayListOf(uri), arrayListOf(type), arrayListOf(md5Byte),
                        iOnSuccess = { index ->
                            if (index == 0) {
                                val prefix = responseData.uploadPrepareInfo.uploadImagePrefix
                                val fileName = responseData.fileInfo[0].uploadFileName
                                viewModel.onPostAvatarCover("$prefix/$fileName")
                            }
                        },
                        iOnFailure = {
                            closeDialog()
                            makeToast("背景图上传失败")
                        },
                        closeDialog = { closeDialog() }
                    )
                }
            }
        }
    }

    // ---------------- 图片 ----------------

    private fun pickImage(avatar: Boolean) {
        cropForAvatar = avatar
        val launcher = if (avatar) pickAvatarLauncher else pickCoverLauncher
        launcher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    /** 头像裁成 600x600、背景图 1080x1080（官方也是方形） */
    private fun startCrop(uri: Uri, avatar: Boolean) {
        val size = if (avatar) AVATAR_SIZE else COVER_SIZE
        cropLauncher.launch(
            Intent(this, CropImageActivity::class.java)
                .putExtra(CropImageActivity.EXTRA_URI, uri.toString())
                .putExtra(CropImageActivity.EXTRA_OUT_W, size)
                .putExtra(CropImageActivity.EXTRA_OUT_H, size)
        )
    }

    /** 背景图先取 OSS 上传凭证（uploadBucket=avatar & uploadDir=cover） */
    private fun uploadCover(uri: Uri) {
        cropUri = uri
        val res = getImageDimensionsAndMD5(contentResolver, uri)
        val width = res.first?.first ?: COVER_SIZE
        val height = res.first?.second ?: COVER_SIZE
        val md5 = res.second?.toHex() ?: ""
        val name = "${UUID.randomUUID().toString().replace("-", "")}.jpg"

        viewModel.onPostOSSUploadPrepare(
            listOf(ArticleUploadFile(name, "${width}x${height}", md5, 0))
        )
    }

    private var cropUri: Uri? = null

    private fun bindProfile(data: UserProfileResponse.Data) {
        binding.nickname.text = data.username
        showIMG(binding.avatar, data.userAvatar)
        showIMG(binding.coverPreview, data.cover, isCover = false)

        binding.gender.text = when (data.gender) {
            1 -> "男"
            0 -> "女"
            else -> "保密"
        }

        val year = data.birthyear ?: 0
        val month = data.birthmonth ?: 0
        val day = data.birthday ?: 0
        binding.birthday.text =
            if (year > 0 && month > 0 && day > 0) "$year-$month-$day" else "保密"

        val province = data.province.orEmpty()
        val city = data.city.orEmpty()
        binding.city.text = when {
            province.isNotEmpty() && city.isNotEmpty() -> "$province $city"
            city.isNotEmpty() -> city
            province.isNotEmpty() -> province
            else -> "保密"
        }

        // 服务端 bio 可能带 HTML 标签，转纯文本
        binding.bio.text = data.bio?.takeIf { it.isNotBlank() }?.richToString() ?: "未填写"
    }

    // ---------------- 文本类字段 ----------------

    private fun pickGender() {
        val items = arrayOf("男", "女", "保密")
        val checked = when (profile?.gender) {
            1 -> 0
            0 -> 1
            else -> 2
        }
        MaterialAlertDialogBuilder(this)
            .setTitle("性别")
            .setSingleChoiceItems(items, checked) { d, which ->
                d.dismiss()
                viewModel.onPostGender(
                    when (which) {
                        0 -> 1
                        1 -> 0
                        else -> -1
                    }
                )
            }
            .show()
    }

    private fun pickBirthday() {
        MaterialAlertDialogBuilder(this)
            .setTitle("生日")
            .setItems(arrayOf("设置生日", "保密")) { _, which ->
                if (which == 1) {
                    viewModel.onResetBirthday()
                } else {
                    showDatePicker()
                }
            }
            .show()
    }

    private fun showDatePicker() {
        val today = java.util.Calendar.getInstance()
        val year = profile?.birthyear?.takeIf { it > 0 } ?: 2000
        val month = (profile?.birthmonth?.takeIf { it > 0 } ?: 1) - 1
        val day = profile?.birthday?.takeIf { it > 0 } ?: 1
        val dialog = android.app.DatePickerDialog(
            this,
            { _, y, m, d -> viewModel.onPostBirthday(y, m + 1, d) },
            year,
            month,
            day
        )
        dialog.datePicker.maxDate = today.timeInMillis
        dialog.show()
    }

    private fun pickCity() {
        MaterialAlertDialogBuilder(this)
            .setTitle("城市")
            .setItems(arrayOf("选择城市", "保密")) { _, which ->
                if (which == 1) viewModel.onResetCity() else pickProvince()
            }
            .show()
    }

    private fun pickProvince() {
        val provinces = RegionData.provinceList
        MaterialAlertDialogBuilder(this)
            .setTitle("省份")
            .setItems(provinces.toTypedArray()) { _, which ->
                pickCityOf(provinces[which])
            }
            .show()
    }

    private fun pickCityOf(province: String) {
        val cities = RegionData.citiesOf(province)
        if (cities.isEmpty()) {
            viewModel.onPostCity(province, province)
            return
        }
        MaterialAlertDialogBuilder(this)
            .setTitle(province)
            .setItems(cities.toTypedArray()) { _, which ->
                viewModel.onPostCity(province, cities[which])
            }
            .show()
    }

    @SuppressLint("InflateParams")
    private fun editBio() {
        val editText = EditText(this).apply {
            setText(profile?.bio.orEmpty().richToString())
            hint = "介绍一下自己"
            maxLines = 4
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            setSelection(text.length)
        }
        val pad = (16 * resources.displayMetrics.density).toInt()
        val container = FrameLayout(this).apply {
            setPadding(pad, pad / 2, pad, 0)
            addView(editText)
        }
        MaterialAlertDialogBuilder(this)
            .setTitle("个人签名")
            .setView(container)
            .setPositiveButton("保存") { _, _ ->
                viewModel.onPostBio(editText.text.toString().trim())
            }
            .setNegativeButton("取消", null)
            .show()
    }

    // ---------------- 对话框 ----------------

    @SuppressLint("InflateParams")
    private fun showDialog() {
        if (dialog != null) return
        dialog = MaterialAlertDialogBuilder(
            this,
            R.style.ThemeOverlay_MaterialAlertDialog_Rounded
        ).apply {
            setView(
                LayoutInflater.from(this@EditProfileActivity)
                    .inflate(R.layout.dialog_refresh, null, false)
            )
            setCancelable(false)
        }.create()
        dialog?.show()
    }

    private fun closeDialog() {
        dialog?.dismiss()
        dialog = null
    }

    companion object {
        private const val AVATAR_SIZE = 600
        private const val COVER_SIZE = 1080
    }
}
