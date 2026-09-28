package dev.arpan.calling

import android.graphics.Bitmap
import android.graphics.BitmapFactory

/** Decodes [path] scaled down to about [reqWidth] x [reqHeight] using [BitmapFactory.Options.inSampleSize]. */
internal fun decodeDownsampledFile(path: String, reqWidth: Int, reqHeight: Int): Bitmap? {
    val bounds =
        BitmapFactory.Options().apply {
            inJustDecodeBounds = true
            inSampleSize = 1
        }
    BitmapFactory.decodeFile(path, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    val options =
        BitmapFactory.Options().apply {
            inSampleSize = inSampleSizeFor(bounds.outWidth, bounds.outHeight, reqWidth, reqHeight)
        }
    return BitmapFactory.decodeFile(path, options)
}

/** Power-of-two sample size that brings both sides down to at most [maxWidth] x [maxHeight]. */
internal fun inSampleSizeToFit(width: Int, height: Int, maxWidth: Int, maxHeight: Int): Int {
    var inSampleSize = 1
    if (width <= 0 || height <= 0 || maxWidth <= 0 || maxHeight <= 0) return inSampleSize
    while (width / inSampleSize > maxWidth || height / inSampleSize > maxHeight) {
        inSampleSize *= 2
    }
    return inSampleSize
}

/**
 * Largest power-of-two sample size that still keeps both sides at least the requested size,
 * matching the platform bitmap-loading guidance.
 */
internal fun inSampleSizeFor(width: Int, height: Int, reqWidth: Int, reqHeight: Int): Int {
    var inSampleSize = 1
    if (reqWidth <= 0 || reqHeight <= 0) return inSampleSize
    if (height > reqHeight || width > reqWidth) {
        val halfHeight = height / 2
        val halfWidth = width / 2
        while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
            inSampleSize *= 2
        }
    }
    return inSampleSize
}
