package com.example.newsworth.ui.view

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.newsworth.R
import com.example.newsworth.databinding.ActivityPdfViewerBinding
import com.github.barteksc.pdfviewer.PDFView
import com.github.barteksc.pdfviewer.scroll.DefaultScrollHandle

class PdfViewerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPdfViewerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPdfViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.backButton.setOnClickListener {
            onBackPressed()

        }
        val pdfView: PDFView = findViewById(R.id.pdfView)
        pdfView.fromAsset("terms_and_conditions.pdf")
            .defaultPage(0)
            .enableSwipe(true)
            .swipeHorizontal(false)
            .onTap { e -> true }
            .scrollHandle(DefaultScrollHandle(this))
            .load()
    }
}
