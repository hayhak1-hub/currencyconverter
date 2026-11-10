package com.hayhak.currencyconverter.ui.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path

/**
 * Draws a small trend line for the widget. Widgets cannot run Compose canvas code, so the line is
 * rendered into a bitmap. Returns null when there are too few finite points to draw.
 */
fun renderSparkline(values: List<Double>, widthPx: Int, heightPx: Int, color: Int): Bitmap? {
    val points = values.filter { it.isFinite() }
    if (points.size < 3 || widthPx <= 0 || heightPx <= 0) return null

    val min = points.min()
    val max = points.max()
    val range = (max - min).takeIf { it > 0 } ?: 1.0
    val stroke = heightPx / 12f
    val pad = stroke * 1.5f
    val w = widthPx - pad * 2
    val h = heightPx - pad * 2

    val path = Path()
    points.forEachIndexed { i, v ->
        val x = pad + w * i / (points.size - 1)
        // A flat series is drawn through the middle rather than along the bottom edge.
        val y = if (max == min) heightPx / 2f else pad + h * (1f - ((v - min) / range).toFloat())
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }

    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = stroke
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        this.color = color
    }
    return Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888).also { Canvas(it).drawPath(path, paint) }
}
