package com.esn.platform.recycler

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Typeface
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.text.PrecomputedTextCompat
import androidx.core.widget.TextViewCompat
import com.esn.platform.stdlib.SchedulersX
import com.esn.platform.xcore.VIEW_CONTENT_TAG
import java.util.*
import java.util.concurrent.Future
import kotlin.math.roundToInt

/** Менеджер типов вью-холдеров. */
internal class AdapterTypesResolver : AdapterDelegate.TypesResolver<Any> {

    /** Сопоставление вью-классов и типов. */
    private val map = IdentityHashMap<Class<out View>, Pair<Int, (Context) -> View>>()

    /** Сопоставление типов и вью-конструкторов. */
    private val views = HashMap<Int, (Context) -> View>()

    /** Сопоставление модель-классов и типов. */
    private val models = IdentityHashMap<Class<out Any>, Int>()

    /**
     * Регистрирует новую связку "типмодель<->типвью".
     *
     * @param model тип модели
     * @param model тип вью
     *
     * @return ID типа
     */
    override fun map(model: Class<out Any>, view: Class<out View>): Int {
        val pair = try {
            map.getOrPut(view) {
                val type = view.hashCode()
                val constructor = view.constructors[0]
                val factory: (Context) -> View = { context ->
                    try {
                        constructor.newInstance(context) as View
                    } catch (throwable: Throwable) {
                        throwable.printStackTrace()
                        StubView(context)
                    }
                }
                Pair(type, factory)
            }
        } catch (throwable: Throwable) {
            UNKNOWN
        }

        if (pair != UNKNOWN) {
            models[model] = pair.first
            views[pair.first] = pair.second
        }

        return pair.first
    }

    /**
     * Предоставляет ID элемента.
     * Для разных элементов представляющих одни и те же данные будет возвращён один и тот же
     * результат.
     *
     * @param item элемент коллекции, чей ID требуется определить
     *
     * @return id элемента
     */
    override fun getIdByItem(item: Any) = 31 * item.javaClass.hashCode() + item.hashCode()

    /**
     * Предоставляет ID типа по элементу.
     * Для разных элементов с одинаковым типом будет возвращён один и тот же результат.
     *
     * @param item элемент коллекции, чей тип требуется определить
     *
     * @return id типа
     */
    override fun getTypeByItem(item: Any) = models[item.javaClass] ?: UNKNOWN.first

    /**
     * Создаёт новый View экземпляр для данного типа элементов.
     *
     * @param parent родительское представление
     * @param type ID типа элемента, для которого требуется view
     *
     * @return новый экземпляр view, совместимый с типом элементов
     */
    override fun createViewByType(parent: ViewGroup, type: Int): View =
        parent.context.let { ctx -> views[type]?.invoke(ctx) ?: StubView(ctx) }

    internal companion object {

        @SuppressLint("AppCompatCustomView")
        private class StubView(context: Context) : AppCompatTextView(context) {
            private val metrics: PrecomputedTextCompat.Params
            private var future: Future<PrecomputedTextCompat>? = null

            init {
                val padding = (resources.displayMetrics.density * 4).roundToInt()
                setPadding(padding, padding, padding, padding)
                gravity = Gravity.CENTER
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 8f)
                typeface = Typeface.create(Typeface.MONOSPACE, 50, false)
                filters = arrayOf()
                metrics = TextViewCompat.getTextMetricsParams(this)
            }

            @Suppress("UNCHECKED_CAST")
            override fun setTag(key: Int, tag: Any?) {
                if (key != VIEW_CONTENT_TAG) {
                    super.setTag(key, tag)
                    return
                }

                future?.cancel(false)
                if (tag == null) return
                future = PrecomputedTextCompat.getTextFuture(
                    tag.toString(),
                    metrics,
                    SchedulersX.CALC
                )
                setTextFuture(future)
            }
        }

        /** Stub пара. */
        private val UNKNOWN: Pair<Int, (Context) -> View> =
            Pair(AdapterDelegate.TypesResolver.UNKNOWN_TYPE, ::StubView)
    }
}
