package com.example.cameraxapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import com.example.cameraxapp.databinding.ActivityBookViewerBinding
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*


class BookViewer : AppCompatActivity() {
  private lateinit var viewBinding: ActivityBookViewerBinding
  private var originalPage1 : Mat? = null
  private var originalPage2 : Mat? = null
  private var original : Mat? = null
  private var result : Mat? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    viewBinding = ActivityBookViewerBinding.inflate(layoutInflater)
    setContentView(viewBinding.root)

    viewBinding.BWFilterButton.setOnClickListener {
      doBWFilter()
    }

    viewBinding.GrayscaleFilterButton.setOnClickListener {
      doGrayscaleFilter()
    }

    viewBinding.EnhanceFilterButton.setOnClickListener {
      doEnhanceFilter()
    }

    viewBinding.SaveButton.setOnClickListener {
      doSave()
    }

    viewBinding.SoftFilterButton.setOnClickListener {
      doSoftFilter()
    }

    viewBinding.ButtonOriginal.setOnClickListener {
      doNoFilter()
    }

    viewBinding.radioPage1.setOnClickListener {
      original = originalPage1
      doNoFilter()
    }

    viewBinding.radioPage2.setOnClickListener {
      original = originalPage2
      doNoFilter()
    }

    val uri = Uri.parse(intent.getStringExtra(EXTRA_PICTURE_URI))
    val imageDecoder = ImageDecoder.createSource(contentResolver, uri)
    val bitmap = ImageDecoder.decodeBitmap(imageDecoder)

    val bmp32 = bitmap.copy(Bitmap.Config.ARGB_8888, true);

    contentResolver.delete(uri, null, null)

    val mat = Mat()
    Utils.bitmapToMat(bmp32, mat)

    // get current camera frame as OpenCV Mat object
    Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGBA2RGB)

    originalPage1 = Mat()
    originalPage2 = Mat()

    CVLib.splitBook(mat.nativeObjAddr, originalPage1!!.nativeObjAddr, originalPage2!!.nativeObjAddr)
    original = originalPage1
    result = original!!.clone()
    

    val resultBitmap = Bitmap.createBitmap(result!!.cols(), result!!.rows(), Bitmap.Config.ARGB_8888)
    Utils.matToBitmap(result, resultBitmap)

    viewBinding.imageView2.setImageBitmap(resultBitmap)
  }

  private fun doBWFilter()
  {
    val original = original ?: return
    val result = result ?: return
    CVLib.doFilterBW(original.nativeObjAddr, result.nativeObjAddr)

    val resultBitmap = Bitmap.createBitmap(result.cols(), result.rows(), Bitmap.Config.ARGB_8888)
    Utils.matToBitmap(result, resultBitmap)

    viewBinding.imageView2.setImageBitmap(resultBitmap)
  }

  private fun doGrayscaleFilter()
  {
    val original = original ?: return
    val result = result ?: return
    CVLib.doFilterGrayscale(original.nativeObjAddr, result.nativeObjAddr)
    val resultBitmap = Bitmap.createBitmap(result.cols(), result.rows(), Bitmap.Config.ARGB_8888)
    Utils.matToBitmap(result, resultBitmap)

    viewBinding.imageView2.setImageBitmap(resultBitmap)
  }

  private fun doEnhanceFilter()
  {
    val original = original ?: return
    val result = result ?: return
    CVLib.doFilterEnhance(original.nativeObjAddr, result.nativeObjAddr)
    val resultBitmap = Bitmap.createBitmap(result.cols(), result.rows(), Bitmap.Config.ARGB_8888)
    Utils.matToBitmap(result, resultBitmap)

    viewBinding.imageView2.setImageBitmap(resultBitmap)
  }

  private fun doSoftFilter()
  {
    val original = original ?: return
    val result = result ?: return
    CVLib.doFilterSoft(original.nativeObjAddr, result.nativeObjAddr)
    val resultBitmap = Bitmap.createBitmap(result.cols(), result.rows(), Bitmap.Config.ARGB_8888)
    Utils.matToBitmap(result, resultBitmap)

    viewBinding.imageView2.setImageBitmap(resultBitmap)
  }

  private fun doNoFilter()
  {
    val original = original ?: return
    result = original.clone()
    val resultBitmap = Bitmap.createBitmap(original.cols(), original.rows(), Bitmap.Config.ARGB_8888)
    Utils.matToBitmap(original, resultBitmap)

    viewBinding.imageView2.setImageBitmap(resultBitmap)
  }

  private fun doSave()
  {
    val result = result ?: return

    val bitmap = Bitmap.createBitmap(result.cols(), result.rows(), Bitmap.Config.ARGB_8888)
    Utils.matToBitmap(result, bitmap)

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

    bitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream)
    stream.close()

    val msg = "Save succeeded: ${uri.getPath()}"
    Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
  }

  companion object {
    private const val TAG = "CameraXApp"
    private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
  }
}

