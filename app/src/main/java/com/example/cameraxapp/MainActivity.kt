package com.example.cameraxapp

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.PointF
import android.media.Image
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import android.widget.Toast
import androidx.camera.lifecycle.ProcessCameraProvider
import android.util.Log
import androidx.camera.core.*
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.VideoRecordEvent
import androidx.core.content.PermissionChecker
import com.example.cameraxapp.databinding.ActivityMainBinding
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc
import java.io.File
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.Locale

typealias CVAnalyzerListener = () -> Unit 

const val EXTRA_PICTURE_URI = "com.example.cameraxapp.PICTURE_URI"
const val EXTRA_PICTURE_TYPE = "com.example.cameraxapp.PICTURE_TYPE"

class MainActivity : AppCompatActivity() {
  private lateinit var viewBinding: ActivityMainBinding

  private var imageCapture: ImageCapture? = null

  private var videoCapture: VideoCapture<Recorder>? = null
  private var recording: Recording? = null

  private var bitmap: Bitmap? = null
  private var mask: Mat? = null

  private lateinit var cameraExecutor: ExecutorService

  private var documentBorders : DocumentBorders? = null
  private var documentImageWidth : Int = 0
  private var documentImageHeight : Int = 0



  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    viewBinding = ActivityMainBinding.inflate(layoutInflater)
    setContentView(viewBinding.root)

    supportActionBar?.hide()

