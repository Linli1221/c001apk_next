package com.example.c001apk.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.c001apk.databinding.ItemProductSelectBinding
import com.example.c001apk.logic.model.HomeFeedResponse
import com.example.c001apk.util.ImageUtil
import com.example.c001apk.util.dp

/**
 * 产品页「参数」tab 里同价位 / 同SoC / 同系列卡片（listCard）的横向机型列表。
 */
class ProductSelectAdapter(
    private val listener: ItemListener
) : ListAdapter<HomeFeedResponse.Entities, ProductSelectAdapter.ViewHolder>(
    ProductSelectDiffCallback()
) {

    class ViewHolder(
        val binding: ItemProductSelectBinding,
        val listener: ItemListener
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(data: HomeFeedResponse.Entities) {
            ImageUtil.showIMG(binding.logo, data.logo)
            binding.title.text = data.title
            binding.score.text = data.starAverageScore?.let { "${it}分" }.orEmpty()
            binding.ratingSpecs.text = data.productRatingSpecs
                ?.entries?.joinToString(" ") { "${it.key}${it.value}" }
                .orEmpty()
            binding.specs.text = data.productSpecs?.joinToString(" | ").orEmpty()
            binding.price.text = listOfNotNull(
                data.priceMin?.let { "¥$it" },
                data.configName
            ).joinToString(" ")
            binding.root.setOnClickListener {
                listener.onOpenLink(it, data.url, data.title)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemProductSelectBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        binding.root.layoutParams.width = 104.dp
        return ViewHolder(binding, listener)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(currentList[position])
    }

}

class ProductSelectDiffCallback : DiffUtil.ItemCallback<HomeFeedResponse.Entities>() {
    override fun areItemsTheSame(
        oldItem: HomeFeedResponse.Entities,
        newItem: HomeFeedResponse.Entities
    ): Boolean = oldItem.url == newItem.url

    override fun areContentsTheSame(
        oldItem: HomeFeedResponse.Entities,
        newItem: HomeFeedResponse.Entities
    ): Boolean = oldItem.url == newItem.url
}
