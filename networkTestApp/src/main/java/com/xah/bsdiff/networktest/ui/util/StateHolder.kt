package com.xah.bsdiff.networktest.ui.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow


const val PARSE_ERROR_CODE = 1000
const val LISTEN_ERROR_CODE = 1001
const val TIMEOUT_ERROR_CODE = 1002
const val CONNECTION_ERROR_CODE = 1003
const val UNKNOWN_ERROR_CODE = 1004
const val OPERATION_FAST_ERROR_CODE = 1005

private interface IStateHolder<in T> {
    fun emitData(data : T)
    fun emitError(e: Throwable?, code: Int? = null)
    fun clear()
    fun setLoading()
}

class StateHolder<T> : IStateHolder<T> {
    private val _state = MutableStateFlow<UiState<T>>(UiState.Prepare)
    val state: StateFlow<UiState<T>> get() = _state

    override fun emitData(data: T) {
        _state.value = UiState.Success(data)
    }

    override fun emitError(e: Throwable?, code: Int?) {
        _state.value = UiState.Error(e, code)
    }


    override fun setLoading() {
        _state.value = UiState.Loading
    }

    override fun clear() {
        _state.value = UiState.Loading
    }

    fun emitPrepare() {
        _state.value = UiState.Prepare
    }
}
