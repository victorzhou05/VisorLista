package com.acutecoder.pdfviewerdemo

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.*
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.acutecoder.pdf.*
import com.acutecoder.pdf.setting.PdfSettingsManager
import com.acutecoder.pdf.sharedPdfSettingsManager
import com.acutecoder.pdfviewerdemo.databinding.ActivityPdfViewerBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jaredrummler.android.colorpicker.ColorPickerDialog
import com.jaredrummler.android.colorpicker.ColorPickerDialogListener
import kotlinx.coroutines.*
import com.acutecoder.pdfviewerdemo.utils.setFullscreen


class PdfViewerActivity : AppCompatActivity() {

    private lateinit var view: ActivityPdfViewerBinding
    private var fullscreen = false
    private lateinit var pdfSettingsManager: PdfSettingsManager

    // ----- Control de inactividad -----
    private val INACTIVITY_TIMEOUT = 120_000L // 2 minutos
    private val inactivityHandler = Handler(Looper.getMainLooper())
    private val inactivityRunnable = Runnable {
        goToInactivityScreen()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setFullscreen(true)

        view = ActivityPdfViewerBinding.inflate(layoutInflater)
        setContentView(view.root)

        ViewCompat.setOnApplyWindowInsetsListener(view.container) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        pdfSettingsManager = sharedPdfSettingsManager("PdfSettings", MODE_PRIVATE).also {
            it.excludeAll()
        }

        // Obtenemos los datos del intent, necesarios para cargar el PDF
        val (filePath, fileName) = getDataFromIntent() ?: run {
            toast("No source available!")
            finish()
            return
        }

        view.pdfViewer.onReady {
            defaultPageScale = PdfViewer.Zoom.PAGE_FIT.floatValue
            pdfSettingsManager.restore(this)

            // Cargamos el archivo PDF desde el intent
            load(filePath)
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
        view.container.setAsLoadingIndicator(view.loader)

        onBackPressedDispatcher.addCallback(this) {
            if (view.pdfToolBar.isFindBarVisible())
                view.pdfToolBar.setFindBarVisible(false)
            else
                finish()
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
                view.pdfViewer.callSafely {
                    fullscreen = !fullscreen
                    setFullscreen(fullscreen)
                    view.container.animateToolBar(!fullscreen)
                }
            }
        })

        // Inicia el temporizador de inactividad
        resetInactivityTimer()
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        resetInactivityTimer()
    }

    private fun resetInactivityTimer() {
        inactivityHandler.removeCallbacks(inactivityRunnable)
        inactivityHandler.postDelayed(inactivityRunnable, INACTIVITY_TIMEOUT)
    }

    private fun goToInactivityScreen() {
        val intent = Intent(this, InactivoActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
        finish()
    }


    override fun onPause() {
        pdfSettingsManager.save(view.pdfViewer)
        super.onPause()
    }

    override fun onDestroy() {
        pdfSettingsManager.save(view.pdfViewer)
        inactivityHandler.removeCallbacks(inactivityRunnable)
        super.onDestroy()
    }

    private fun getDataFromIntent(): Pair<String, String>? {
        val filePath = intent.getStringExtra("filePath") ?: return null
        val fileName = intent.getStringExtra("fileName") ?: return null
        return filePath to fileName
    }
}
