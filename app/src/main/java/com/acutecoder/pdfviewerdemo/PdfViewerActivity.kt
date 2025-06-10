package com.acutecoder.pdfviewerdemo

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.*
import android.widget.Toast
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

class PdfViewerActivity : BaseActivity() {

    // Modificar tiempo en PDFs
    override val inactivityTimeout: Long = 120 * 1000

    private lateinit var view: ActivityPdfViewerBinding
    private lateinit var pdfSettingsManager: PdfSettingsManager

    // Corrutina que se ejecuta en el hilo principal con un SupervisorJob
    // Sirve para lanzar tareas seguras que se cancelan automáticamente en onDestroy
    private val activityScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // Flag que indica si el visor PDF está listo
    private var isViewerReady = false

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

        val (filePath, fileName) = getDataFromIntent() ?: run {
            toast("No source available!")
            finish()
            return
        }

        view.pdfViewer.onReady {
            isViewerReady = true
            defaultPageScale = PdfViewer.Zoom.AUTOMATIC.floatValue
            pdfSettingsManager.restore(this)

            // Uso de corrutinas para evitar bloquear el hilo principal al esperar al visor
            activityScope.launch {
                waitUntilViewerIsReady()
                view.pdfViewer.load(filePath)
                view.pdfToolBar.setFileName(fileName)
            }
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
    }

    // Corrutina que espera a que el visor esté listo sin bloquear el hilo principal
    private suspend fun waitUntilViewerIsReady() {
        while (!isViewerReady) {
            delay(50)
        }
    }

    override fun onPause() {
        pdfSettingsManager.save(view.pdfViewer)
        super.onPause()
    }

    // Cancelamos las corrutinas lanzadas para evitar fugas de memoria si la activity se destruye
    override fun onDestroy() {
        activityScope.cancel() // 🔒 Seguridad extra contra memory leaks
        super.onDestroy()
    }

    private fun getDataFromIntent(): Pair<String, String>? {
        val filePath = intent.getStringExtra("filePath") ?: return null
        val fileName = intent.getStringExtra("fileName") ?: return null
        return filePath to fileName
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}
