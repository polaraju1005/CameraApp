package com.example.cameraxapp
import android.annotation.SuppressLint
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.util.Base64
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import com.example.cameraxapp.databinding.ActivityImageFilterBinding
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Environment
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer

const val EXTRA_OCR_TEXT = "com.example.cameraxapp.OCR_TEXT"

class ImageFilter : AppCompatActivity() {
  private lateinit var viewBinding: ActivityImageFilterBinding
  private var original: Mat? = null
  private var result: Mat? = null
  @SuppressLint("WrongThread")
  @RequiresApi(Build.VERSION_CODES.P)
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    viewBinding = ActivityImageFilterBinding.inflate(layoutInflater)
    setContentView(viewBinding.root)
    deleteInternalStorageDirectoryy()
    viewBinding.BWFilterButton.setOnClickListener {
      doBWFilter()
    }
    viewBinding.GrayscaleFilterButton.setOnClickListener {
      doGrayscaleFilter()
    }
    viewBinding.EnhanceFilterButton.setOnClickListener {
      deleteInternalStorageDirectoryy()
      doSaveGetSave()
      val input_path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString() + "/CameraX-Image-Input/"
      val output_path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString() + "/CameraX-Image-Output/"
      val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString() + "/CameraX-Image-Output/")
      if(!file.exists()){
        file.mkdirs()
      }
      val input = input_path + "cropped_image" + ".jpg"
      if (!Python.isStarted()) {
        Python.start(AndroidPlatform(this))
      }
      val py = Python.getInstance()
      val module = py.getModule("script")

      val fact = module["process_and_enhance_image"]
      fact?.call(input,output_path)
      val f=File(output_path,"enhanced_image.jpg")
      System.out.println("122334465=")
      val b=BitmapFactory.decodeStream(FileInputStream(f))

      viewBinding.imageView2.setImageBitmap(b)
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
    viewBinding.OCRButton.setOnClickListener {
      doOCR()
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
    original = mat.clone()
    result = mat.clone()
    val resultBitmap = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888)
    Utils.matToBitmap(mat, resultBitmap)

    doSaveGetSave()
    doEnhancePythonFilter()
   // viewBinding.imageView2.setImageBitmap(resultBitmap)
  }

  override fun onDestroy() {
    super.onDestroy()
    deleteInternalStorageDirectoryy()
  }
  private fun doEnhancePythonFilter()
  {


    val input_path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString() + "/CameraX-Image-Input/"
    val output_path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString() + "/CameraX-Image-Output/"
    val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString() + "/CameraX-Image-Output/")
    if(!file.exists()){
      file.mkdirs()
    }
    val input = input_path + "cropped_image" + ".jpg"
    if (!Python.isStarted()) {
      Python.start(AndroidPlatform(this))
    }
    val py = Python.getInstance()
    val module = py.getModule("script")

    val fact = module["process_and_enhance_image"]
    fact?.call(input,output_path)
    val f=File(output_path,"enhanced_image.jpg")
    System.out.println("122334465=")
    val b=BitmapFactory.decodeStream(FileInputStream(f))

    viewBinding.imageView2.setImageBitmap(b)
  }

  private fun doBWFilter() {
    val original = original ?: return
    val result = result ?: return
    CVLib.doFilterBW(original.nativeObjAddr, result.nativeObjAddr)
    val resultBitmap = Bitmap.createBitmap(result.cols(), result.rows(), Bitmap.Config.ARGB_8888)
    Utils.matToBitmap(result, resultBitmap)
    viewBinding.imageView2.setImageBitmap(resultBitmap)
  }
  private fun doGrayscaleFilter() {
    val original = original ?: return
    val result = result ?: return
    CVLib.doFilterGrayscale(original.nativeObjAddr, result.nativeObjAddr)
    val resultBitmap = Bitmap.createBitmap(result.cols(), result.rows(), Bitmap.Config.ARGB_8888)
    Utils.matToBitmap(result, resultBitmap)
    viewBinding.imageView2.setImageBitmap(resultBitmap)
  }
  private fun doEnhanceFilter() {
    val original = original ?: return
    val result = result ?: return
    CVLib.doFilterEnhance(original.nativeObjAddr, result.nativeObjAddr)
    val resultBitmap = Bitmap.createBitmap(result.cols(), result.rows(), Bitmap.Config.ARGB_8888)
    Utils.matToBitmap(result, resultBitmap)
    viewBinding.imageView2.setImageBitmap(resultBitmap)
  }
  private fun doSoftFilter() {
    val original = original ?: return
    val result = result ?: return
    CVLib.doFilterSoft(original.nativeObjAddr, result.nativeObjAddr)
    val resultBitmap = Bitmap.createBitmap(result.cols(), result.rows(), Bitmap.Config.ARGB_8888)
    Utils.matToBitmap(result, resultBitmap)
    viewBinding.imageView2.setImageBitmap(resultBitmap)
  }
  private fun doNoFilter() {
    val original = original ?: return
    result = original.clone()
    val resultBitmap =
      Bitmap.createBitmap(original.cols(), original.rows(), Bitmap.Config.ARGB_8888)
    Utils.matToBitmap(original, resultBitmap)
    viewBinding.imageView2.setImageBitmap(resultBitmap)
  }
  private fun doOCR() {
    val result = result ?: return
    val resultBitmap = Bitmap.createBitmap(result.cols(), result.rows(), Bitmap.Config.ARGB_8888)
    Utils.matToBitmap(result, resultBitmap)
// https://developers.google.com/ml-kit/vision/text-recognition/android
    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    val image = InputImage.fromBitmap(resultBitmap, 0)
    recognizer.process(image)
      .addOnSuccessListener { visionText ->
// Task completed successfully
// ...
        val msg = "Recognition success!"
        Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
        val intent = Intent(this, OCRResultViewer::class.java).apply {
          putExtra(EXTRA_OCR_TEXT, visionText.getText())
        }
        startActivity(intent)
      }
      .addOnFailureListener { e ->
// Task failed with an exception
// ...
        val msg = "Recognition failed: ${e}"
        Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
      }
  }

  private fun doSave() {
    val result = result ?: return
    val bitmap = Bitmap.createBitmap(result.cols(), result.rows(), Bitmap.Config.ARGB_8888)
    Utils.matToBitmap(result, bitmap)
    val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US)
      .format(System.currentTimeMillis())
    val contentValues = ContentValues().apply {
      put(MediaStore.MediaColumns.DISPLAY_NAME, name)
      put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
      if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
        put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraX-Image")
      }
    }
    val uri = contentResolver.insert(
      MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
      contentValues
    ) ?: throw IOException("Could not open uri")
    val stream =
      contentResolver.openOutputStream(uri) ?: throw IOException("Could not open output stream")
    bitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream)
    stream.close()
    val msg = "Save succeeded: ${uri.getPath()}"
    Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
  }
  private fun doSaveGetSave() {

    val original = original ?: return
    result = original.clone()
    val bitmap =
      Bitmap.createBitmap(original.cols(), original.rows(), Bitmap.Config.ARGB_8888)
    Utils.matToBitmap(original, bitmap)

    val name = "cropped_image"
    val contentValues = ContentValues().apply {
      put(MediaStore.MediaColumns.DISPLAY_NAME, name)
      put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
      if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
        put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraX-Image-Input")
      }
    }


   val uri = contentResolver.insert(
      MediaStore.Images.Media.EXTERNAL_CONTENT_URI,contentValues
   ) ?: throw IOException("Could not open uri")
