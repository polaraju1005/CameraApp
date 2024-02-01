package com.example.cameraxapp

import org.opencv.core.Mat

class DocLib {
  companion object {
    fun detect(mat : Mat) : Mat?
    {
      val resized = Mat()

      CVLib.getReducedImage(mat.nativeObjAddr, resized.nativeObjAddr)

      val mask = Mat()
      CVLib.getDocumentMask(resized.nativeObjAddr, mask.nativeObjAddr)

      val cnt = Mat()
      CVLib.getDocumentContour(mask.nativeObjAddr, cnt.nativeObjAddr)

      val approxCnt = Mat()
      val found = CVLib.getDocumentApproxContour(cnt.nativeObjAddr, approxCnt.nativeObjAddr)


      if(found) {
        val resizedCnt = Mat()
        CVLib.resizeContour(mat.nativeObjAddr, resized.nativeObjAddr, approxCnt.nativeObjAddr, resizedCnt.nativeObjAddr)
        return resizedCnt

      }

      return null
    }


  }
}

