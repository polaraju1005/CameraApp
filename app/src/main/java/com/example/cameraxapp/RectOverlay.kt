package com.example.cameraxapp

import android.content.Context
import android.graphics.*
import android.graphics.Paint.ANTI_ALIAS_FLAG
import android.util.AttributeSet
import android.view.View
import java.lang.Integer.min

class RectOverlay(context: Context, attrs: AttributeSet) : View(context, attrs) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        textAlign = Paint.Align.CENTER
        textSize = 55.0f
        typeface = Typeface.create( "", Typeface.BOLD)
    }

    private var radius = 0.0f
    private var bitmap : Bitmap? = null
    private var documentBorders : DocumentBorders? = null

    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        radius = (min(width, height) / 2.0 * 0.8).toFloat()
    }

    fun getRadius(): Float {
        return radius
    }

    fun setRadius(newRadius: Float) {
        radius = newRadius
        invalidate()
        requestLayout()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        documentBorders?.let {
          canvas.drawCircle(documentBorders!!.pt1.x, documentBorders!!.pt1.y, 30.0f, paint)
          canvas.drawCircle(documentBorders!!.pt2.x, documentBorders!!.pt2.y, 30.0f, paint)
          canvas.drawCircle(documentBorders!!.pt3.x, documentBorders!!.pt3.y, 30.0f, paint)
          canvas.drawCircle(documentBorders!!.pt4.x, documentBorders!!.pt4.y, 30.0f, paint)
        }

        // val bm = bitmap
        // bm?.let {
          // canvas.drawBitmap(bm, null,
            // Rect(
              // ((width - bm.width)/2),
              // ((height - bm.height)/2),
              // ((width - bm.width)/2 + bm.width),
              // ((height - bm.height)/2 + bm.height)), null)
        // }
    }

    fun setDocumentBorders(newDocumentBorders: DocumentBorders?, 
      oldWidth : Int,
      oldHeight : Int
    ) {
      documentBorders = newDocumentBorders?.copy()
      documentBorders?.rescaleSpecial(
        oldWidth.toFloat(), 
        oldHeight.toFloat(), 
        width.toFloat(), 
        height.toFloat(), height > width)
      invalidate()
      requestLayout()
    }

    fun setBitmap(newBitmap: Bitmap?) {
      bitmap = newBitmap
      invalidate()
      requestLayout()
    }

    fun clear() {
      bitmap = null
      documentBorders = null
      invalidate()
      requestLayout()
    }
}
