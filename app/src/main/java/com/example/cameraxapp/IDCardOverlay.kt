package com.example.cameraxapp

import android.content.Context
import android.graphics.*
import android.graphics.Paint.ANTI_ALIAS_FLAG
import android.util.AttributeSet
import android.view.DragEvent
import android.view.MotionEvent
import android.view.View
import java.lang.Integer.min

class IDCardOverlay(context: Context, attrs: AttributeSet) : View(context, attrs) {
  private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    style = Paint.Style.FILL
    textAlign = Paint.Align.CENTER
    textSize = 55.0f
    typeface = Typeface.create( "", Typeface.BOLD)
  }

  private var bitmap : Bitmap? = null
  private var documentBorders : DocumentBorders? = null
  private var center : Point = Point()
  private var size : Point = Point()

  private var cardSelected: Boolean = false

  override fun onDraw(canvas: Canvas) {
    super.onDraw(canvas)

    val bm = bitmap
    bm?.let {
      canvas.drawBitmap(bm, null,
      Rect(
        center.x - size.x/2,
        center.y - size.y/2,
        center.x + size.x/2,
        center.y + size.y/2), null)

      val p1 = PointF((center.x - size.x/2).toFloat(), (center.y - size.y/2).toFloat())
      val p2 = PointF((center.x + size.x/2).toFloat(), (center.y - size.y/2).toFloat())
      val p3 = PointF((center.x + size.x/2).toFloat(), (center.y + size.y/2).toFloat())
      val p4 = PointF((center.x - size.x/2).toFloat(), (center.y + size.y/2).toFloat())
      
      canvas.drawLine(p1.x, p1.y, p2.x, p2.y, paint)
      canvas.drawLine(p2.x, p2.y, p3.x, p3.y, paint)
      canvas.drawLine(p3.x, p3.y, p4.x, p4.y, paint)
      canvas.drawLine(p4.x, p4.y, p1.x, p1.y, paint)

      canvas.drawCircle(p1.x, p1.y, 15.0f, paint)
      canvas.drawCircle(p2.x, p2.y, 15.0f, paint)
      canvas.drawCircle(p3.x, p3.y, 15.0f, paint)
      canvas.drawCircle(p4.x, p4.y, 15.0f, paint)

    }
  }

  fun setBitmap(newBitmap: Bitmap?) {
    bitmap = newBitmap
    invalidate()
    requestLayout()
  }

  fun setCenter(newCenter : Point) {
    center = newCenter
    invalidate()
    requestLayout()
  }

  fun setSize(newSize : Point) {
    size = newSize
    invalidate()
    requestLayout()
  }

  override fun onTouchEvent(e : MotionEvent) : Boolean
  {
    val x: Float = e.x
    val y: Float = e.y

    when (e.action) {
      MotionEvent.ACTION_DOWN -> {
        cardSelected = false

        if(x > center.x - size.x/2 && x < center.x + size.x/2 && y > center.y - size.y/2 && y < center.y + size.y/2) {
          cardSelected = true
        }
      }

      MotionEvent.ACTION_MOVE -> {
        if(cardSelected) {
          center.x = x.toInt()
          center.y = y.toInt()

          invalidate()
          requestLayout()
        }
      }
    }
    return true
  }

  fun getSize() : Point
  {
    return size;
  }

  fun getCenter() : Point
  {
    return center;
  }
}
