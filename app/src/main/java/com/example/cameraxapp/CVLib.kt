package com.example.cameraxapp


class CVLib {
  companion object {
    fun initLib() {
      System.loadLibrary("cameraxapp")
    }

    @JvmStatic 
    external fun getReducedImage(
      matAddr: Long,
      resultAddr: Long
    )

    @JvmStatic 
    external fun getDocumentMask(
      matAddr: Long,
      maskAddr: Long
    )

    @JvmStatic 
    external fun getDocumentContour(
      matAddr: Long,
      cntAddr: Long
    )

    @JvmStatic 
    external fun getDocumentApproxContour(
      cntAddr: Long,
      cntApproxAddr: Long
    ) : Boolean

    @JvmStatic 
    external fun resizeContour(
      matAddr: Long,
      resizedMatAddr: Long,
      cntAddr: Long,
      resizedCntAddr: Long
    )

    @JvmStatic 
    external fun drawContour(
      matAddr: Long,
      cntAddr: Long,
      R: Int, G: Int, B: Int
    )

    @JvmStatic 
    external fun getRotatedImage(
      matAddr: Long,
      resultAddr: Long
    )

    @JvmStatic 
    external fun isContourValid(
      cntAddr: Long,
      pcntAddr: Long
    ) : Boolean

    @JvmStatic 
    external fun smoothenContour(
      cntAddr: Long,
      pcntAddr: Long,
      acntAddr: Long
    )

    @JvmStatic 
    external fun drawContourDots(
      matAddr: Long,
      cntAddr: Long,
      R: Int, G: Int, B: Int
    )

    @JvmStatic
    external fun getDocumentWarped(
      matAddr : Long,
      resultAddr : Long,
      cntAddr : Long)

    @JvmStatic
    external fun doFilterBW(
      matAddr : Long,
      resultAddr : Long)

    @JvmStatic
    external fun doFilterGrayscale(
      matAddr : Long,
      resultAddr : Long)

    @JvmStatic
    external fun doFilterEnhance(
      matAddr : Long,
      resultAddr : Long)

    @JvmStatic
    external fun doFilterSoft(
      matAddr : Long,
      resultAddr : Long)

    @JvmStatic
    external fun addContour(
      cntAddr : Long,
      acntAddr : Long)

    @JvmStatic
    external fun divContour(
      acntAddr : Long,
      bcntAddr : Long,
      count : Int)

    @JvmStatic 
    external fun drawContourLines(
      matAddr: Long,
      cntAddr: Long,
      R: Int, G: Int, B: Int
    )

    @JvmStatic 
    external fun splitBook(
      matAddr: Long,
      page1Addr: Long,
      page2Addr: Long
    )

    @JvmStatic 
    external fun createWhiteBackground(
      matAddr: Long,
      width: Int,
      height: Int
    )

    @JvmStatic 
    external fun blitImage(
      matAddr: Long,
      srcAddr: Long,
      centerx: Int,
      centery: Int,
      sizex: Int,
      sizey: Int
    )
  }
}

