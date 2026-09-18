package com.example.c001apk.ui.collection

import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.c001apk.R
import com.example.c001apk.databinding.DialogCollectionEditBinding
import com.example.c001apk.logic.model.CollectionData
import com.example.c001apk.ui.base.BasePagerFragment
import com.example.c001apk.ui.feed.CollectionPickViewModel
import com.example.c001apk.util.ImageUtil
import com.example.c001apk.util.makeToast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CollectionFragment : BasePagerFragment() {

    private val id by lazy { arguments?.getString("id") }
    private val title by lazy { arguments?.getString("title").orEmpty() }

    private val viewModel by viewModels<CollectionPickViewModel>()

    private var editBinding: DialogCollectionEditBinding? = null
    private var editCoverUri: Uri? = null

    private val pickCover = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri == null) return@registerForActivityResult
        editCoverUri = uri
        editBinding?.cover?.setImageURI(uri)
    }

    companion object {
        @JvmStatic
        fun newInstance(id: String?, title: String) =
            CollectionFragment().apply {
                arguments = Bundle().apply {
                    putString("id", id)
                    putString("title", title)
                }
            }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initBar()
        initEditEntry()
    }

    override fun getFragment(position: Int): Fragment =
        CollectionContentFragment.newInstance(
            if (id.isNullOrEmpty()) "/v6/collection/list"
            else "/v6/collection/itemList",
            id
        )


    override fun initTabList() {
        binding.tabLayout.isVisible = false
        tabList = listOf("")
    }

    override fun initBar() {
        super.initBar()
        binding.collapsingToolbar.isTitleEnabled = false
        binding.toolBar.title = title.ifEmpty { "我的收藏单" }
    }

    override fun onBackClick() {
        if (id.isNullOrEmpty())
            activity?.finish()
        else
            activity?.supportFragmentManager?.popBackStack()
    }

    // ---------------- 收藏夹编辑入口（右上角，仅具体收藏夹页显示） ----------------

    private fun initEditEntry() {
        if (id.isNullOrEmpty()) return
        val colId = id ?: return
        binding.toolBar.inflateMenu(R.menu.collection_menu)
        binding.toolBar.setOnMenuItemClickListener { item ->
            if (item.itemId == R.id.editCollection) {
                showEditActions(colId)
                true
            } else false
        }
        viewModel.toastText.observe(viewLifecycleOwner) { event ->
            event?.getContentIfNotHandledOrReturnNull()?.let { requireContext().makeToast(it) }
        }
        viewModel.detail.observe(viewLifecycleOwner) { showEditDialog(it) }
        viewModel.deleted.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandledOrReturnNull()?.let {
                activity?.supportFragmentManager?.popBackStack()
            }
        }
    }

    private fun showEditActions(colId: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(title.ifEmpty { "编辑收藏夹" })
            .setItems(arrayOf("编辑收藏夹信息", "清除无效内容", "删除收藏夹")) { _, which ->
                when (which) {
                    0 -> viewModel.loadDetail(colId)

                    1 -> MaterialAlertDialogBuilder(requireContext())
                        .setTitle("清除无效内容")
                        .setMessage("将清除该收藏夹内已失效的内容，约 5 分钟后生效，确定继续？")
                        .setNegativeButton(android.R.string.cancel, null)
                        .setPositiveButton(android.R.string.ok) { _, _ ->
                            viewModel.clearUnUse(colId)
                        }
                        .show()

                    2 -> MaterialAlertDialogBuilder(requireContext())
                        .setTitle("删除收藏夹")
                        .setMessage("删除后不可恢复，确定删除「$title」？")
                        .setNegativeButton(android.R.string.cancel, null)
                        .setPositiveButton(android.R.string.ok) { _, _ ->
                            viewModel.delete(colId)
                        }
                        .show()
                }
            }
            .show()
    }

    /** 编辑收藏夹信息：标题 / 简介 / 公开-私密 / 封面（复用收藏弹窗的编辑对话框） */
    private fun showEditDialog(item: CollectionData) {
        editCoverUri = null
        val editBinding = DialogCollectionEditBinding.inflate(layoutInflater)
        this.editBinding = editBinding
        editBinding.cover.setOnClickListener {
            pickCover.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }
        editBinding.title.setText(item.title.orEmpty())
        editBinding.description.setText(item.description.orEmpty())
        editBinding.isOpen.isChecked = item.is_open == 1
        if (!item.cover_pic.isNullOrEmpty())
            ImageUtil.showIMG(editBinding.cover, item.cover_pic)
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.collection_edit)
            .setView(editBinding.root)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val newTitle = editBinding.title.text?.toString()?.trim().orEmpty()
                if (newTitle.isEmpty()) {
                    requireContext().makeToast("请填写收藏夹名称")
                    return@setPositiveButton
                }
                val description = editBinding.description.text?.toString()?.trim().orEmpty()
                val isOpen = if (editBinding.isOpen.isChecked) 1 else 0
                viewModel.update(
                    id.orEmpty(), newTitle, description, isOpen, editCoverUri,
                    item.cover_pic, requireContext().contentResolver, null
                )
            }
            .show()
    }
}
