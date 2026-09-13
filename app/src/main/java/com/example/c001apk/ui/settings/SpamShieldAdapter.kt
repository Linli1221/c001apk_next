package com.example.c001apk.ui.settings

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.c001apk.databinding.ItemSpamItemBinding
import com.example.c001apk.util.ImageUtil

class SpamShieldAdapter(
    private val onDeleteClick: (SpamShieldItem) -> Unit
) : ListAdapter<SpamShieldItem, SpamShieldAdapter.ViewHolder>(DiffCallback) {

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<SpamShieldItem>() {
            override fun areItemsTheSame(oldItem: SpamShieldItem, newItem: SpamShieldItem) =
                oldItem.key == newItem.key

            override fun areContentsTheSame(oldItem: SpamShieldItem, newItem: SpamShieldItem) =
                oldItem == newItem
        }
    }

    class ViewHolder(val binding: ItemSpamItemBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(ItemSpamItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        with(holder.binding) {
            title.text = item.title
            desc.isVisible = !item.desc.isNullOrEmpty()
            desc.text = item.desc.orEmpty()
            logo.isVisible = !item.logo.isNullOrEmpty()
            if (!item.logo.isNullOrEmpty())
                ImageUtil.showIMG(logo, item.logo)
            delete.setOnClickListener { onDeleteClick(item) }
        }
    }
}
