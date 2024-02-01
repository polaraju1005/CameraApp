package com.example.cameraxapp

import android.content.Context
import android.graphics.*
import android.graphics.Paint.ANTI_ALIAS_FLAG
import android.util.AttributeSet
import android.view.DragEvent
import android.view.MotionEvent
import android.view.View
import java.lang.Integer.min
import kotlin.math.sqrt



class BorderOverlay(context: Context, attrs: AttributeSet) : View(context, attrs) {
  private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    strokeWidth = 4.0f
    style = Paint.Style.FILL
    textAlign = Paint.Align.CENTER
    textSize = 55.0f
    typeface = Typeface.create( "", Typeface.BOLD)
  }

  private var documentBorders : DocumentBorders? = null
  private var previousX: Float = 0f
  private var previousY: Float = 0f
  private var selected: Int? = null

  private var midpoints = arrayOf(PointF(), PointF(), PointF(), PointF())


  override fun onDraw(canvas: Canvas) {
    super.onDraw(canvas)
    val docBorders = documentBorders

    if(docBorders != null) {
      canvas.drawLine(docBorders.pt1.x, docBorders.pt1.y,
      docBorders.pt2.x, docBorders.pt2.y, paint)
      canvas.drawLine(docBorders.pt2.x, docBorders.pt2.y,
      docBorders.pt3.x, docBorders.pt3.y, paint)
      canvas.drawLine(docBorders.pt3.x, docBorders.pt3.y,
      docBorders.pt4.x, docBorders.pt4.y, paint)
      canvas.drawLine(docBorders.pt4.x, docBorders.pt4.y,
      docBorders.pt1.x, docBorders.pt1.y, paint)

      canvas.drawCircle(docBorders.pt1.x, docBorders.pt1.y, 20.0f, paint)
      canvas.drawCircle(docBorders.pt2.x, docBorders.pt2.y, 20.0f, paint)
      canvas.drawCircle(docBorders.pt3.x, docBorders.pt3.y, 20.0f, paint)
      canvas.drawCircle(docBorders.pt4.x, docBorders.pt4.y, 20.0f, paint)

      for(i in 0..3) {
        canvas.drawCircle(midpoints[i].x, midpoints[i].y, 10.0f, paint)
      }
    }
  }

  fun updateMidpoints()
  {
    val docBorders = documentBorders
    if(docBorders != null) {
      for(i in 0..3) {
        val pt_before = docBorders.getPt(i)
        val pt_after = docBorders.getPt((i+1)%4)
        midpoints[i].set((pt_before!!.x + pt_after!!.x)/2, (pt_before!!.y + pt_after!!.y)/2)
      }
    }
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
    updateMidpoints()
  }

  fun getDocumentBorders() : DocumentBorders?
  {
    return documentBorders
  }

  override fun onTouchEvent(e : MotionEvent) : Boolean
  {
    val doc = documentBorders ?: return true

    val x: Float = e.x
    val y: Float = e.y

    when (e.action) {
      MotionEvent.ACTION_DOWN -> {
        selected = null

        for(i in 0..3) {
          val pt = doc.getPt(i)

          if(pt != null) {
            val dx = x - pt.x
            val dy = y - pt.y

            if(dx*dx + dy*dy < 100*100) {
              selected = i
            }
          }
        }

        for(i in 0..3) {
          val pt = midpoints[i]

          if(pt != null) {
            val dx = x - pt.x
            val dy = y - pt.y

            if(dx*dx + dy*dy < 100*100) {
              selected = i+4
            }
          }
        }

      }

      MotionEvent.ACTION_MOVE -> {
        val sel = selected 
        if(sel != null) {
          if(sel < 4) {
            doc.setPt(sel, PointF(x, y))
          } else {
            val pt_before = doc.getPt(sel-4)
            val pt_after = doc.getPt((sel-4+1)%4)
            val pt = midpoints[sel-4]
            if(pt != null && pt_before != null && pt_after != null) {
              val d1 = PointF(pt_before.x - pt_after.x, pt_before.y - pt_after.y)
              val d2 = PointF(-d1.y, d1.x)
              val d2_len = sqrt(d2.x*d2.x + d2.y*d2.y)

              // Normalize 
              d2.x /= d2_len 
              d2.y /= d2_len 

              val diff = PointF(x - pt.x, y - pt.y)
              val t = diff.x * d2.x + diff.y * d2.y

              pt_before.x += t*d2.x
              pt_before.y += t*d2.y

              pt_after.x += t*d2.x
              pt_after.y += t*d2.y

              doc.setPt(sel-4, pt_before)
              doc.setPt((sel-4+1)%4, pt_after)
            }

          }

          updateMidpoints()
          invalidate()
          requestLayout()
        }
      }
    }

    return true
  }

  fun clear() {
    documentBorders = null
    invalidate()
    requestLayout()
  }
}

