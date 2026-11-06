package com.artstudio.pro.core.graphics

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class NativeCanvasView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private var canvasBitmap: Bitmap = Bitmap.createBitmap(1080, 1920, Bitmap.Config.ARGB_8888)
    private var drawCanvas: Canvas = Canvas(canvasBitmap)
    private val strokePoints = mutableListOf<Float>()
    
    var currentBrushSize: Float = 24f
    var currentBrushColor: Int = Color.BLACK

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawBitmap(canvasBitmap, 0f, 0f, null)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y
        // Direct Pressure detection for S-Pen / Active Stylus & Touch
        val pressure = if (event.getPressure(0) > 0) event.getPressure(0) else 1.0f

        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                strokePoints.add(x)
                strokePoints.add(y)
                strokePoints.add(pressure)

                if (strokePoints.size >= 6) {
                    // Call Native NDK Engine for instant low-latency rasterization
                    NativeBrushEngine.drawStrokeNative(
                        canvasBitmap,
                        strokePoints.toFloatArray(),
                        currentBrushColor,
                        currentBrushSize,
                        0.8f
                    )
                    invalidate()
                }
            }
            MotionEvent.ACTION_UP -> {
                strokePoints.clear()
            }
        }
        return true
    }
}

object NativeBrushEngine {
    init {
        System.loadLibrary("artstudio_graphics")
    }

    external fun drawStrokeNative(
        bitmap: Bitmap,
        points: FloatArray,
        color: Int,
        brushSize: Float,
        hardness: Float
    )
}
