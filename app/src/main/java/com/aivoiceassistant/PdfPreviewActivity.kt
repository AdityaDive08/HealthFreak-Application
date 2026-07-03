package com.aivoiceassistant

import android.content.ContentValues
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

class PdfPreviewActivity : AppCompatActivity() {

    private lateinit var txtTranscript: TextView
    private lateinit var btnDownloadPdf: Button
    private lateinit var btnBack: ImageView
    private var transcriptText: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pdf_preview)

        txtTranscript = findViewById(R.id.txtTranscript)
        btnDownloadPdf = findViewById(R.id.btnDownloadPdf)
        btnBack = findViewById(R.id.btnBack)

        transcriptText = intent.getStringExtra("EXTRA_CHAT_TEXT") ?: "No conversation data."
        txtTranscript.text = transcriptText

        btnBack.setOnClickListener {
            finish()
        }

        btnDownloadPdf.setOnClickListener {
            generateAndSavePdf()
        }
    }

    private fun generateAndSavePdf() {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 Size approx
        val page = pdfDocument.startPage(pageInfo)

        val canvas: Canvas = page.canvas
        val paint = Paint()
        paint.color = Color.BLACK
        paint.textSize = 12f
        
        // Draw Header
        val titlePaint = Paint()
        titlePaint.color = Color.parseColor("#1D4ED8")
        titlePaint.textSize = 18f
        titlePaint.isFakeBoldText = true
        canvas.drawText("AI Doctor Consultation Summary", 50f, 50f, titlePaint)

        // Draw Date
        val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        paint.textSize = 10f
        canvas.drawText("Date: $dateStr", 50f, 75f, paint)

        // Draw Transcript (Handling multi-line)
        paint.textSize = 12f
        val startX = 50f
        var startY = 110f
        val lineSpacing = 16f
        
        val lines = transcriptText.split("\n")
        for (line in lines) {
            // Very simple text wrapping (could be improved, but works for basic transcripts)
            var currentLine = line
            while (currentLine.length > 80) {
                val wrapIndex = currentLine.lastIndexOf(" ", 80)
                val splitIndex = if (wrapIndex > 0) wrapIndex else 80
                
                canvas.drawText(currentLine.substring(0, splitIndex), startX, startY, paint)
                startY += lineSpacing
                currentLine = currentLine.substring(splitIndex).trim()
                
                if (startY > 800f) break // Prevent drawing off the page
            }
            if (startY > 800f) break
            
            canvas.drawText(currentLine, startX, startY, paint)
            startY += lineSpacing
        }

        pdfDocument.finishPage(page)

        val fileName = "Consultation_${System.currentTimeMillis()}.pdf"
        var outputStream: OutputStream? = null

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                if (uri != null) {
                    outputStream = contentResolver.openOutputStream(uri)
                }
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val file = File(downloadsDir, fileName)
                outputStream = FileOutputStream(file)
            }

            outputStream?.let {
                pdfDocument.writeTo(it)
                Toast.makeText(this, "PDF Saved to Downloads!", Toast.LENGTH_LONG).show()
            } ?: run {
                Toast.makeText(this, "Failed to create file.", Toast.LENGTH_SHORT).show()
            }

        } catch (e: Exception) {
            Toast.makeText(this, "Error saving PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        } finally {
            pdfDocument.close()
            outputStream?.close()
        }
    }
}
