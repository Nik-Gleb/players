package com.esn.platform.players

import android.graphics.Matrix
import android.graphics.Point
import android.graphics.PointF

/**
 * VideoMatrix.
 *
 * Calculates matrix transformation for normalize aspect ratio.
 *
 * @param width initial horizontal size of view
 * @param height initial horizontal size of view
 * @param matrix initial view matrix
 * @param callback set matrix callback
 * @param crop fitting mode (crop/fit)
 */
class VideoMatrix(
    width: Int,
    height: Int,
    matrix: Matrix,
    private val callback: (Matrix) -> Unit,
    private val crop: Boolean = true
) {

    private val view = Point(width, height)
    private val center = PointF()
    private val scale = PointF()
    private val video = Point(NO_SIZE, NO_SIZE)
    private val matrix = Matrix().also { reset(); }
    private val post = Matrix().also { reset(); matrix.invert(it) }

    fun view(width: Int, height: Int) {
        if (view.x != width || view.y != height) {
            view.set(width, height)
            center.set(width * HALF, height * HALF)
            invalidate()
        }
    }

    fun reset() = video(NO_SIZE, NO_SIZE)

    fun video(width: Int, height: Int) {
        if (video.x != width || video.y != height) {
            video.set(width, height)
            invalidate()
        }
    }

    fun post(matrix: Matrix) {
        post.reset()
        matrix.invert(post)
        invalidate()
    }

    private fun invalidate() {
        matrix.reset()

        if (video.x != NO_SIZE && video.y != NO_SIZE) {
            val viewW = view.x.toFloat()
            val viewH = view.y.toFloat()
            val videoW = video.x.toFloat()
            val videoH = video.y.toFloat()
            scale.set(NO_SCALE, NO_SCALE)

            val ratio = viewW / viewH
            val origin = videoW / videoH
            val more = origin > ratio
            val less = ratio > origin

            if (if (crop) less else more) scale.y = ratio / origin
            else if (if (crop) more else less) scale.x = origin / ratio

            matrix.setScale(scale.x, scale.y, center.x, center.y)

            matrix.postConcat(post)
        }

        callback(matrix)
    }

    companion object {
        private const val NO_SIZE = 0
        private const val NO_SCALE = 1f
        private const val HALF = 0.5f
    }
}
