package com.acutecoder.pdfviewerdemo

import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.text.InputType
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.acutecoder.pdfviewerdemo.databinding.ActivityMainBinding
import com.google.gson.Gson
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.File
import java.util.Stack

import com.acutecoder.pdfviewerdemo.utils.setFullscreen
import com.google.gson.reflect.TypeToken


class MainActivity : BaseActivity() {

    private val secretPassoword = "iescierva"

    private val PICK_JSON_FILE = 1001
    private val navigationStack = Stack<List<Elemento>>()
    private val titleStack = Stack<String>()
    private val descriptionStack = Stack<String>()

    private lateinit var rootElementos: List<Elemento>

    private lateinit var recyclerView: RecyclerView
    private lateinit var toolbar: Toolbar
    private lateinit var btnHome: ImageButton
    private lateinit var btnBack: ImageButton
    private lateinit var btnConfig: ImageButton
    private lateinit var tituloToolbar: TextView
    private lateinit var descripcionToolbar: TextView

    private lateinit var adapter: ElementoAdapter
    private lateinit var sinElementos: TextView
    private lateinit var viewBinding: ActivityMainBinding

    private lateinit var pref: SharedPreferences
    private val PREF_KEY_JSON_URI = "jsonFileUri"


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        pref = getSharedPreferences("pref", MODE_PRIVATE)
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        setFullscreen(true)

        recyclerView = findViewById(R.id.recyclerView)
        toolbar = findViewById(R.id.toolbarMain)
        btnHome = findViewById(R.id.btnHome)
        btnBack = findViewById(R.id.btnBack)
        tituloToolbar = findViewById(R.id.toolbarTitle)
        descripcionToolbar = findViewById(R.id.toolbarDescription)

        btnConfig = findViewById(R.id.btnConfig)

        sinElementos = findViewById(R.id.sinElementos)
        sinElementos.visibility = View.INVISIBLE
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.setHasFixedSize(false)
        ViewCompat.setOnApplyWindowInsetsListener(viewBinding.container) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            startActivity(
                Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:$packageName")
                }
            )
        }

        btnHome.setOnClickListener {
            if (navigationStack.isEmpty()) return@setOnClickListener // Desactiva botón Home en pantalla inicial
            navigationStack.clear()
            titleStack.clear()
            descriptionStack.clear()
            showElementos(rootElementos)
        }

        btnBack.setOnClickListener {
            if (navigationStack.isEmpty()) return@setOnClickListener // Desactiva botón Atrás en pantalla inicial
            onBackPressed()
        }

        btnConfig.setOnClickListener {
            showPasswordDialog()
        }

        val cachedUri = pref.getString(PREF_KEY_JSON_URI, null)
        if (cachedUri != null) {
            try {
                val uri = Uri.parse(cachedUri)
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                loadJsonFromUri(uri)
                return
            } catch (e: Exception) {
                pref.edit().remove(PREF_KEY_JSON_URI).apply()
                Toast.makeText(this, "Error leyendo el JSON guardado", Toast.LENGTH_SHORT).show()
            }
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_JSON_FILE && resultCode == RESULT_OK) {
            val uriString = data?.getStringExtra("jsonUri") ?: return
            val uri = Uri.parse(uriString)
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            try {
                loadJsonFromUri(uri)
                pref.edit().putString(PREF_KEY_JSON_URI, uriString).apply()
            } catch (e: Exception) {
                Toast.makeText(this, "Error leyendo JSON seleccionado", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        }
    }

    private fun loadJsonFromUri(uri: Uri) {
        contentResolver.openInputStream(uri)?.use { input ->
            BufferedReader(InputStreamReader(input)).use { reader ->
                val sb = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) sb.append(line)
                val listType = object : TypeToken<List<Elemento>>() {}.type
                val root : List<Elemento> = Gson().fromJson(sb.toString(), listType)
                rootElementos = root
                toolbar.visibility = View.VISIBLE
                showElementos(rootElementos)

            }
        } ?: throw Exception("No se pudo abrir flujo para $uri")
    }

    private fun showElementos(elementos: List<Elemento>) {

        adapter = ElementoAdapter(elementos, object : ElementoAdapter.OnElementoClickListener {
            override fun onElementoClick(elemento: Elemento) {
                if (elemento.type == "curso") {
                    navigationStack.push(adapter.elementos)
                    titleStack.push(elemento.nombre ?: "Curso sin nombre")
                    descriptionStack.push(elemento.descripcion ?: "")
                    showElementos(elemento.subelementos!!)
                } else if (elemento.type == "documento") openPdf(elemento.descripcion ?: return)
            }
        })
        if (elementos.isEmpty()) {
            sinElementos.visibility = View.VISIBLE
        }else {
            sinElementos.visibility = View.GONE
        }
        tituloToolbar.text = if (titleStack.isEmpty()) "Inicio" else titleStack.peek()
        descripcionToolbar.text = if (descriptionStack.isEmpty()) "Selecciona un curso para obtener las listas de admisiones" else descriptionStack.peek()
        recyclerView.adapter = adapter
    }

    private fun openPdf(nombreArchivo: String) {
        val pdfFile = File(Environment.getExternalStorageDirectory(), "ArchivosPdf/$nombreArchivo")
        if (!pdfFile.exists()) {
            Toast.makeText(this, "Archivo no encontrado: $nombreArchivo", Toast.LENGTH_SHORT).show()
            return
        }
        startActivity(
            Intent(this, PdfViewerActivity::class.java).apply {
                putExtra("fileName", pdfFile.nameWithoutExtension)
                putExtra("filePath", pdfFile.absolutePath)
            }
        )
    }

    override fun onBackPressed() {
        if (navigationStack.isNotEmpty()) {
            titleStack.pop()
            descriptionStack.pop()
            showElementos(navigationStack.pop())
        } else {
            // Evita acción por defecto si estamos en el inicio
            // super.onBackPressed() se omite aquí
        }
    }



private fun showPasswordDialog() {
    val editText = EditText(this)
    editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD

    AlertDialog.Builder(this)
        .setTitle("Introduce la contraseña")
        .setView(editText)
        .setPositiveButton("Aceptar") { dialog, _ ->
            val password = editText.text.toString()
            if (password == secretPassoword) {
                // Contraseña correcta: abrir ConfigActivity
                val intent = Intent(this, ConfigActivity::class.java)
                startActivityForResult(intent, PICK_JSON_FILE)
            } else {
                Toast.makeText(this, "Contraseña incorrecta", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }
        .setNegativeButton("Cancelar") { dialog, _ ->
            dialog.dismiss()
        }
        .show()
}

}
