package com.example.cameraxapp

import com.example.cameraxapp.databinding.ActivityOcrresultViewerBinding
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

class OCRResultViewer : AppCompatActivity() {

  private lateinit var viewBinding: ActivityOcrresultViewerBinding
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    viewBinding = ActivityOcrresultViewerBinding.inflate(layoutInflater)
    setContentView(viewBinding.root)

    viewBinding.textView.text = intent.getStringExtra(EXTRA_OCR_TEXT)
  }
}

