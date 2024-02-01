package com.example.cameraxapp

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.graphics.decodeBitmap
import com.example.cameraxapp.databinding.ActivityImageViewerBinding
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class ImageViewer : AppCompatActivity() {
  private lateinit var viewBinding: ActivityImageViewerBinding
  private var original : Mat? = null
  private var pictureType : String? = ""

  @SuppressLint("WrongThread")
  @RequiresApi(Build.VERSION_CODES.P)
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    viewBinding = ActivityImageViewerBinding.inflate(layoutInflater)
    setContentView(viewBinding.root)

      viewBinding.warpButton.setOnClickListener {
        warpImage()
      }

    val uri = Uri.parse(intent.getStringExtra(EXTRA_PICTURE_URI))
    pictureType = intent.getStringExtra(EXTRA_PICTURE_TYPE);

    val imageDecoder = ImageDecoder.createSource(contentResolver, uri)
    val bitmap = ImageDecoder.decodeBitmap(imageDecoder)

    val bmp32 = bitmap.copy(Bitmap.Config.ARGB_8888, true);

    contentResolver.delete(uri, null, null)

    val mat = Mat()
    
    Utils.bitmapToMat(bmp32, mat)


    // get current camera frame as OpenCV Mat object
    Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGBA2RGB)

    original = mat.clone()

    viewBinding.borderOverlay.post(Runnable {
      val approxCnt = DocLib.detect(mat)
      if(approxCnt != null) {
        val documentBorders = DocumentBorders(approxCnt)
        viewBinding.borderOverlay.setDocumentBorders(documentBorders, mat.cols(), mat.rows())
      }
    })

    val result = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888)
    Utils.matToBitmap(mat, result)
    viewBinding.imageView.setImageBitmap(result)
  }

  private fun warpImage()
  {
    val border = viewBinding.borderOverlay.getDocumentBorders()
    val mat = original
    if(mat != null && border != null) {
      border.rescaleSpecial(
          viewBinding.borderOverlay.getWidth().toFloat(),
          viewBinding.borderOverlay.getHeight().toFloat(),
          mat.cols().toFloat(),
          mat.rows().toFloat(), mat.rows() > mat.cols() )
      val cnt = border.toMat()
      val warped = Mat()
      CVLib.getDocumentWarped(mat.nativeObjAddr, warped.nativeObjAddr, cnt.nativeObjAddr)

      val result = Bitmap.createBitmap(warped.cols(), warped.rows(), Bitmap.Config.ARGB_8888)
      Utils.matToBitmap(warped, result)

      val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US)
      .format(System.currentTimeMillis())
      val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, name)
        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
          put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraX-Image")
        }
      }

      val uri = contentResolver.insert(
          MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
          contentValues
      ) ?: throw IOException("Could not open uri")

      val stream = contentResolver.openOutputStream(uri) ?: throw IOException("Could not open output stream")

      result.compress(Bitmap.CompressFormat.JPEG, 95, stream)
      stream.close()

      if(pictureType == "book") {
        val intent = Intent(this, BookViewer::class.java).apply {
          putExtra(EXTRA_PICTURE_URI, uri.toString())
        }
        startActivity(intent)

      } else if(pictureType == "idcard") {
        val intent = Intent(this, IDCardViewer::class.java).apply {
          putExtra(EXTRA_PICTURE_URI, uri.toString())
        }
        startActivity(intent)

      } else {
        val intent = Intent(this, ImageFilter::class.java).apply {
          putExtra(EXTRA_PICTURE_URI, uri.toString())
        }
        startActivity(intent)
      }
    }
  }



  companion object {
    private const val TAG = "CameraXApp"
    private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"

  }
}

