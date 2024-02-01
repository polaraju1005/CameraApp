package com.example.cameraxapp

import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.*
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import com.example.cameraxapp.databinding.ActivityIdcardViewerBinding
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class IDCardViewer : AppCompatActivity() {

  private lateinit var viewBinding: ActivityIdcardViewerBinding
  private var original : Mat? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    viewBinding = ActivityIdcardViewerBinding.inflate(layoutInflater)
    setContentView(viewBinding.root)

    val uri = Uri.parse(intent.getStringExtra(EXTRA_PICTURE_URI))
    val imageDecoder = ImageDecoder.createSource(contentResolver, uri)
    val bitmap = ImageDecoder.decodeBitmap(imageDecoder)

    val bmp32 = bitmap.copy(Bitmap.Config.ARGB_8888, true);

    contentResolver.delete(uri, null, null)

    original = Mat()
    Utils.bitmapToMat(bmp32, original)

    // get current camera frame as OpenCV Mat object
    Imgproc.cvtColor(original, original, Imgproc.COLOR_RGBA2RGB)

    viewBinding.IDCardOverlay.setBitmap(bmp32)

    val width = bmp32.width
    val height = bmp32.height

    viewBinding.IDCardOverlay.post(Runnable {
      viewBinding.IDCardOverlay.setCenter(Point(
        viewBinding.IDCardOverlay.width/2,
        viewBinding.IDCardOverlay.height/2)
      )

      // compute card width at initialization
      val cardWidth = viewBinding.IDCardOverlay.width/2
      val cardHeight = height * cardWidth / width

      viewBinding.IDCardOverlay.setSize(
        Point(cardWidth, cardHeight))
    })

    viewBinding.SaveButton.setOnClickListener {
      doSave()
    }

  }

  private fun doSave()
  {
    val result = Mat()
    CVLib.createWhiteBackground(result.nativeObjAddr, 
      viewBinding.IDCardOverlay.width,
      viewBinding.IDCardOverlay.height)

    CVLib.blitImage(result.nativeObjAddr, 
      original!!.nativeObjAddr,
      viewBinding.IDCardOverlay.getCenter().x,
      viewBinding.IDCardOverlay.getCenter().y,
      viewBinding.IDCardOverlay.getSize().x,
      viewBinding.IDCardOverlay.getSize().y
    )

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

