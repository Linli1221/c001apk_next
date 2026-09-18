package com.example.c001apk.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.example.c001apk.R
import com.example.c001apk.databinding.ItemProductSortHeaderBinding

/**
 * 产品页讨论 tab 顶部的「默认 / 最新 / 热度」三段式排序控件（对齐官方 UI）。
 * [onSort] 仅在用户点击时回调，恢复选中态不会触发。
 */
class ProductSortHeaderAdapter(
    private val currentSort: String,
    private val onSort: (String) -> Unit
) : RecyclerView.Adapter<ProductSortHeaderAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemProductSortHeaderBinding) :
        RecyclerView.ViewHolder(binding.root)

    private var suppressCallback = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(
            ItemProductSortHeaderBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )

    override fun getItemCount(): Int = 1

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        (holder.itemView.layoutParams as? StaggeredGridLayoutManager.LayoutParams)
            ?.isFullSpan = true

        val checkedId = when (currentSort) {
            "最新" -> R.id.btnSortLatest
            "热度" -> R.id.btnSortHot
            else -> R.id.btnSortDefault
        }
        with(holder.binding) {
            suppressCallback = true
            sortGroup.check(checkedId)
            suppressCallback = false
            sortGroup.clearOnButtonCheckedListeners()
            sortGroup.addOnButtonCheckedListener { _, id, isChecked ->
                if (isChecked && !suppressCallback) {
                    onSort(
                        when (id) {
                            R.id.btnSortLatest -> "最新"
                            R.id.btnSortHot -> "热度"
                            else -> "默认"
                        }
                    )
                }
            }
        }
    }

}
