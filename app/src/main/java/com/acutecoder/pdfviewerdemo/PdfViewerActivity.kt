package com.acutecoder.pdfviewerdemo

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.acutecoder.pdf.PdfListener
import com.acutecoder.pdf.PdfOnLinkClick
import com.acutecoder.pdf.PdfOnPageLoadFailed
import com.acutecoder.pdf.PdfUnstableApi
import com.acutecoder.pdf.PdfViewer
import com.acutecoder.pdf.callIfScrollSpeedLimitIsEnabled
import com.acutecoder.pdf.callSafely
import com.acutecoder.pdf.setting.PdfSettingsManager
import com.acutecoder.pdf.sharedPdfSettingsManager
import com.acutecoder.pdfviewerdemo.databinding.ActivityPdfViewerBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jaredrummler.android.colorpicker.ColorPickerDialog
import com.jaredrummler.android.colorpicker.ColorPickerDialogListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class PdfViewerActivity : AppCompatActivity() {

    private lateinit var view: ActivityPdfViewerBinding
    private var fullscreen = false
    private lateinit var pdfSettingsManager: PdfSettingsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        view = ActivityPdfViewerBinding.inflate(layoutInflater)
        setContentView(view.root)

        ViewCompat.setOnApplyWindowInsetsListener(view.container) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        pdfSettingsManager = sharedPdfSettingsManager("PdfSettings", MODE_PRIVATE)
            .also { it.excludeAll()}

        val (filePath, fileName) = getDataFromIntent() ?: run {
            toast("No source available!")
            finish()
            return
        }


        view.pdfViewer.onReady {
//            minPageScale = 5f
//            maxPageScale = 5f
            defaultPageScale = PdfViewer.Zoom.PAGE_FIT.floatValue
            pdfSettingsManager.restore(this)
            load(filePath)
            if (filePath.isNotBlank())
                view.pdfToolBar.setFileName(fileName)
        }

        view.pdfToolBar.alertDialogBuilder = { MaterialAlertDialogBuilder(this) }
        var selectedColor = Color.WHITE
        view.pdfToolBar.pickColor = { onPickColor ->
            ColorPickerDialog.newBuilder()
                .setColor(selectedColor)
                .create().apply {
                    setColorPickerDialogListener(object : ColorPickerDialogListener {
                        override fun onColorSelected(dialogId: Int, color: Int) {
                            selectedColor = color
                            onPickColor(color)
                        }

                        override fun onDialogDismissed(dialogId: Int) {}
                    })
                    show(supportFragmentManager, "color-picker-dialog")
                }
        }
        view.container.alertDialogBuilder = view.pdfToolBar.alertDialogBuilder
//        view.pdfViewer.addListener(DownloadPdfListener(fileName))
        view.container.setAsLoadingIndicator(view.loader)

        onBackPressedDispatcher.addCallback(this) {
            if (view.pdfToolBar.isFindBarVisible()) view.pdfToolBar.setFindBarVisible(false)
            else finish()
        }

        view.pdfViewer.run {
            highlightEditorColors = listOf("blue" to Color.BLUE, "black" to Color.BLACK)
            editor.highlightColor = Color.BLUE
            addListener(PdfOnPageLoadFailed {
                toast(it)
                finish()
            })
            addListener(PdfOnLinkClick { link ->
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link)))
            })
        }

        view.pdfViewer.addListener(object : PdfListener {
            @OptIn(PdfUnstableApi::class)
            override fun onSingleClick() {
                view.pdfViewer.callSafely { // Helpful if you are using scrollSpeedLimit or skip if editing pdf
                    fullscreen = !fullscreen
                    setFullscreen(fullscreen)
                    view.container.animateToolBar(!fullscreen)
                }
            }
        })
    }

    override fun onPause() {
        pdfSettingsManager.save(view.pdfViewer)
        super.onPause()
    }

    override fun onDestroy() {
        pdfSettingsManager.save(view.pdfViewer)
        super.onDestroy()
    }

//    inner class DownloadPdfListener(private val pdfTitle: String) : PdfListener {
//        private var bytes: ByteArray? = null
//        private val saveFileLauncher = registerForActivityResult(
//            ActivityResultContracts.StartActivityForResult()
//        ) { result ->
//            CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
//                bytes?.let { pdfAsBytes ->
//                    if (result.resultCode == Activity.RESULT_OK) {
//                        result.data?.data?.let { uri ->
//                            try {
//                                contentResolver.openOutputStream(uri)?.use { it.write(pdfAsBytes) }
//                            } catch (e: Exception) {
//                                e.printStackTrace()
//                            }
//                        }
//                    }
//                }
//            }
//        }
//
//        override fun onSavePdf(pdfAsBytes: ByteArray) {
//            bytes = pdfAsBytes
//
//            saveFileLauncher.launch(Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
//                addCategory(Intent.CATEGORY_OPENABLE)
//                type = "application/pdf"
//                putExtra(Intent.EXTRA_TITLE, pdfTitle)
//            })
//        }
//    }

}
