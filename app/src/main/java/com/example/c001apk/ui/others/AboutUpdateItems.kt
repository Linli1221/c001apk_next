package com.example.c001apk.ui.others

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.drakeet.multitype.ItemViewBinder
import com.example.c001apk.databinding.ItemAboutActionBinding
import com.example.c001apk.databinding.ItemAboutSwitchBinding

/**
 * 「关于本应用」页里插在「开发者」和「反馈」之间的自定义条目，
 * 注册到 about-page 的 MultiTypeAdapter 上使用。
 */

/** 带开关的条目 */
class UpdateSwitchItem(
    val title: String,
    val icon: Int,
    val get: () -> Boolean,
    val set: (Boolean) -> Unit,
)

/** 可点击的条目（立即检查更新） */
class UpdateActionItem(
    val title: String,
    val icon: Int,
    val onClick: () -> Unit,
)

class UpdateSwitchBinder : ItemViewBinder<UpdateSwitchItem, UpdateSwitchBinder.ViewHolder>() {

    class ViewHolder(val binding: ItemAboutSwitchBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(context: Context, parent: ViewGroup): ViewHolder =
        ViewHolder(ItemAboutSwitchBinding.inflate(LayoutInflater.from(context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, item: UpdateSwitchItem) {
        holder.binding.apply {
            icon.setImageResource(item.icon)
            title.text = item.title
            // 先清掉旧的监听再赋值，避免复用时旧 item 的回调误写新值
            switchView.setOnCheckedChangeListener(null)
            switchView.isChecked = item.get()
            switchView.setOnCheckedChangeListener { _, checked -> item.set(checked) }
            root.setOnClickListener { switchView.toggle() }
        }
    }
}

class UpdateActionBinder : ItemViewBinder<UpdateActionItem, UpdateActionBinder.ViewHolder>() {

    class ViewHolder(val binding: ItemAboutActionBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(context: Context, parent: ViewGroup): ViewHolder =
        ViewHolder(ItemAboutActionBinding.inflate(LayoutInflater.from(context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, item: UpdateActionItem) {
        holder.binding.apply {
            icon.setImageResource(item.icon)
            title.text = item.title
            root.setOnClickListener { item.onClick() }
        }
    }
}
