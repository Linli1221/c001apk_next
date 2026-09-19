package com.example.c001apk.ui.article

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.c001apk.R
import com.example.c001apk.databinding.ActivityArticlePublishBinding
import com.example.c001apk.databinding.ItemArticleImageBlockBinding
import com.example.c001apk.databinding.ItemArticleTextBlockBinding
import com.example.c001apk.ui.base.BaseActivity
import com.example.c001apk.util.ImageUtil.getImageDimensionsAndMD5
import com.example.c001apk.util.ImageUtil.toHex
import com.example.c001apk.util.makeToast
import com.example.c001apk.util.ossUpload
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * 发布图文（酷安「图文」= type feed + is_html_article=1）。
 * 结构：标题 + 封面（固定 1600:719 裁剪）+ 正文（文本/图片块混排，图片可加说明）+ 查看权限。
 */
@AndroidEntryPoint
class ArticlePublishActivity : BaseActivity<ActivityArticlePublishBinding>() {

    private val viewModel: ArticlePublishViewModel by viewModels()

    /** 正文块（按顺序即正文混排结构） */
    private val blocks = ArrayList<ArticleBlock>()

    private var coverUri: Uri? = null
    private var coverMd5Byte: ByteArray? = null
    private var coverMd5 = ""
    private var coverName = ""

    private lateinit var pickCoverLauncher: ActivityResultLauncher<PickVisualMediaRequest>
    private lateinit var pickBodyLauncher: ActivityResultLauncher<PickVisualMediaRequest>
    private lateinit var cropLauncher: ActivityResultLauncher<Intent>

