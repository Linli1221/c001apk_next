package com.example.c001apk.util

import android.content.Context
import com.example.c001apk.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * 「修改可见性」选择弹窗。
 *
 * @param publishStatus 当前可见性：1 = 仅自己可见，0 = 公开，null = 未知（不标「当前」）
 * @param onSelect 回传选中的可见性（1 = 仅自己可见，0 = 公开）
 */
fun showPublishStatusDialog(
    context: Context,
    publishStatus: Int?,
    onSelect: (Int) -> Unit
) {
    val items = arrayOf(
        context.getString(
            if (publishStatus == 0) R.string.publish_status_public_current
            else R.string.publish_status_public
        ),
        context.getString(
            if (publishStatus == 1) R.string.publish_status_private_current
            else R.string.publish_status_private
        )
    )
    MaterialAlertDialogBuilder(context)
        .setTitle(R.string.change_publish_status)
        .setItems(items) { _, position -> onSelect(position) }
        .show()
}
