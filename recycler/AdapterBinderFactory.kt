package com.esn.platform.recycler

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.esn.platform.xcore.changing
import com.esn.platform.xcore.setContent

/**
 * Фабрика нетипизированных вью-биндеров.
 */
class AdapterBinderFactory : AdapterDelegate.BinderFactory<Any, RecyclerView.ViewHolder> {

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
    override fun create(holder: RecyclerView.ViewHolder, batch: Boolean): (Any?) -> Unit {

        // Для "горячих" апдейтов включаем layout transitions
        (holder.itemView as? ViewGroup)?.layoutTransition?.changing = batch

        /*
            Работает с любой вью, передавая нетипизированный объект в виде спецтэга

            Для обработки данных внутри вью разработчику необходимо:
             - переопределить этот метод во своём вью: override `fun setTag(key: Int, tag: Any?)`
             - ненулевой объект - представляет биндинг данных, нулевой - очистку
             - если элементы поступают чередуясь с null - это означает прокрутку,
                если без зануления - горячее обновление состояния (разработчик может в зависимости
                от этого принимать правильные решения по визуализации изменений)
         */
        return { holder.itemView.setContent(it) }
    }

}
