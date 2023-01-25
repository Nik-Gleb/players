package com.esn.platform.recycler

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import androidx.annotation.AttrRes
import androidx.annotation.StyleRes
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

@SuppressLint("PrivateResource")
class GridManagerLayout
@JvmOverloads
internal constructor(
    context: Context,
    attr: AttributeSet? = null,
    @AttrRes attrs: Int = 0,
    @StyleRes style: Int = 0,
    private val extraLayoutSpace: Int,
    private val predictiveAnimations: Boolean
) : GridLayoutManager(context, attr, attrs, style) {

    @Deprecated(
        "Deprecated in Java", ReplaceWith(
            "super.getExtraLayoutSpace(state)",
            "androidx.recyclerview.widget.LinearLayoutManager"
        )
    )
    @Suppress("DEPRECATION")
    override fun getExtraLayoutSpace(state: RecyclerView.State?) =
        (if (orientation == VERTICAL) height else width) * extraLayoutSpace

    @Suppress("DEPRECATION")
    override fun calculateExtraLayoutSpace(state: RecyclerView.State, extraLayoutSpace: IntArray) {
        val extraScrollSpace = getExtraLayoutSpace(state)
        extraLayoutSpace[0] = extraScrollSpace
        extraLayoutSpace[1] = extraScrollSpace
        if (layoutDirection == -1)
            extraLayoutSpace[0]++
        else extraLayoutSpace[1]++
    }

    override fun supportsPredictiveItemAnimations() =
        if (predictiveAnimations) super.supportsPredictiveItemAnimations() else false

}
