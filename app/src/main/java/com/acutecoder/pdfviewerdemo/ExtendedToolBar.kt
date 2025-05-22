package com.acutecoder.pdfviewerdemo

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.PopupMenu
import com.acutecoder.pdf.PdfUnstableApi
import com.acutecoder.pdf.PdfViewer
import com.acutecoder.pdf.ui.PdfToolBar
import com.acutecoder.pdfviewerdemo.databinding.ZoomLimitDialogBinding
import kotlin.math.roundToInt

/**
 * Barra de herramientas extendida para el visor PDF
 * Añade opciones personalizadas al menú como:
 * - Abrir PDF en otra app
 * - Establecer límites de zoom
 * - Habilitar/deshabilitar límite de velocidad de scroll
 */
class ExtendedToolBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : PdfToolBar(context, attrs, defStyleAttr) {

    /**
     * Crea el menú emergente (PopupMenu) con nuevas opciones:
     * - Si el archivo no es de los assets, añade "Open in other app"
     * - Opción para establecer límites de zoom
     * - Opción para activar/desactivar el límite de velocidad de scroll
     */
    @OptIn(PdfUnstableApi::class)
    override fun getPopupMenu(anchorView: View): PopupMenu {
        return PopupMenu(context, anchorView).apply {
            // Los IDs 0-9 ya están usados por la clase base

            // Añade la opción "Open in other app" si el PDF no está en assets
            if (pdfViewer.currentSource?.startsWith("file:///android_asset") == false)
                menu.add(Menu.NONE, 10, Menu.NONE, "Open in other app")

            // Opción para abrir el diálogo de límite de zoom
            menu.add(Menu.NONE, 11, Menu.NONE, "Zoom Limit")

            // Opción para activar/desactivar límite de scroll, depende del estado actual
            menu.add(
                Menu.NONE,
                12,
                Menu.NONE,
                (if (pdfViewer.scrollSpeedLimit == PdfViewer.ScrollSpeedLimit.None) "Enable" else "Disable")
                        + " scroll speed limit"
            )

            // Añade los menús por defecto definidos en la clase base
            addDefaultMenus(this)
        }
    }

    /**
     * Maneja los clics sobre los elementos del menú emergente
     * - 10: Abrir en otra app
     * - 11: Mostrar diálogo para establecer límites de zoom
     * - 12: Alternar límite de velocidad de scroll
     */
    @OptIn(PdfUnstableApi::class)
    override fun handlePopupMenuItemClick(item: MenuItem): Boolean {
        // Si la clase base ya lo maneja, no se continúa
        if (super.handlePopupMenuItemClick(item)) return true

        return when (item.itemId) {
            10 -> {
                // Crea un intent para abrir el archivo PDF en otra aplicación
                val uri = Uri.parse(pdfViewer.currentSource)
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, uri).apply {
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                )
                true
            }

            11 -> {
                // Muestra el diálogo personalizado para establecer límites de zoom
                showZoomLimitDialog()
                true
            }

            12 -> {
                // Alterna entre activar y desactivar el límite de velocidad de scroll
                if (pdfViewer.scrollSpeedLimit == PdfViewer.ScrollSpeedLimit.None)
                    pdfViewer.scrollSpeedLimit = PdfViewer.ScrollSpeedLimit.AdaptiveFling()
                else
                    pdfViewer.scrollSpeedLimit = PdfViewer.ScrollSpeedLimit.None
                true
            }

            else -> false // Para otros IDs, no se hace nada
        }
    }

    /**
     * Muestra un diálogo personalizado para establecer los límites de zoom
     * - Usa ZoomLimitDialogBinding para inflar el layout
     * - Los valores se ajustan con una SeekBar personalizada
     */
    private fun showZoomLimitDialog() {
        // Infla el diseño del diálogo desde el binding generado
        val view = ZoomLimitDialogBinding.inflate(LayoutInflater.from(context))

        // Configura los valores actuales en la SeekBar
        view.zoomSeekbar.apply {
            currentMinValue = (pdfViewer.minPageScale * 100).roundToInt()
            currentMaxValue = (pdfViewer.maxPageScale * 100).roundToInt()
        }

        // Crea y muestra el diálogo
        alertDialogBuilder()
            .setTitle("Zoom Limit")
            .setView(view.root)
            .setPositiveButton("Done") { dialog, _ ->
                dialog.dismiss()
                // Aplica los nuevos valores al visor PDF
                view.zoomSeekbar.let {
                    pdfViewer.minPageScale = it.currentMinValue / 100f
                    pdfViewer.maxPageScale = it.currentMaxValue / 100f
                    pdfViewer.scalePageTo(pdfViewer.currentPageScale)
                }
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }
}
