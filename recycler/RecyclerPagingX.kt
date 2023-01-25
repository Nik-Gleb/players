package com.esn.platform.recycler

import androidx.annotation.IntRange
import androidx.paging.*
import kotlinx.coroutines.flow.Flow
import kotlin.collections.HashMap
import kotlin.math.max
import kotlin.math.min

private fun config(@IntRange(from = 1) numOfVisibleItems: Int = 1): PagingConfig {
    val pageSize = numOfVisibleItems * (if (numOfVisibleItems == 1) 3 else 10)
    val prefetchDistance = pageSize * 1
    val initialLoadSize = pageSize * 2
    val maxSize = PagingConfig.MAX_SIZE_UNBOUNDED
    val jumpThreshold = Int.MIN_VALUE
    val enablePlaceHolders = false
    return PagingConfig(
        pageSize,
        prefetchDistance,
        enablePlaceHolders,
        initialLoadSize,
        maxSize,
        jumpThreshold
    )
}


private fun <Key : Any, Value : Any> PagingCall<Key, Value>.toPagingSource(anchor: () -> Int) =
    object : PagingSource<Key, Value>() {
        override fun getRefreshKey(state: PagingState<Key, Value>) =
            state.closestItemToPosition(anchor())?.let(this@toPagingSource::identity)

        override suspend fun load(params: LoadParams<Key>) =
            try {
                val response = call(params.toPagingRequest())
                if (!invalid) response.toLoadResult()
                else LoadResult.Invalid()
            } catch (error: Throwable) {
                LoadResult.Error(error)
            }
    }

private fun <Key : Any> PagingSource.LoadParams<Key>.toPagingRequest() =
    PagingRequest(
        loadSize, key, when (this) {
            is PagingSource.LoadParams.Append -> true
            is PagingSource.LoadParams.Prepend -> false
            else -> true
        }
    )

data class PagingRequest<Key>(val limit: Int, var key: Key?, val forward: Boolean = true)

data class PagingResponse<Key, Value>(
    val data: List<Value>,
    val prev: Key? = null,
    val next: Key? = null
)

interface PagingCall<Key, Value> {

    fun identity(value: Value): Key = error("Not supported yet!")

    suspend fun call(request: PagingRequest<Key>): PagingResponse<Key, Value>
}

data class PagingView(val numOfVisibleItems: Int, val anchor: () -> Int)

private fun <Key : Any, Value : Any> PagingResponse<Key, Value>.toLoadResult() =
    PagingSource.LoadResult.Page(data, prev, next)


/// ==========================================================================================
fun <Key : Any, Value : Any> pager(
    call: () -> PagingCall<Key, Value>,
    view: PagingView,
    key: Key?
): Flow<PagingData<Value>> {
    return Pager(config(view.numOfVisibleItems), key) { call().toPagingSource(view.anchor) }.flow
}

val DUMMY_PAGING_CALL = object : PagingCall<String, String> {

    override fun identity(value: String) = value

    override suspend fun call(request: PagingRequest<String>): PagingResponse<String, String> {
        val dataset = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789АБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯ"
            .toCharArray().map { ch -> ch.toString() }
        val index = with(dataset) { max(indexOf(find { item -> item == request.key }), 0) }
        val from: Int
        val to: Int
        if (request.forward) {
            from = index
            to = from + request.limit
        } else {
            from = index - request.limit + 1
            to = index + 1
        }
        val drop = max(from, 0)
        val take = min(from, 0) + request.limit
        val list = dataset.drop(drop).take(take)
        val prev = try {
            dataset[from - 1]
        } catch (e: Throwable) {
            null
        }
        val next = try {
            dataset[to]
        } catch (e: Throwable) {
            null
        }
        return PagingResponse(list, next, prev).apply {
            val logKey = request.key ?: " "
            val initial: String = if (request.forward) "$logKey>" else "<$logKey}"
            val logTag = "PAGING_TEST($initial)"
            println("$logTag: ${prev ?: " "}|${data.joinToString("")}|${next ?: " "}")
        }
    }
}

private val identity = HashMap<Any, Int>()

fun <Value : Any> PagingCall<Int, Value>.esnSpecificPagination(): PagingCall<Int, Value> =
    object : PagingCall<Int, Value> {

        override fun identity(value: Value): Int {
            return identity[value] ?: error("Key not found")
            //this@adaptToBasePaging.identity(value) not supported
        }

        @Suppress("UnnecessaryVariable")
        override suspend fun call(request: PagingRequest<Int>): PagingResponse<Int, Value> {
            val bounds = intArrayOf(0, 0)
            val esnRequest = request.toESNBackRequest(bounds)

            if (bounds[0] <= 0 && bounds[1] <= 0)
                return PagingResponse(emptyList())

            val esnResponse = this@esnSpecificPagination.call(esnRequest)
            bounds[0] = esnResponse.prev ?: bounds[0]
            val pagingResponse = esnResponse.toPagingResponse(bounds).toESNPagingResponse(bounds)
            return pagingResponse.apply {
                data.forEachIndexed { index, value ->
                    identity[value] = (esnRequest.key ?: 0) + index
                }
            }
        }
    }

private fun PagingRequest<Int>.toESNBackRequest(bounds: IntArray): PagingRequest<Int> {
    val index = key ?: 0

    val from: Int // inclusive
    val to: Int   // exclusive

    if (forward) {
        from = index
        to = from + limit

    } else { // prepend
        from = index - limit
        to = index
    }

    bounds[0] = from
    bounds[1] = to

    val currentOffset = if (from < 0) 0 else from
    val currentLimit = if (from < 0) from + limit else limit

    return PagingRequest(currentLimit, currentOffset, forward)
}

private fun <Value> PagingResponse<Int, Value>.toPagingResponse(
    bounds: IntArray
): PagingResponse<Int, Value> {
    val prev = if (bounds[0] < 0) null else bounds[0]
    return PagingResponse(data, prev, bounds[1])
}

private fun <Value> PagingResponse<Int, Value>.toESNPagingResponse(
    bounds: IntArray
): PagingResponse<Int, Value> {
    val prev = if (bounds[0] < 0) null else bounds[0]
    val next = if (data.isEmpty()) {
        null
    } else {
        bounds[1]
    }
    return PagingResponse(data, prev, next)
}
