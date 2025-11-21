package com.xah.bsdiff.networktest.viewmodel

import androidx.lifecycle.ViewModel
import com.xah.bsdiff.networktest.logic.network.repo.UpgradeLinkRepository
import com.xah.bsdiff.networktest.logic.util.UpgradeLinkBean
import com.xah.bsdiff.networktest.ui.util.StateHolder

class NetworkViewModel : ViewModel() {
    val loginResult = StateHolder<UpgradeLinkBean>()
    suspend fun getUpgrade() = UpgradeLinkRepository.getUpgrade(loginResult)
}