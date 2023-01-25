package com.esn.platform.recycler

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlin.reflect.KClass

/**
 * Адаптер-делегат для RecyclerView адаптеров.
 *
 * Делегат для реализации базового функционала адаптеров коллекций.
 *
 * @param pool пулл вьюхолдеров
 * @param maxPolledPerType максимальное число холдер на тип
 * @param typesResolver роутер типов элементов
 * @param holderFactory фабрика вью-холдеров
 * @param binderFactory фабрика вью-биндеров
 *
 * @param Item тип элементов данных
 * @param Holder тип вью-холдеров
 */
class AdapterDelegate<Item : Any, Holder : RecyclerView.ViewHolder>(
    private val pool: RecyclerView.RecycledViewPool,
    private val typesResolver: TypesResolver<Item>,
    private val holderFactory: HolderFactory<Holder>,
    private val binderFactory: BinderFactory<Item, Holder>,
    private val maxPolledPerType: Int = 128,
) {

    /* Начальная настройка пула на лимит stub-вью холдеров. */
    init {
        pool.setMaxRecycledViews(TypesResolver.UNKNOWN_TYPE, maxPolledPerType)
    }

    /**
     * Возвращает идентификатор элемента.
     *
     * @param item элемент, чей тип необходимо определить
     *
     * @return ID элемента
     */
    fun getIdByItem(item: Item) = typesResolver.getIdByItem(item).toLong()

    /**
     * Возвращает тип элемента.
     *
     * Все используемые в коллекции типы должны быть предварительно известны роутеру типов.
     *
     * @param item элемент, чей тип необходимо определить
     *
     * @return ID типа элемента
     */
    fun getTypeByItem(item: Item) = typesResolver.getTypeByItem(item)

    /**
     * Создаёт вью-холдер, соответствующий указанному типу.
     *
     * Все используемые в коллекции вью должны быть предварительно известны роутеру типов.
     * В противном случае этот метод возвращает fallback вью.
     *
     * @param parent родительсеое представление-контейнер для новой вью
     * @param type тип элементов, под который инстанцируется новыая вью
     *
     * @return новый экземпляр вью-холдера
     */
    fun createViewHolderByType(parent: ViewGroup, type: Int) =
        holderFactory.create(typesResolver.createViewByType(parent, type))

    /**
     * Привязка данных к вью-холдеру.
     * Вызывается, когда элемент появляется в видимой области, либо получает changes обновления.
     *
     * @param holder вью-холдер для привязки данных
     * @param changes change-значения необходимые для привязки
     * @param fallback значение для `холодной замены`.
     */
    fun bindViewHolder(holder: Holder, changes: List<Item>, fallback: Item) {
        val hotBinding = changes.isNotEmpty()
        val bind = binderFactory.create(holder, hotBinding)
        if (!hotBinding) bind(fallback)
        else changes.forEach(bind)
    }

    /**
     * Отвязка данных от вью-холдера.
     * Вызывается, когда элемент покидает видимую область.
     *
     * @param holder вью-холдер, который нужно очистить
     */
    fun unbindViewHolder(holder: Holder) {
        binderFactory.create(holder)(null)
    }

    /** @param recyclerView [RecyclerView] для подключения пула. */
    fun attachRecyclerView(recyclerView: RecyclerView) =
        recyclerView.setRecycledViewPool(pool)

    /** @param recyclerView [RecyclerView] для отключения пула. */
    fun detachRecyclerView(recyclerView: RecyclerView) =
        recyclerView.setRecycledViewPool(null)

    /**
     * Сопоставление типов моделей и типов вью
     *
     * @param types пары "типмодель<->типвью"
     */
    fun map(vararg types: Pair<KClass<out Any>, KClass<out View>>) = types.forEach {
        pool.setMaxRecycledViews(typesResolver.map(it.first.java, it.second.java), maxPolledPerType)
    }

    /**
     * Роутер типов элементов.
     *
     * Обеспечивает координацию типов элементов и создание соответствующих view-экземпляров.
     */
    interface TypesResolver<T> {

        /**
         * Предоставляет ID элемента.
         * Для разных элементов представляющих одни и те же данные будет возвращён один и тот же
         * результат.
         *
         * @param item элемент коллекции, чей ID требуется определить
         *
         * @return id элемента
         */
        fun getIdByItem(item: T): Int

        /**
         * Предоставляет ID типа по элементу.
         * Для разных элементов с одинаковым типом будет возвращён один и тот же результат.
         *
         * @param item элемент коллекции, чей тип требуется определить
         *
         * @return id типа
         */
        fun getTypeByItem(item: T): Int

        /**
         * Создаёт новый View экземпляр для данного типа элементов.
         *
         * @param parent родительское представление
         * @param type ID типа элемента, для которого требуется view
         *
         * @return новый экземпляр view, совместимый с типом элементов
         */
        fun createViewByType(parent: ViewGroup, type: Int): View

        /**
         * Регистрирует новую связку "типмодель<->типвью".
         *
         * @param model тип модели
         * @param model тип вью
         *
         * @return ID типа
         */
        fun map(model: Class<out Any>, view: Class<out View>): Int

        companion object {

            /** Индекс неизвестного типа. */
            const val UNKNOWN_TYPE = -1
        }
    }

    /**
     * Фабрика вью-холдеров для использования в RecyclerView адаптере.
     *
     * @param Holder класс вью-холдера который необходимо инстанцировать
     */
    interface HolderFactory<Holder : RecyclerView.ViewHolder> {

        /**
         * @param view экземпляр представления, который необходимо завернуть во вью-холдер
         *
         * @return новый экземпляр вьюхолдера, содержащий исходную вью.
         */
        fun create(view: View): Holder
    }

    /**
     * Фабрика биндеров для использования в RecyclerView адаптере.
     *
     * @param Item тип элементов коллекции с которыми совместим вью-холдер
     * @param Holder класс вью-холдера, который необходимо инстанцировать
     */
    interface BinderFactory<Item, Holder : RecyclerView.ViewHolder> {

        /**
         * На основе холдера создаёт функцию-биндер, которая может принимать элементы или null для
         * внутренней очистки ресурсов, если это не обходимо представлению.
         *
         * Может быть вызвана многократно для последовательной трансформации вью представления из
         * состояния в состояние.
         *
         * @param holder вью-холдер, с совместимый для работы с определёнными элементами
         * @param batch batch режим помогает сконфигурировать холдер в зависимости от режима
         * биндинга: `серия элементов` либо `одиночный binding`
         *
         * @return функция-биндер элементов коллекции с вью-холдером
         */
        fun create(holder: Holder, batch: Boolean = false): (Item?) -> Unit
    }
}
