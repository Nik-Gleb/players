package com.esn.platform.recycler

import androidx.paging.PagingData

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

internal class PagingDataSubmitter<T : Any>(private val submit: suspend (PagingData<T>) -> Unit) {

    internal var scope: CoroutineScope? = null
        set(value) {
            if (field != value) {
                field?.cancel()
                field = value
                invalidateState()
            }
        }

    internal var data: PagingData<T>? = null
        set(value) {
            if (field != value) {
                field = value
                invalidateState()
            }
        }

    private fun invalidateState() {
        val scope = this.scope
        val data = this.data
        if (scope == null || data == null) return
        scope.launch { submit(data) }
    }
}
