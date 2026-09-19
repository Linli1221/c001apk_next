package com.example.c001apk.ui.topic

import android.os.Bundle
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.ConcatAdapter
import com.example.c001apk.adapter.FooterState
import com.example.c001apk.adapter.LoadingState
import com.example.c001apk.adapter.ProductSortHeaderAdapter
import com.example.c001apk.logic.model.HomeFeedResponse
import com.example.c001apk.ui.base.BaseAppFragment
import dagger.hilt.android.AndroidEntryPoint
import java.net.URLDecoder
import javax.inject.Inject

@AndroidEntryPoint
class TopicContentFragment : BaseAppFragment<TopicContentViewModel>() {

    @Inject
    lateinit var viewModelAssistedFactory: TopicContentViewModel.Factory
    override val viewModel by viewModels<TopicContentViewModel> {
        TopicContentViewModel.provideFactory(
            viewModelAssistedFactory,
            arguments?.getString("url").orEmpty(),
            arguments?.getString("title").orEmpty(),
        )
    }

    companion object {
        @JvmStatic
        fun newInstance(
            url: String,
            title: String,
            configRows: ArrayList<HomeFeedResponse.ConfigRow>? = null,
        ) =
            TopicContentFragment().apply {
                arguments = Bundle().apply {
                    putString("url", url)
                    putString("title", title)
                    putParcelableArrayList("configRows", configRows)
                }
            }
    }

    override fun initObserve() {
        super.initObserve()

        viewModel.toastText.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandledOrReturnNull()?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onViewCreated(view: android.view.View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // 产品页「参数」tab：接收 product/detail 下发的版本配置列表
        viewModel.configRows = arguments?.getParcelableArrayList("configRows")
    }

    override fun initAdapter() {
        super.initAdapter()
        // 「默认/最新/热度」三段式排序（对齐官方 UI），覆盖两类栏目：
        // 1) 产品讨论 tab：product/feedList + type=feed（排序参数 listType）
        // 2) 话题讨论 tab：topic/tagFeedList + withSortCard=1（服务端下发排序卡片，排序参数 sortField）
        // 注意：product/detail 下发的 tab url 是 URL 编码的（如 %2Fproduct%2FfeedList、type%3Dfeed），
        // 必须先解码再判断，否则 contains 匹配不到，排序开关就不显示。
        val decodedUrl = URLDecoder.decode(viewModel.url, "UTF-8")
        val isProductFeed = decodedUrl.contains("product/feedList") && decodedUrl.contains("type=feed")
        val isTopicFeed = decodedUrl.contains("topic/tagFeedList") && decodedUrl.contains("withSortCard=1")
        if (isProductFeed || isTopicFeed) {
            val sortAdapter = ProductSortHeaderAdapter(viewModel.currentSort) { label ->
                onSortChanged(label)
            }
            mAdapter = ConcatAdapter(sortAdapter, mAdapter)
        }
    }

    private fun onSortChanged(label: String) {
        val decodedUrl = URLDecoder.decode(viewModel.url, "UTF-8")
        viewModel.currentSort = label
        viewModel.title = label
        viewModel.url = if (decodedUrl.contains("topic/tagFeedList")) {
            // 话题讨论 tab：排序走 sortField 参数（lastupdate_desc / dateline_desc / rank_score）
            val sortValue = when (label) {
                "最新" -> "dateline_desc"
                "热度" -> "rank_score"
                else -> "lastupdate_desc"
            }
            if (decodedUrl.contains("sortField="))
                decodedUrl.replace(Regex("sortField=[^&]*"), "sortField=$sortValue")
            else
                "$decodedUrl&sortField=$sortValue"
        } else {
            // 产品讨论 tab：排序走 listType 参数
            val id = Regex("id=(\\d+)").find(decodedUrl)?.groupValues?.get(1) ?: return
            when (label) {
                "最新" ->
                    "/page?url=/product/feedList?cacheExpires=60&type=feed&ignoreEntityById=1&listType=dateline_desc&id=$id"

                "热度" ->
                    "/page?url=/product/feedList?cacheExpires=60&type=feed&listType=rank_score&id=$id"

                else ->
                    "/page?url=/product/feedList?cacheExpires=60&type=feed&ignoreEntityById=1&id=$id"
            }
        }
        viewModel.dataList.value = emptyList()
        viewModel.footerState.value = FooterState.LoadingDone
        viewModel.loadingState.value = LoadingState.Loading
    }

}
