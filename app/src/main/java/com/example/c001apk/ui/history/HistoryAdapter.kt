package com.example.c001apk.ui.history

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.c001apk.databinding.ItemHistoryCloudBinding
import com.example.c001apk.logic.model.HitHistoryData
import com.example.c001apk.util.DateUtils
import com.example.c001apk.util.ImageUtil

/** 云端浏览历史条目：logo + 标题 + 描述 + 类型/时间 */
class HistoryAdapter(
    private val onClick: (HitHistoryData) -> Unit,
) : ListAdapter<HitHistoryData, HistoryAdapter.ViewHolder>(DiffCallback) {

    class ViewHolder(val binding: ItemHistoryCloudBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(
            ItemHistoryCloudBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        with(holder.binding) {
            title.text = item.title.orEmpty()
            desc.text = item.description.orEmpty()
            desc.isVisible = !item.description.isNullOrEmpty()
            info.text = buildString {
                if (!item.typeName.isNullOrEmpty()) append(item.typeName)
                item.dateline?.let {
                    if (isNotEmpty()) append(" · ")
                    append(DateUtils.fromToday(it))
                }
            }
            if (item.logo.isNullOrEmpty()) {
                logo.isVisible = false
            } else {
                logo.isVisible = true
                ImageUtil.showIMG(logo, item.logo)
            }
            root.setOnClickListener { onClick(item) }
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<HitHistoryData>() {
            override fun areItemsTheSame(oldItem: HitHistoryData, newItem: HitHistoryData) =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: HitHistoryData, newItem: HitHistoryData) =
                oldItem == newItem
        }
    }
}
