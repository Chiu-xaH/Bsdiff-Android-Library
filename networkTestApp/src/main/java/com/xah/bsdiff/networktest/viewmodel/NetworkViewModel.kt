package com.xah.bsdiff.networktest.viewmodel

import androidx.lifecycle.ViewModel
import com.xah.bsdiff.networktest.logic.model.UpgradeLinkBean
import com.xah.bsdiff.networktest.logic.model.UpgradeLinkResponseBody
import com.xah.bsdiff.networktest.logic.network.repo.UpgradeLinkRepository
import com.xah.bsdiff.networktest.ui.util.StateHolder

class NetworkViewModel : ViewModel() {
    val loginResult = StateHolder<UpgradeLinkResponseBody>()
    suspend fun getUpgrade() = UpgradeLinkRepository.getUpgrade(loginResult)
}