//    val stream = contentResolver.openOutputStream(uri) ?: throw IOException("Could not open output stream")
//
//// Create a buffer to hold the bitmap's pixels
//    val byteBuffer = ByteBuffer.allocate(bitmap.byteCount)
//    bitmap.copyPixelsToBuffer(byteBuffer)
//    byteBuffer.rewind()
//
//// Write the buffer's contents to the output stream
//    stream.write(byteBuffer.array())
//
//    stream.close()
    val stream =
      contentResolver.openOutputStream(uri) ?: throw IOException("Could not open output stream")
    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
    stream.close()
//val msg = "Save succeeded: ${uri.getPath()}"
//Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
  }

  fun deleteInternalStorageDirectoryy() {
    if (ContextCompat.checkSelfPermission(
        this@ImageFilter,
        android.Manifest.permission.READ_EXTERNAL_STORAGE
      ) == PackageManager.PERMISSION_DENIED
    ) {
      val input_path = File(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
          .toString() + "/CameraX-Image-Input/"
      )
      val output_pathh = File(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
          .toString() + "/CameraX-Image-Output/"
      )
      if (input_path.exists()) {
        input_path.deleteRecursively()
      }
      if (output_pathh.exists()) {
        output_pathh.deleteRecursively()
      }
    } else {
      requestRuntimePermissionn()
    }
  }
  private fun requestRuntimePermissionn(): Boolean {
    if (ActivityCompat.checkSelfPermission(
        this@ImageFilter,
        android.Manifest.permission.READ_EXTERNAL_STORAGE
      ) != PackageManager.PERMISSION_GRANTED
    ) {
      ActivityCompat.requestPermissions(
        this@ImageFilter,
        arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE),
        14
      )
      return false
    }
    return true
  }

  override fun onRequestPermissionsResult(
    requestCode: Int,
    permissions: Array<out String>,
    grantResults: IntArray
  ) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    when (requestCode) {
      14 -> {
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
          Toast.makeText(this@ImageFilter, "Permission Granted", Toast.LENGTH_LONG)
            .show()
        } else {
          ActivityCompat.requestPermissions(
            this@ImageFilter,
            arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE),
            14
          )
        }
      }
    }
  }

  override fun onBackPressed() {
    // Start MainActivity
    val intent = Intent(this, MainActivity::class.java)
    startActivity(intent)

    // Optional: if you want to finish the current activity
    finish()
  }

  companion object {
    private const val TAG = "CameraXApp"
    private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
  }
}