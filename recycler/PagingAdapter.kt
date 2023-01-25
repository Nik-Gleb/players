package com.esn.platform.recycler

import android.os.Handler
import android.view.ViewGroup
import androidx.annotation.IntRange
import androidx.paging.PagingData
import androidx.paging.PagingDataAdapter
import androidx.paging.map
import androidx.recyclerview.widget.DiffUtil.ItemCallback
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.esn.platform.stdlib.SchedulersX
import com.esn.platform.xcore.createCoroutineScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import java.util.concurrent.Executor

/**
 * Пагинационный адаптер для RecyclerView.
 *
 * @param delegate адаптер-делегат
 * @param diff стратегия вычисления различий
 * @param handler поток доставки нотификаций для адаптера
 * @param calc потоко для рассчётов различий списков
 * @param data начальный список данных
 */
class PagingAdapter internal constructor(
    private val delegate: AdapterDelegate<Any, ViewHolder>,
    diff: ItemCallback<Any>,
    handler: Handler,
    calc: Executor,
    data: PagingData<*>
) : PagingDataAdapter<Any, ViewHolder>(diff, handler.dispatcher(), calc.dispatcher()) {

    private val submitter = PagingDataSubmitter(this::submitData).data(data)

    override fun getItemViewType(position: Int) =
        delegate.getTypeByItem(getItemSafe(position))

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        delegate.createViewHolderByType(parent, viewType)

    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: List<Any>) =
        delegate.bindViewHolder(holder, payloads, peekItemSafe(position))

    override fun onViewRecycled(holder: ViewHolder) =
        delegate.unbindViewHolder(holder)

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = errorNotSupportedThere()

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        delegate.attachRecyclerView(recyclerView)
        submitter.scope = recyclerView.createCoroutineScope()
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        submitter.scope?.cancel()
        submitter.scope = null
        delegate.detachRecyclerView(recyclerView)
    }

    private fun peekItemSafe(@IntRange(from = 0) index: Int) =
        peek(index) ?: errorNotSupportedThere()

    private fun getItemSafe(@IntRange(from = 0) index: Int) =
        getItem(index) ?: errorNotSupportedThere()

    companion object {

        internal fun RecyclerView.Adapter<ViewHolder>.data(
            value: PagingData<*>
        ): RecyclerView.Adapter<ViewHolder> {
            (this as? PagingAdapter)?.submitter?.data(value)
            return this
        }

        private fun PagingDataSubmitter<Any>.data(data: PagingData<*>) = apply {
            this.data = data.map(SchedulersX.DIRECT) { it }
        }

        /**
         * @receiver экземпляр [Handler]
         *
         * @return соответствующий [CoroutineDispatcher]
         */
        private fun Handler.dispatcher() = asCoroutineDispatcher()

        /**
         * @receiver экземпляр [Executor]
         *
         * @return соответствующий [CoroutineDispatcher]
         */
        private fun Executor.dispatcher() = asCoroutineDispatcher()
    }
}
