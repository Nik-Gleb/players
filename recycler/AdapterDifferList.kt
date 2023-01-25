package com.esn.platform.recycler

import android.annotation.SuppressLint
import androidx.recyclerview.widget.*
import java.util.concurrent.Executor
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * Предоставляет адаптеру read/write поле со списком элементов List<Any>.
 *
 * Инкапсулирует калькулятор изменений,
 * созданый на основе некоторого diff-callback и начального списка.
 *
 * Предоставляет настройку планировщиков для тестирования.
 *
 * @param diffCallback стратегия вычисления различий
 * @param mainExecutor поток доставки нотификаций для адаптера
 * @param calcExecutor потоко для рассчётов различий списков
 * @param initialItems начальный список данных
 */
@Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
internal fun <T : RecyclerView.Adapter<*>> T.createListDiffer(
    diffCallback: DiffUtil.ItemCallback<Any>,
    mainExecutor: Executor,
    calcExecutor: Executor,
    initialItems: List<Any>
): ReadWriteProperty<T, List<Any>> = object : ReadWriteProperty<T, List<Any>> {

    /** Обертка для доставки изменений в адаптер. */
    private val callback = AdapterListUpdateCallback(this@createListDiffer)

    /** Обертка для пакетной доставки изменений. */
    private val updater = BatchingListUpdateCallback(callback)

    /** Конфигурация калькулятора. */
    @SuppressLint("RestrictedApi")
    private val config = AsyncDifferConfig.Builder(diffCallback)
        .setMainThreadExecutor(mainExecutor)
        .setBackgroundThreadExecutor(calcExecutor)
        .build()

    /** Калькулятор изменений. */
    private val async = AsyncListDiffer(updater, config)

    init {
        // Начальная инициализация списка.
        // Данные задаются моментально без вычисления изменений.
        async.submitList(initialItems)
        updater.dispatchLastEvent()
    }

    /**
     * Отправляет список в калькулятор.
     *
     * @param ref объект-владелец свойством
     * @param prop метаданные свойства
     * @param value новый список для калькулятора
     */
    override fun setValue(ref: T, prop: KProperty<*>, value: List<Any>) =
        async.submitList(value)

    /**
     * Возвращает последний сохранённый в калькуляторе список.
     *
     * @param ref объект-владелец свойством
     * @param prop метаданные свойства
     *
     * @return актуальный список элементов
     */
    override fun getValue(ref: T, prop: KProperty<*>) = async.currentList
}
