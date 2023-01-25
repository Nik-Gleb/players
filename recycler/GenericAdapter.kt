package com.esn.platform.recycler

import android.os.Handler
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.esn.platform.stdlib.executor
import java.util.concurrent.Executor

/**
 * Базовый адаптер для RecyclerView.
 *
 * @param delegate адаптер-делегат
 * @param diff стратегия вычисления различий
 * @param handler поток доставки нотификаций для адаптера
 * @param calc поток для рассчётов различий списков
 * @param items начальный список данных
 */
class GenericAdapter internal constructor(
    private val delegate: AdapterDelegate<Any, ViewHolder>,
    diff: DiffUtil.ItemCallback<Any>,
    handler: Handler,
    calc: Executor,
    items: List<*>,
    private val endless: Boolean = true
) : RecyclerView.Adapter<ViewHolder>() {

    private var differ by createListDiffer(diff, handler.executor(), calc, items.requireNoNulls())

    override fun getItemCount() = if (endless) Int.MAX_VALUE else differ.size

    override fun getItemId(position: Int) =
        delegate.getIdByItem(getItem(position))

    override fun getItemViewType(position: Int) =
        delegate.getTypeByItem(getItem(position))

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        delegate.createViewHolderByType(parent, viewType)

    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: List<Any>) =
        delegate.bindViewHolder(holder, payloads, getItem(position))

    override fun onViewRecycled(holder: ViewHolder) =
        delegate.unbindViewHolder(holder)

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = errorNotSupportedThere()

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) =
        delegate.attachRecyclerView(recyclerView)

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) =
        delegate.detachRecyclerView(recyclerView)

    private fun getItem(position: Int): Any =
        differ[if (endless) position % differ.size else position]


    internal companion object {

        internal fun RecyclerView.Adapter<ViewHolder>.items(list: List<*>): RecyclerView.Adapter<ViewHolder> {
            (this as? GenericAdapter)?.differ = list.requireNoNulls()
            return this
        }

        /**
         * Задаёт элементы в адаптер если он - Generic.
         *
         * @receiver целевой RecyclerView
         */
        internal fun GenericAdapter?.content(list: List<*>) {
            this?.differ = list.requireNoNulls()
        }
    }
}