    private var dialog: AlertDialog? = null

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
        addTextBlock()
    }

    private fun initView() {
        binding.back.setOnClickListener { finish() }
        binding.coverContainer.setOnClickListener { pickCover() }
        binding.addImage.setOnClickListener { pickBody() }
        binding.addText.setOnClickListener { addTextBlock() }
        binding.publish.setOnClickListener { publish() }
    }

    private fun initLauncher() {
        pickCoverLauncher =
            registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
                if (uri != null) {
                    cropLauncher.launch(
                        Intent(this, CropImageActivity::class.java)
                            .putExtra(CropImageActivity.EXTRA_URI, uri.toString())
                    )
                }
            }
        cropLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_OK) {
                    val uri = result.data?.getStringExtra(CropImageActivity.RESULT_URI)
                        ?.let { Uri.parse(it) }
                    if (uri != null) {
                        coverUri = uri
                        coverName = "${UUID.randomUUID().toString().replace("-", "")}.jpg"
                        val res = getImageDimensionsAndMD5(contentResolver, uri)
                        coverMd5Byte = res.second
                        coverMd5 = res.second?.toHex() ?: ""
                        Glide.with(this).load(uri).into(binding.coverImage)
                        binding.coverImage.isVisible = true
                        binding.coverHint.isVisible = false
                    }
                }
            }
        pickBodyLauncher =
            registerForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(9)) { uris ->
                for (uri in uris) {
                    if (blocks.count { it is ArticleBlock.Image } >= 20) {
                        makeToast("正文最多插入20张图片")
                        break
                    }
                    addImageBlock(uri)
                }
            }
    }

    private fun initObserve() {
        viewModel.toastText.observe(this) { event ->
            event.getContentIfNotHandledOrReturnNull()?.let {
                closeDialog()
                makeToast(it)
            }
        }
        viewModel.over.observe(this) { event ->
            event.getContentIfNotHandledOrReturnNull()?.let {
                closeDialog()
                makeToast("发布成功")
                finish()
            }
        }
        viewModel.uploadImage.observe(this) { event ->
            event.getContentIfNotHandledOrReturnNull()?.let { responseData ->
                val prefix = responseData.uploadPrepareInfo.uploadImagePrefix
                val fileInfo = responseData.fileInfo

                // message JSON 数组：text/image 块交错；fileInfo[0]=封面，fileInfo[1..]=正文图
                var bodyIdx = 0
                val msgList = ArrayList<Map<String, String>>()
                blocks.forEach { block ->
                    when (block) {
                        is ArticleBlock.Text ->
                            msgList.add(mapOf("type" to "text", "message" to block.text))
                        is ArticleBlock.Image -> {
                            val url = prefix + "/" + fileInfo[1 + bodyIdx].uploadFileName
                            msgList.add(
                                mapOf(
                                    "type" to "image",
                                    "url" to url,
                                    "description" to block.description
                                )
                            )
                            bodyIdx++
                        }
                    }
                }
                viewModel.feedData["message"] = Gson().toJson(msgList)
                viewModel.feedData["message_cover"] = prefix + "/" + fileInfo[0].uploadFileName

                // 上传列表与 uploadFileList 顺序一致：封面 + 正文图
                val uriList = ArrayList<Uri>().apply {
                    add(coverUri!!)
                    blocks.filterIsInstance<ArticleBlock.Image>().forEach { add(it.uri) }
                }
                val typeList = ArrayList<String>().apply {
                    add("image/jpeg")
                    blocks.filterIsInstance<ArticleBlock.Image>().forEach { add(it.type) }
                }
                val md5List = ArrayList<ByteArray?>().apply {
                    add(coverMd5Byte)
                    blocks.filterIsInstance<ArticleBlock.Image>().forEach { add(it.md5Byte) }
                }

                lifecycleScope.launch(Dispatchers.IO) {
                    ossUpload(
                        this@ArticlePublishActivity, responseData, uriList, typeList, md5List,
                        iOnSuccess = { index ->
                            if (index == uriList.lastIndex) {
                                viewModel.onPostCreateFeed()
                            }
                        },
                        iOnFailure = {
                            closeDialog()
                            makeToast("图片上传失败")
                        },
                        closeDialog = { closeDialog() }
                    )
                }
            }
        }
    }

    private fun pickCover() {
        pickCoverLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    private fun pickBody() {
        pickBodyLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    private fun addTextBlock() {
        val block = ArticleBlock.Text()
        blocks.add(block)
        val item = ItemArticleTextBlockBinding.inflate(layoutInflater, binding.blockContainer, false)
        item.blockText.setText(block.text)
        item.blockText.addTextChangedListener { block.text = it?.toString() ?: "" }
        item.blockDelete.setOnClickListener {
            binding.blockContainer.removeView(item.root)
            blocks.remove(block)
        }
        binding.blockContainer.addView(item.root)
    }

    private fun addImageBlock(uri: Uri) {
        val res = getImageDimensionsAndMD5(contentResolver, uri)
        val md5Byte = res.second
        val width = res.first?.first ?: 0
        val height = res.first?.second ?: 0
        val type = res.first?.third ?: "image/jpeg"
        val ext = if (type.startsWith("image/")) type.substringAfterLast("/") else "jpg"

        val block = ArticleBlock.Image(
            uri = uri,
            name = "${UUID.randomUUID().toString().replace("-", "")}.$ext",
            resolution = "${width}x${height}",
            md5 = md5Byte?.toHex() ?: "",
            type = type,
            md5Byte = md5Byte,
        )
        blocks.add(block)
        val item = ItemArticleImageBlockBinding.inflate(layoutInflater, binding.blockContainer, false)
        Glide.with(this).load(uri).into(item.blockImage)
        item.blockDescription.addTextChangedListener { block.description = it?.toString() ?: "" }
        item.blockDelete.setOnClickListener {
            binding.blockContainer.removeView(item.root)
            blocks.remove(block)
        }
        binding.blockContainer.addView(item.root)
    }

    private fun publish() {
        val title = binding.articleTitle.text.toString().trim()
        if (title.isEmpty()) {
            makeToast("请输入标题")
            return
        }
        if (coverUri == null) {
            makeToast("请选择封面图")
            return
        }
        val hasContent = blocks.any {
            when (it) {
                is ArticleBlock.Text -> it.text.isNotBlank()
                is ArticleBlock.Image -> true
            }
        }
        if (!hasContent) {
            makeToast("请输入正文")
            return
        }

        // uploadFileList：封面 + 正文图
        val uploadFiles = ArrayList<ArticleUploadFile>()
        uploadFiles.add(ArticleUploadFile(coverName, "1600x719", coverMd5, 0))
        blocks.filterIsInstance<ArticleBlock.Image>().forEach {
            uploadFiles.add(ArticleUploadFile(it.name, it.resolution, it.md5, 0))
        }

        viewModel.feedData.apply {
            put("id", "")
            put("type", "feed")
            put("status", "1")
            put("publish_status", if (binding.checkBox.isChecked) "1" else "0")
            put("message_title", title)
            put("is_html_article", "1")
            put("pic", "")
        }

        viewModel.onPostOSSUploadPrepare(uploadFiles)
        showDialog()
    }

    @SuppressLint("InflateParams")
    private fun showDialog() {
        dialog = MaterialAlertDialogBuilder(
            this,
            R.style.ThemeOverlay_MaterialAlertDialog_Rounded
        ).apply {
            setView(
                LayoutInflater.from(this@ArticlePublishActivity)
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
}
