package com.example.c001apk.ui.user

import android.os.Bundle
import androidx.fragment.app.viewModels
import com.example.c001apk.ui.base.BaseAppFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/** 个人主页里的单个 tab 列表 */
@AndroidEntryPoint
class UserTabFragment : BaseAppFragment<UserTabViewModel>() {

    @Inject
    lateinit var viewModelAssistedFactory: UserTabViewModel.Factory

    override val viewModel by viewModels<UserTabViewModel> {
        UserTabViewModel.provideFactory(
            viewModelAssistedFactory,
            requireArguments().getString("uid").orEmpty(),
            requireArguments().getString("type").orEmpty()
        )
    }

    companion object {
        @JvmStatic
        fun newInstance(uid: String, type: String): UserTabFragment =
            UserTabFragment().apply {
                arguments = Bundle().apply {
                    putString("uid", uid)
                    putString("type", type)
                }
            }
    }
}
