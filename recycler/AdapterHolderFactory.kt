package com.esn.platform.recycler

import android.view.View
import androidx.recyclerview.widget.RecyclerView

/**
 * Фабрика базовых вью-холдеров.
 */
class AdapterHolderFactory : AdapterDelegate.HolderFactory<RecyclerView.ViewHolder> {

    /**
     * @param view экземпляр представления, который необходимо завернуть во вью-холдер
     *
     * @return новый экземпляр вьюхолдера, содержащий исходную вью.
     */
    override fun create(view: View) = object : RecyclerView.ViewHolder(view) {}

    /*

     Данная реализация не использует каких-либо специальных наследников RecyclerView.ViewHolder.
     Однако поскольку RecyclerView.ViewHolder - класс абстрактный, мы вынуждены создавать аноним.

     Для самой же системы RecyclerView - это не играет никакой роли.
     RecyclerView.ViewHolder - сущность необходимая только RecyclerView, но не разработчику.

     */
}
