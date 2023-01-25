package com.esn.platform.recycler

import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager

internal fun errorNotSupportedThere(): Nothing = error("Не поддерживается в этой версии адаптера")

@Suppress("unused")
private fun RecyclerView.staggered() = layoutManager as? StaggeredGridLayoutManager
private fun RecyclerView.linear() = layoutManager as? LinearLayoutManager
private fun RecyclerView.grid() = layoutManager as? GridLayoutManager

fun RecyclerView.firstVisibleAdapterPositionProvider(): () -> Int = {
    (linear() ?: grid())?.findFirstVisibleItemPosition() ?: RecyclerView.NO_POSITION
}