    // Request camera permissions
    if (allPermissionsGranted()) {
      startCamera()
    } else {
      ActivityCompat.requestPermissions(
        this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
      }

      // Set up the listeners for take photo and video capture buttons
      viewBinding.imageCaptureButton.setOnClickListener { takePhoto() }

      cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun takePhoto() {
      // Get a stable reference of the modifiable image capture use case
      val imageCapture = imageCapture ?: return

      // Create time stamped name and MediaStore entry.
      val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US)
      .format(System.currentTimeMillis())
      val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, name)
        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
          put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraX-Image")
        }
      }

      // Create output options object which contains file + metadata
      val outputOptions = ImageCapture.OutputFileOptions
      .Builder(contentResolver,
      MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
      contentValues)
      .build()

      // Set up image capture listener, which is triggered after photo has
      // been taken
      imageCapture.takePicture(
        outputOptions,
        ContextCompat.getMainExecutor(this),
        object : ImageCapture.OnImageSavedCallback {
          override fun onError(exc: ImageCaptureException) {
            Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
          }

          override fun
          onImageSaved(output: ImageCapture.OutputFileResults){
            val msg = "Photo capture succeeded: ${output.savedUri}"
            // Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
            // Log.d(TAG, msg)
            val uri = output.savedUri ?: return
            startViewer(uri)
          }
        }
      )
    }

    private fun startViewer(uri : Uri) {
      var pictureType = ""
      if(viewBinding.radioDocument.isChecked) {
        pictureType = "document"
      }

      if(viewBinding.radioIDCard.isChecked) {
        pictureType = "idcard"
      }

      if(viewBinding.radioBook.isChecked) {
        pictureType = "book"
      }

      val intent = Intent(this, ImageViewer::class.java).apply {
        putExtra(EXTRA_PICTURE_URI, uri.toString())
        putExtra(EXTRA_PICTURE_TYPE, pictureType)
      }
      startActivity(intent)
    }

    private fun captureVideo() {}

    private fun startCamera() {
      val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

      cameraProviderFuture.addListener({
        // Used to bind the lifecycle of cameras to the lifecycle owner
        val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

        // Preview
        val preview = Preview.Builder()
          .build()
          .also {
            it.setSurfaceProvider(viewBinding.viewFinder.surfaceProvider)
          }

        imageCapture = ImageCapture.Builder()
          .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
          .build()

        // Analyzer
        val analyzer = ImageAnalysis.Builder()
        .build()
        .also {
          it.setAnalyzer(cameraExecutor, CVAnalyzer {
            runOnUiThread(Runnable {  
              if(documentBorders != null) {
                viewBinding.rectOverlay.setDocumentBorders(documentBorders, documentImageWidth, documentImageHeight)
              } else {
                viewBinding.rectOverlay.clear();
              }

              if(bitmap != null) {
                viewBinding.rectOverlay.setBitmap(bitmap)
              }
            })
          })
        }

        // Select back camera as a default
        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        try {
          // Unbind use cases before rebinding
          cameraProvider.unbindAll()

          // Bind use cases to camera
          cameraProvider.bindToLifecycle(
            this, cameraSelector, preview, analyzer, imageCapture)

          } catch(exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
          }

        }, ContextCompat.getMainExecutor(this))
      }


      private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
          baseContext, it) == PackageManager.PERMISSION_GRANTED
      }

        override fun onRequestPermissionsResult(
          requestCode: Int, permissions: Array<String>, grantResults:
          IntArray) {
            if (requestCode == REQUEST_CODE_PERMISSIONS) {
              if (allPermissionsGranted()) {
                startCamera()
              } else {
                Toast.makeText(this,
                "Permissions not granted by the user.",
                Toast.LENGTH_SHORT).show()
                finish()
              }
            }
          }


          override fun onDestroy() {
            super.onDestroy()
            cameraExecutor.shutdown()
          }

          override fun onResume() {
            super.onResume()
            if (!OpenCVLoader.initDebug()) {
              Log.d(TAG, "ERROR OpenCV library not found.")
            } else {
              CVLib.initLib()
              Log.d(TAG, "OpenCV library found inside package. Using it!")
            }
          }

          private inner class CVAnalyzer(private val listener : CVAnalyzerListener) : ImageAnalysis.Analyzer {

            private var pcnt: Mat? = null

            private var acnt: Mat? = null

            private var success_counter : Int = 0
            private var frame_counter : Int = 0


            // [Reference] https://stackoverflow.com/questions/58102717/android-camerax-analyzer-image-with-format-yuv-420-888-to-opencv-mat
            // Ported from opencv private class JavaCamera2Frame
            private fun Image.yuvToRgba(): Mat {
              val rgbaMat = Mat()

              if (format == ImageFormat.YUV_420_888
              && planes.size == 3) {

                val chromaPixelStride = planes[1].pixelStride

                if (chromaPixelStride == 2) { // Chroma channels are interleaved
                assert(planes[0].pixelStride == 1)
                assert(planes[2].pixelStride == 2)
                val yPlane = planes[0].buffer
                val uvPlane1 = planes[1].buffer
                val uvPlane2 = planes[2].buffer
                val yMat = Mat(height, width, CvType.CV_8UC1, yPlane)
                val uvMat1 = Mat(height / 2, width / 2, CvType.CV_8UC2, uvPlane1)
                val uvMat2 = Mat(height / 2, width / 2, CvType.CV_8UC2, uvPlane2)
                val addrDiff = uvMat2.dataAddr() - uvMat1.dataAddr()
                if (addrDiff > 0) {
                  assert(addrDiff == 1L)
                  Imgproc.cvtColorTwoPlane(yMat, uvMat1, rgbaMat, Imgproc.COLOR_YUV2RGBA_NV12)
                } else {
                  assert(addrDiff == -1L)
                  Imgproc.cvtColorTwoPlane(yMat, uvMat2, rgbaMat, Imgproc.COLOR_YUV2RGBA_NV21)
                }
              } else { // Chroma channels are not interleaved
              val yuvBytes = ByteArray(width * (height + height / 2))
              val yPlane = planes[0].buffer
              val uPlane = planes[1].buffer
              val vPlane = planes[2].buffer

              yPlane.get(yuvBytes, 0, width * height)

              val chromaRowStride = planes[1].rowStride
              val chromaRowPadding = chromaRowStride - width / 2

              var offset = width * height
              if (chromaRowPadding == 0) {
                // When the row stride of the chroma channels equals their width, we can copy
                // the entire channels in one go
                uPlane.get(yuvBytes, offset, width * height / 4)
                offset += width * height / 4
                vPlane.get(yuvBytes, offset, width * height / 4)
              } else {
                // When not equal, we need to copy the channels row by row
                for (i in 0 until height / 2) {
                  uPlane.get(yuvBytes, offset, width / 2)
                  offset += width / 2
                  if (i < height / 2 - 1) {
                    uPlane.position(uPlane.position() + chromaRowPadding)
                  }
                }
                for (i in 0 until height / 2) {
                  vPlane.get(yuvBytes, offset, width / 2)
                  offset += width / 2
                  if (i < height / 2 - 1) {
                    vPlane.position(vPlane.position() + chromaRowPadding)
                  }
                }
              }

              val yuvMat = Mat(height + height / 2, width, CvType.CV_8UC1)
              yuvMat.put(0, 0, yuvBytes)
              Imgproc.cvtColor(yuvMat, rgbaMat, Imgproc.COLOR_YUV2RGBA_I420, 4)
            }
          }

          return rgbaMat
        }
        override fun analyze(imageProxy: ImageProxy) {
          val mat = imageProxy.image?.yuvToRgba();
          val rotation = imageProxy.imageInfo.rotationDegrees
          imageProxy.close()
          mat?.let {
            val result = processImg(mat, rotation)
            Log.d(TAG, "cv width ${result.cols()} height ${result.rows()}")

            if(bitmap == null) {
              bitmap = Bitmap.createBitmap(mask!!.cols(), mask!!.rows(), Bitmap.Config.ARGB_8888)
            }
            Utils.matToBitmap(mask, bitmap)
            listener()
          }
        }

        fun processImg(mat: Mat, rotation: Int) : Mat {
          // get current camera frame as OpenCV Mat object
          Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGBA2RGB)

          val resized = Mat()

          CVLib.getReducedImage(mat.nativeObjAddr, resized.nativeObjAddr)

          mask = Mat()
          CVLib.getDocumentMask(resized.nativeObjAddr, mask!!.nativeObjAddr)

          val cnt = Mat()
          CVLib.getDocumentContour(mask!!.nativeObjAddr, cnt.nativeObjAddr)

          val approxCnt = Mat()
          val found = CVLib.getDocumentApproxContour(cnt.nativeObjAddr, approxCnt.nativeObjAddr)


          if(found) {
            if(pcnt == null) {
              pcnt = Mat()
            }

            val is_valid = CVLib.isContourValid(approxCnt.nativeObjAddr, pcnt!!.nativeObjAddr)
            pcnt = approxCnt;

            if(is_valid) {
              if(acnt == null) {
                acnt = Mat()
              }

              CVLib.addContour(approxCnt.nativeObjAddr, acnt!!.nativeObjAddr)
              success_counter += 1
            }

          }

          if(frame_counter >= 30) {
            frame_counter = 0;
            if(success_counter > 20) {
              val bcnt = Mat()
              CVLib.divContour(acnt!!.nativeObjAddr, bcnt.nativeObjAddr, success_counter)

              val resizedCnt = Mat()
              CVLib.resizeContour(mat.nativeObjAddr, resized.nativeObjAddr, bcnt.nativeObjAddr, resizedCnt.nativeObjAddr)

              // This is ugly but what can you do
              documentBorders = DocumentBorders(resizedCnt)
              when(rotation) {
                0 -> {
                  documentImageWidth = mat.cols()
                  documentImageHeight = mat.rows()
                }
                90 -> {
                  documentBorders?.rotate(DocumentBorders.Rotation.ROTATE_90, mat.cols(), mat.rows())

                  documentImageWidth = mat.rows()
                  documentImageHeight = mat.cols()
                }
                180 -> {
                  documentImageWidth = mat.cols()
                  documentImageHeight = mat.rows()
                }
                270 -> {
                  documentBorders?.rotate(DocumentBorders.Rotation.ROTATE_180, mat.cols(), mat.rows())

                  documentImageWidth = mat.rows()
                  documentImageHeight = mat.cols()
                }
              }

            } else {
              documentBorders = null
            }
            acnt = Mat()

            success_counter = 0;
          }
          frame_counter += 1


          System.gc()
          return mat
        }
      }
  companion object {
    private const val TAG = "CameraXApp"
    private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
    private const val REQUEST_CODE_PERMISSIONS = 10
    private val REQUIRED_PERMISSIONS =
    mutableListOf (
      Manifest.permission.CAMERA,
    ).apply {
      if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
        add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
      }
    }.toTypedArray()
  }

}

