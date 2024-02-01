package com.example.cameraxapp

import android.graphics.PointF
import org.opencv.core.CvType
import org.opencv.core.Mat

class DocumentBorders(var pt1 : PointF, var pt2: PointF, var pt3: PointF, var pt4: PointF)  {
    enum class Rotation {
        ROTATE_90,
        ROTATE_180
    }

    fun rotate(rot: Rotation, width: Int, height: Int) {
        when(rot) {
            Rotation.ROTATE_90 -> {
                pt1.set(height - pt1.y, pt1.x)
                pt2.set(height - pt2.y, pt2.x)
                pt3.set(height - pt3.y, pt3.x)
                pt4.set(height - pt4.y, pt4.x)
            }

            Rotation.ROTATE_180 -> {
                pt1.set(pt1.x, height - pt1.y)
                pt2.set(pt2.x, height - pt2.y)
                pt3.set(pt3.x, height - pt3.y)
                pt4.set(pt4.x, height - pt4.y)
            }
        }
    }

    fun rescale(scale: Float) {
        pt1.set(scale*pt1.x, scale*pt1.y)
        pt2.set(scale*pt2.x, scale*pt2.y)
        pt3.set(scale*pt3.x, scale*pt3.y)
        pt4.set(scale*pt4.x, scale*pt4.y)
    }

    fun getPt(index: Int) : PointF? {
        when(index) {
            0 -> return pt1
            1 -> return pt2
            2 -> return pt3
            3 -> return pt4
        }
        return null
    }

    fun setPt(index: Int, pt: PointF)  {
        when(index) {
            0 -> pt1 = pt
            1 -> pt2 = pt
            2 -> pt3 = pt
            3 -> pt4 = pt
        }
    }

    fun rescaleSpecial( oldWidth : Float, oldHeight : Float,
                        newWidth : Float, newHeight : Float, matchWidth : Boolean) {

        var scale = 1.0f
        var offsetx = 0.0f;
        var offsety = 0.0f;

        if(matchWidth) {
            offsetx = 0.0f
            scale = newWidth/oldWidth
            offsety = (newHeight - scale*oldHeight)/2.0f
        } else {
            offsety = 0.0f
            scale = newHeight/oldHeight
            offsetx = (newWidth - scale*oldWidth)/2.0f
        }

        pt1.set(scale*pt1.x + offsetx, scale*pt1.y + offsety)
        pt2.set(scale*pt2.x + offsetx, scale*pt2.y + offsety)
        pt3.set(scale*pt3.x + offsetx, scale*pt3.y + offsety)
        pt4.set(scale*pt4.x + offsetx, scale*pt4.y + offsety)
    }

    fun copy() = DocumentBorders(
        PointF(pt1.x, pt1.y),
        PointF(pt2.x, pt2.y),
        PointF(pt3.x, pt3.y),
        PointF(pt4.x, pt4.y))

    constructor(cnt : Mat) : this(
        PointF(cnt.get(0, 0)[0].toFloat(), cnt.get(0, 0)[1].toFloat()),
        PointF(cnt.get(1, 0)[0].toFloat(), cnt.get(1, 0)[1].toFloat()),
        PointF(cnt.get(2, 0)[0].toFloat(), cnt.get(2, 0)[1].toFloat()),
        PointF(cnt.get(3, 0)[0].toFloat(), cnt.get(3, 0)[1].toFloat())) {
    }

    fun toMat() : Mat {
        val mat = Mat.zeros(4, 1, CvType.CV_32FC2)
        mat.put(0, 0, floatArrayOf(pt1.x, pt1.y))
        mat.put(1, 0, floatArrayOf(pt2.x, pt2.y))
        mat.put(2, 0, floatArrayOf(pt3.x, pt3.y))
        mat.put(3, 0, floatArrayOf(pt4.x, pt4.y))

        return mat
    }
}