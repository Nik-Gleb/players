package com.esn.platform.recycler

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.View
import androidx.annotation.AttrRes
import androidx.annotation.StyleRes
import androidx.paging.PagingData
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SnapHelper
import com.esn.platform.recycler.GenericAdapter.Companion.items
import com.esn.platform.recycler.PagingAdapter.Companion.data
import com.esn.platform.stdlib.SchedulersX
import com.esn.platform.xcore.createCoroutineScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlin.reflect.KClass

@SuppressLint("CustomViewStyleable", "PrivateResource")
@Suppress("LeakingThis")
open class ESNRecyclerView
@JvmOverloads
constructor(
    context: Context,
    attr: AttributeSet? = null,
    @SuppressLint("PrivateResource")
    @AttrRes
    attrs: Int = 0,
    @StyleRes style: Int = 0
) : RecyclerView(context, attr, attrs) {

    private var scope: CoroutineScope? = null
        set(value) {
            if (field != value) {
                field?.cancel()
                field = value
                invalidateScope()
            }
        }

    private var pages: Flow<PagingData<*>>? = null
        set(value) {
            if (field != value) {
                field = value
                invalidateScope()
            }
        }

    var onRefreshed: (() -> Unit)? = null
        set(value) {
            if (field != value) {
                field?.let(this::unregisterOnRefreshed)
                field = value
                field?.let(this::registerOnRefreshed)
            }
        }

    var onAdapterChanged: ((Adapter<*>?) -> Unit)? = null

    private var paging: Adapter<ViewHolder>? = null
        set(value) {
            if (field != value) {
                onRefreshed?.let(this::unregisterOnRefreshed)
                field = value
                onRefreshed?.let(this::registerOnRefreshed)
            }
        }

    private val numOfVisibleItems: Int

    private var generic: Adapter<ViewHolder>? = null

    private val snapHelper: SnapHelper?

    private val adapters: (Any) -> Adapter<ViewHolder>

    init {
        val esnArray = getContext().obtainStyledAttributes(attr, R.styleable.ESNRecyclerView)
        val options = esnArray.getInteger(R.styleable.ESNRecyclerView_options, OPTION_NONE)
        val viewCacheSize = esnArray.getInteger(R.styleable.ESNRecyclerView_cacheSize, 0)
        val snap = esnArray.getInteger(R.styleable.ESNRecyclerView_snapping, -1)
        val space = esnArray.getInteger(R.styleable.ESNRecyclerView_extraSpace, 0)
        numOfVisibleItems = esnArray.getInteger(R.styleable.ESNRecyclerView_visibleItems, 1)
        esnArray.recycle()

        setItemViewCacheSize(viewCacheSize)
        setHasFixedSize(!options.has(OPTION_NO_FIXED))
        val prefetch = options.has(OPTION_PREFETCH)
        val isPaging = options.has(OPTION_PAGING)
        val endless = options.has(OPTION_ENDLESS)
        val predictive = options.has(OPTION_PREDICTIVE)

        adapters = createRecyclerAdaptersFactory(ADAPTER_DELEGATE, REMOVE_AND_RECYCLE, endless)

        val styleable = androidx.recyclerview.R.styleable.RecyclerView
        val span = androidx.recyclerview.R.styleable.RecyclerView_spanCount
        val rvArray = getContext().obtainStyledAttributes(attr, styleable)
        val spanCount = rvArray.getInteger(span, 1)
        rvArray.recycle()

        snapHelper = when (snap) {
            0 -> PagerSnapHelper(); 1 -> LinearSnapHelper(); else -> null
        }

        val lm = if (spanCount > 1) {
            GridManagerLayout(context, attr, attrs, style, space, predictive)
        } else LinearManagerLayout(context, attr, attrs, style, space, predictive)

        lm.isItemPrefetchEnabled = prefetch

        layoutManager = lm

        if (isPaging) paging = adapters(PagingData.empty<Any>())

        if (endless) scrollToPosition(Int.MAX_VALUE / 2)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        snapHelper?.attachToRecyclerView(this)
        scope = createCoroutineScope()
    }

    override fun onDetachedFromWindow() {
        snapHelper?.attachToRecyclerView(null)
        scope?.cancel()
        scope = null
        super.onDetachedFromWindow()
    }

    //    val loadStateFlow: Flow<CombinedLoadStates>?
//        get() {
//            return (adapter as? PagingDataAdapter<*, *>)?.loadStateFlow
//        }
    /**
     * бесполезно. используется фабрика адаптеров и не предсказуемо когда адаптер будет создан
     * и/или будет заменен на новый адаптер
     */
    val onPagesUpdatedFlow: Flow<Unit>?
        get() {
            return (adapter as? PagingDataAdapter<*, *>)?.onPagesUpdatedFlow
        }

    fun map(vararg types: Pair<KClass<out Any>, KClass<out View>>) = ADAPTER_DELEGATE.map(*types)

    fun items(vararg items: Any) = items(items.toList())

    fun items(value: List<*>) {
        if (paging == null) generic = generic?.items(value) ?: adapters(value)
        else pages(PagingData.from(value.requireNoNulls()))
    }

    private fun pages(value: PagingData<*>) {
        paging = paging?.data(value) ?: adapters(value)
    }

    private fun registerOnRefreshed(onRefreshed: () -> Unit) {
        (paging as? PagingAdapter)?.addOnPagesUpdatedListener(onRefreshed)
    }

    private fun unregisterOnRefreshed(onRefreshed: () -> Unit) {
        (paging as? PagingAdapter)?.removeOnPagesUpdatedListener(onRefreshed)
    }

    fun reset() {
        generic = null
        paging?.data(PagingData.empty<Any>())
        paging = null
    }

    fun <Key : Any, Value : Any> pager(call: () -> PagingCall<Key, Value>) {
        val pagingView = PagingView(numOfVisibleItems, firstVisibleAdapterPositionProvider())
        pages = pager(call, pagingView, null)
    }

    private fun invalidateScope() {
        pages?.apply {
            scope?.launch {
                collect { pages(it) }
            }
        }
    }

    override fun setAdapter(adapter: Adapter<*>?) {
        super.setAdapter(adapter)
        onAdapterChanged?.invoke(adapter)
    }

    override fun swapAdapter(adapter: Adapter<*>?, removeAndRecycleExistingViews: Boolean) {
        super.swapAdapter(adapter, removeAndRecycleExistingViews)
        onAdapterChanged?.invoke(adapter)
    }

    fun refresh() = (adapter as? PagingAdapter)?.refresh()

    companion object {

        /** Все опции выключены. */
        private const val OPTION_NONE = 0

        /** Префетч элементов в layoutManager. */
        private const val OPTION_PREFETCH = 1

        /** Поддержка "предсказательной анимации". */
        private const val OPTION_PREDICTIVE = 2

        /** Нефиксированный размер. */
        private const val OPTION_NO_FIXED = 4

        /** Режим "бесконечный список". */
        private const val OPTION_ENDLESS = 8

        /** Режим пагинации. */
        private const val OPTION_PAGING = 16

        /** Все опции включены. */
        private const val OPTION_ALL = 31

        /** Имена для инфлейтера. */
        val INFLATE_NAMES = setOf(
            "RecyclerView",
            RecyclerView::class.java.canonicalName,
            ESNRecyclerView::class.java.canonicalName
        )

        private const val REMOVE_AND_RECYCLE = false

        /** Adapter delegate. */
        private val ADAPTER_DELEGATE = AdapterDelegate(
            RecycledViewPool(),
            AdapterTypesResolver(),
            AdapterHolderFactory(),
            AdapterBinderFactory()
        )

        private fun RecyclerView.createRecyclerAdaptersFactory(
            delegate: AdapterDelegate<Any, ViewHolder>,
            removeAndRecycleExistingViews: Boolean = false,
            endless: Boolean
        ): (Any) -> Adapter<ViewHolder> {
            val diff = createEqualsHashCodeDiffUtil<Any>()
            val calc = SchedulersX.CALC
            return { content ->
                val handler = this.handler ?: Handler(Looper.getMainLooper())
                when (content) {
                    is PagingData<*> -> PagingAdapter(delegate, diff, handler, calc, content)
                    is List<*> -> GenericAdapter(delegate, diff, handler, calc, content, endless)
                    else -> errorNotSupportedThere()
                }.also { swapAdapter(it, removeAndRecycleExistingViews) }
            }
        }

        private fun Int.has(flag: Int) = flag and this == flag
    }
}
